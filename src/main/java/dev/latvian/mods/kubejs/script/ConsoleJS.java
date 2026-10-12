package dev.latvian.mods.kubejs.script;

import com.google.gson.JsonElement;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.ws.WSHandler;
import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.JavaWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.StringUtilsWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.TextIcons;
import dev.latvian.mods.kubejs.util.JsonIO;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.LogType;
import dev.latvian.mods.kubejs.util.MutedError;
import dev.latvian.mods.kubejs.util.NBTUtils;
import dev.latvian.mods.kubejs.util.StackTraceCollector;
import dev.latvian.mods.kubejs.util.TimeJS;
import dev.latvian.mods.kubejs.util.WrappedJS;
import dev.latvian.mods.kubejs.web.JsonContent;
import dev.latvian.mods.kubejs.web.KJSHTTPRequest;
import dev.latvian.mods.kubejs.web.KJSWSSession;
import dev.latvian.mods.kubejs.web.local.KubeJSWeb;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.EcmaError;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.WrappedException;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// Logger for a given [ScriptType], exposed to scripts as the global `console`.
///
/// By default, one instance of this class exists for each [ScriptType] that survives across
/// reloads and whose log file is cleared by [#resetFile()] on reload, though custom console
/// instances may additionally be created by users (these also save to the script type's `.log``)
///
/// Messages are written to:
///
///   - The SLF4J [Logger] (i.e. the game log / stdout)
///   - The per-type log file at `logs/kubejs/<type>.log`, asynchronously via the type's background executor
///   - Any connected web-socket clients via [KubeJSWeb]
///
/// @see JavaWrapper#createConsole(KubeJSContext, String)
@NullUnmarked
public class ConsoleJS {
	public static ConsoleJS STARTUP;
	public static ConsoleJS SERVER;
	public static ConsoleJS CLIENT;

	public static ConsoleJS getCurrent(@Nullable Context cx) {
		if (cx instanceof KubeJSContext kcx) {
			return kcx.getConsole();
		}

		return STARTUP;
	}

	private static final Pattern GARBAGE_PATTERN = Pattern.compile("(?:TRANSFORMER|LAYER PLUGIN|MC-BOOTSTRAP)/\\w+@[^/]+/");

	@HideFromJS
	public static final Function<String, String> ERROR_REDUCE = s -> {
		if (s.startsWith("java.util.concurrent.ForkJoin") || s.startsWith("jdk.internal.")) {
			return "";
		}

		return GARBAGE_PATTERN.matcher(s).replaceAll("").replace("dev.latvian.mods.", "…");
	};

	public static Pattern methodPattern(Class<?> c, String method) {
		return Pattern.compile(c.getName().replace(".", "\\.") + "\\." + method);
	}

	public final ScriptType scriptType;
	public final transient Collection<ConsoleLine> errors;
	public final transient Collection<ConsoleLine> warnings;
	public final transient Logger logger;
	private final Path logFile;
	/// @see #isCapturingErrors()
	private boolean shouldCaptureErrors;
	private String group;
	private boolean muted;
	private boolean debugEnabled;
	private boolean writeToFile;
	private final List<String> writeQueue;
	private final Calendar calendar;
	public WeakReference<ContextFactory> contextFactory;
	public WSHandler<KJSHTTPRequest, KJSWSSession> wsBroadcaster;

	public ConsoleJS(ScriptType m, Logger log) {
		this.scriptType = m;
		this.errors = new ConcurrentLinkedDeque<>();
		this.warnings = new ConcurrentLinkedDeque<>();
		this.logger = log;
		this.logFile = m.getLogFile();
		this.shouldCaptureErrors = false; // this can be overridden by DevProperties
		this.group = "";
		this.muted = false;
		this.debugEnabled = false;
		this.writeToFile = true;
		this.writeQueue = new LinkedList<>();
		this.calendar = Calendar.getInstance();
	}

	public Logger getLogger() {
		return logger;
	}

	protected boolean shouldPrint() {
		return !muted;
	}

	public void setMuted(boolean m) {
		muted = m;
	}

	public boolean getMuted() {
		return muted;
	}

	public void setDebugEnabled(boolean m) {
		debugEnabled = m;
	}

	public boolean getDebugEnabled() {
		return debugEnabled;
	}

	public synchronized void setWriteToFile(boolean m) {
		writeToFile = m;
	}

	public synchronized boolean getWriteToFile() {
		return writeToFile;
	}

	public synchronized void startCapturingErrors() {
		setCapturingErrors(true);
	}

	public synchronized void stopCapturingErrors() {
		setCapturingErrors(false);
	}

	private synchronized void setCapturingErrors(boolean enabled) {
		var wasCapturing = shouldCaptureErrors;
		shouldCaptureErrors = enabled;

		if (!FMLLoader.getCurrent().isProduction() && enabled != wasCapturing) {
			if (enabled) {
				logger.info("Capturing errors for {} scripts enabled", scriptType.name);
			} else {
				logger.info("Capturing errors for {} scripts disabled", scriptType.name);
			}
		}
	}

	public synchronized void resetFile() {
		errors.clear();
		warnings.clear();
		KubeJSWeb.broadcastEvent(wsBroadcaster, "clear", "", null);
		scriptType.executor.execute(() -> {
			try {
				Files.write(logFile, List.of());
			} catch (Exception ex) {
				logger.error("Failed to clear the log file: {}", String.valueOf(ex));
			}
		});
	}

	private Object unwrapForPrinting(Object object) {
		return switch (object) {
			case CharSequence cs -> cs.toString();
			case Identifier id -> id.toString();
			case Tag tag -> JsonIO.toObject(NBTUtils.toJson(tag));
			case JsonElement json -> JsonIO.toObject(json);
			case Iterable<?> itr -> ListJS.of(itr);
			case Map<?, ?> map -> map;
			default -> {
				if (object.getClass().isArray()) {
					yield ListJS.ofArray(object);
				} else {
					yield object;
				}
			}
		};
	}

	private ConsoleLine line(LogType type, SourceLine sourceLine, Object object, @Nullable Throwable error) {
		Object o = unwrapForPrinting(object);

		if (o instanceof Component c) {
			o = c.getString();
		}

		var timestamp = System.currentTimeMillis();
		var lineStr = (o == null
			|| o instanceof Boolean
			|| o instanceof Character
			|| o instanceof Number
			|| o instanceof CharSequence
			|| o instanceof WrappedJS
		) ? String.valueOf(o) : (o + " [" + o.getClass().getName() + "]");
		var line = new ConsoleLine(this, timestamp, lineStr);
		line.type = type;
		line.group = group;

		line.withSourceLine(sourceLine);

		if (error instanceof KubeRuntimeException ex) {
			ex.apply(line);
		}

		if (error instanceof RhinoException ex) {
			if (ex.lineSource() != null) {
				line.withSourceLine(ex.lineSource(), ex.lineNumber());
			}

			if (isCapturingErrors()) {
				for (var el : ex.getScriptStack()) {
					if (el.fileName != null && el.lineNumber >= 0) {
						line.withSourceLine(el.fileName, el.lineNumber);
					}
				}
			}
		} else if (error != null && isCapturingErrors()) {
			for (var el : error.getStackTrace()) {
				if (el.getFileName() != null && el.getLineNumber() >= 0 && el.getClassName().startsWith("dev.latvian.mods.kubejs.")) {
					line.withSourceLine(el.getFileName(), el.getLineNumber());
				}
			}
		}

		if (line.message != null && line.message.endsWith(")")) {
			int lpi = line.message.lastIndexOf('(');
			if (lpi > 0) {
				var pe = line.message.substring(lpi + 1, line.message.length() - 1);
				int ci = pe.lastIndexOf('#');

				if (ci > 0) {
					try {
						line.withSourceLine(pe.substring(0, ci), Integer.parseInt(pe.substring(ci + 1)));
						line.message = line.message.substring(0, lpi).trim();
					} catch (Exception ignored) {
					}
				}
			}
		}

		if (line.sourceLines.isEmpty()) {
			var factory = contextFactory == null ? null : contextFactory.get();

			if (factory != null) {
				line.withSourceLine(SourceLine.of(factory.enter()));
			}
		}

		return line;
	}

	private ConsoleLine log(LogType type, SourceLine sourceLine, @Nullable Throwable error, Object message) {
		if (shouldPrint()) {
			var line = line(type, sourceLine, message, error);
			type.callback.accept(logger, line.getText());

			if (isCapturingErrors()) {
				if (type == LogType.ERROR) {
					errors.add(line);
				} else if (type == LogType.WARN) {
					warnings.add(line);
				}
			}

			if (writeToFile) {
				writeToFile(type, line.timestamp, line.getText());
				KubeJSWeb.broadcastEvent(wsBroadcaster, type.id, "", line);
			}

			return line;
		}

		return null;
	}

	public synchronized void writeToFile(LogType type, String line) {
		writeToFile(type, System.currentTimeMillis(), line);
	}

	public synchronized void writeToFile(LogType type, long timestamp, String line) {
		if (!writeToFile || PlatformWrapper.isGeneratingData()) {
			return;
		}

		calendar.setTimeInMillis(timestamp);
		var sb = new StringBuilder();

		sb.append('[');
		TimeJS.appendTimestamp(sb, calendar);
		sb.append("] [");
		sb.append(type);
		sb.append("] ");

		if (type == LogType.ERROR) {
			sb.append("! ");
		}

		sb.append(line);
		writeQueue.add(sb.toString());
	}

	public synchronized void flush(boolean sync) {
		if (writeQueue.isEmpty()) {
			return;
		}

		var lines = Arrays.asList(writeQueue.toArray(StringUtilsWrapper.EMPTY_STRING_ARRAY));
		writeQueue.clear();

		if (sync) {
			try {
				Files.write(logFile, lines, StandardOpenOption.APPEND);
			} catch (Exception ex) {
				logger.error("Failed to write to the log file: {}", String.valueOf(ex));
			}
		} else {
			scriptType.executor.execute(() -> {
				try {
					Files.write(logFile, lines, StandardOpenOption.APPEND);
				} catch (Exception ex) {
					logger.error("Failed to write to the log file: {}", String.valueOf(ex));
				}
			});
		}
	}

	public void log(Object... message) {
		for (var s : message) {
			info(s);
		}
	}

	public void log(Context cx, Object... message) {
		for (var s : message) {
			info(cx, s);
		}
	}

	public ConsoleLine info(Object message) {
		return log(LogType.INFO, SourceLine.UNKNOWN, null, message);
	}

	public ConsoleLine info(Context cx, Object message) {
		return log(LogType.INFO, SourceLine.of(cx), null, message);
	}

	public ConsoleLine infof(String message, Object... args) {
		return info(message.formatted(args));
	}

	public ConsoleLine warn(Object message) {
		return log(LogType.WARN, SourceLine.UNKNOWN, null, message);
	}

	public ConsoleLine warn(Context cx, Object message) {
		return log(LogType.WARN, SourceLine.of(cx), null, message);
	}

	public ConsoleLine warn(String message, Throwable error, @Nullable Pattern exitPattern) {
		return warn(message, SourceLine.UNKNOWN, error, exitPattern);
	}

	public ConsoleLine warn(String message, SourceLine sourceLine, Throwable error, @Nullable Pattern exitPattern) {
		if (shouldPrint()) {
			var l = log(LogType.WARN, sourceLine, error, messageForPrint(message, error));
			handleError(l, error, exitPattern, !isCapturingErrors());
			return l;
		}

		return null;
	}

	private static String messageForPrint(String message, @Nullable Throwable error) {
		if (message.isEmpty()) {
			return Objects.requireNonNull(error, "Both message and error are empty!").toString();
		} else {
			return error == null ? message : (message + ": " + error);
		}
	}

	public ConsoleLine warn(String message, Throwable error) {
		return warn(message, error, null);
	}

	public ConsoleLine warnf(String message, Object... args) {
		return warn(message.formatted(args));
	}

	public ConsoleLine error(Object message) {
		return log(LogType.ERROR, SourceLine.UNKNOWN, null, message);
	}

	public ConsoleLine error(Context cx, Object message) {
		return log(LogType.ERROR, SourceLine.of(cx), null, message);
	}

	public ConsoleLine error(String message, Throwable error, @Nullable Pattern exitPattern) {
		return error(message, SourceLine.UNKNOWN, error, exitPattern);
	}

	public ConsoleLine error(String message, SourceLine sourceLine, Throwable error, @Nullable Pattern exitPattern) {
		if (shouldPrint()) {
			var l = log(LogType.ERROR, sourceLine, error, messageForPrint(message, error));
			handleError(l, error, exitPattern, true);
			return l;
		}

		return null;
	}

	public ConsoleLine error(String message, Throwable throwable) {
		return error(message, throwable, null);
	}

	public ConsoleLine errorf(String message, Object... args) {
		return error(message.formatted(args));
	}

	public boolean shouldPrintDebug() {
		return debugEnabled && shouldPrint();
	}

	public ConsoleLine debug(Object message) {
		if (shouldPrintDebug()) {
			return log(LogType.DEBUG, SourceLine.UNKNOWN, null, message);
		}

		return null;
	}

	public ConsoleLine debug(Context cx, Object message) {
		if (shouldPrintDebug()) {
			return log(LogType.DEBUG, SourceLine.of(cx), null, message);
		}

		return null;
	}

	public ConsoleLine debugf(String message, Object... args) {
		if (shouldPrintDebug()) {
			return debug(message.formatted(args));
		}

		return null;
	}

	public void group() {
		group += "  ";
	}

	public void groupEnd() {
		if (group.length() >= 2) {
			group = group.substring(0, group.length() - 2);
		}
	}

	public void trace() {
		var elements = Thread.currentThread().getStackTrace();
		info("=== Stack Trace ===");

		for (var element : elements) {
			info("=\t" + element);
		}
	}

	public int getScriptLine() {
		var factory = contextFactory == null ? null : contextFactory.get();

		if (factory != null) {
			var linep = new int[]{0};
			Context.getSourcePositionFromStack(factory.enter(), linep);
			return linep[0];
		}

		return 0;
	}

	public void printClass(String className, boolean tree) {
		try {
			var c = Class.forName(className);
			var sc = c.getSuperclass();

			info("=== " + c.getName() + " ===");
			info("= Parent class =");
			info("> " + (sc == null ? "-" : sc.getName()));

			var vars = new HashMap<String, VarFunc>();
			var funcs = new HashMap<String, VarFunc>();

			for (var field : c.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.PUBLIC) == 0 || (field.getModifiers() & Modifier.TRANSIENT) == 0) {
					continue;
				}

				var f = new VarFunc(field.getName(), field.getType());
				f.flags |= 1;

				if ((field.getModifiers() & Modifier.FINAL) == 0) {
					f.flags |= 2;
				}

				vars.put(f.name, f);
			}

			for (var method : c.getDeclaredMethods()) {
				if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
					continue;
				}

				var f = new VarFunc(method.getName(), method.getReturnType());

				for (var i = 0; i < method.getParameterCount(); i++) {
					f.params.add(method.getParameters()[i].getType());
				}

				if (f.name.length() >= 4 && f.name.startsWith("get") && Character.isUpperCase(f.name.charAt(3)) && f.params.isEmpty()) {
					var n = Character.toLowerCase(f.name.charAt(3)) + f.name.substring(4);
					var f0 = vars.get(n);

					if (f0 == null) {
						vars.put(n, new VarFunc(n, f.type));
						continue;
					} else if (f0.type.equals(f.type)) {
						f0.flags |= 1;
						continue;
					}
				}

				funcs.put(f.name, f);
			}

			info("= Variables and Functions =");

			if (vars.isEmpty() && funcs.isEmpty()) {
				info("-");
			} else {
				vars.values().stream().sorted().forEach(f -> info("> " + ((f.flags & 2) == 0 ? "val" : "var") + " " + f.name + ": " + getSimpleName(f.type)));
				funcs.values().stream().sorted().forEach(f -> info("> function " + f.name + "(" + f.params.stream().map(this::getSimpleName).collect(Collectors.joining(", ")) + "): " + getSimpleName(f.type)));
			}

			if (tree && sc != null) {
				info("");
				printClass(sc.getName(), true);
			}
		} catch (Throwable ex) {
			error("= Error loading class '" + className + "' =", ex);
		}
	}

	public void printClass(String className) {
		printClass(className, false);
	}

	private String getSimpleName(Class<?> c) {
		if (c.isPrimitive()) {
			return c.getName();
		}

		var s = c.getName();
		var i = s.lastIndexOf('.');
		s = s.substring(i + 1);
		i = s.lastIndexOf('$');
		s = s.substring(i + 1);
		return s;
	}

	public void printObject(@Nullable Object o, boolean tree) {
		if (o == null) {
			info("=== null ===");
		} else {
			info("=== " + o.getClass().getName() + " ===");
			info("= toString() =");
			info("> " + o);
			info("= hashCode() =");
			info("> " + Integer.toHexString(o.hashCode()));
			info("");
			printClass(o.getClass().getName(), tree);
		}
	}

	public void printObject(@Nullable Object o) {
		printObject(o, false);
	}

	public void handleError(ConsoleLine line, @Nullable Throwable error, @Nullable Pattern exitPattern, boolean print) {
		while (error instanceof WrappedException ex) {
			error = ex.getWrappedException();
		}

		if (error == null || error instanceof EcmaError) {
			return;
		}

		line.stackTrace = new ArrayList<>();
		error.printStackTrace(new StackTraceCollector(line.stackTrace, exitPattern, ERROR_REDUCE));

		if (print && !(error instanceof MutedError err && err.isMuted())) {
			for (var s : line.stackTrace) {
				line.type.callback.accept(logger, s);

				if (writeToFile) {
					writeToFile(line.type, line.timestamp, s);
				}
			}
		}
	}

	public Component errorsComponent(String command) {
		return Component.empty()
			.append(TextIcons.error())
			.append(Component.literal(" KubeJS errors found [" + errors.size() + "]!").kjs$red())
			.kjs$clickRunCommand(command)
			.kjs$hover(Component.literal("Click to show more info"));
	}

	private boolean isCapturingErrors() {
		return shouldCaptureErrors || DevProperties.get().alwaysCaptureErrors;
	}

	private static final class VarFunc implements Comparable<VarFunc> {
		public final String name;
		public final Class<?> type;
		public final ArrayList<Class<?>> params;
		public int flags;

		public VarFunc(String n, Class<?> t) {
			name = n;
			type = t;
			flags = 0;
			params = new ArrayList<>();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			var varFunc = (VarFunc) o;
			return Objects.equals(name, varFunc.name) && Objects.equals(type, varFunc.type) && Objects.equals(flags, varFunc.flags) && Objects.equals(params, varFunc.params);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, type, flags, params);
		}

		@Override
		public int compareTo(VarFunc o) {
			return name.compareToIgnoreCase(o.name);
		}
	}

	public HTTPResponse getErrorsResponse(KJSHTTPRequest ctx) {
		return HTTPResponse.ok().content(JsonContent.array(json -> {
			for (var error : errors) {
				json.add(error.toJson());
			}
		}));
	}

	public HTTPResponse getWarningsResponse(KJSHTTPRequest ctx) {
		return HTTPResponse.ok().content(JsonContent.array(json -> {
			for (var error : warnings) {
				json.add(error.toJson());
			}
		}));
	}
}
