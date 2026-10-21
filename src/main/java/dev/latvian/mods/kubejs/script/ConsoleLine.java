package dev.latvian.mods.kubejs.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.codec.KubeJSCodecs;
import dev.latvian.mods.kubejs.util.LogType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ConsoleLine implements Supplier<JsonElement> {
	public static final ConsoleLine[] EMPTY_ARRAY = new ConsoleLine[0];

	public static final StreamCodec<FriendlyByteBuf, ConsoleLine> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public ConsoleLine decode(FriendlyByteBuf buf) {
			var console = ScriptType.VALUES[buf.readByte()].console;
			var timestamp = buf.readVarLong();
			var message = buf.readUtf();
			var line = new ConsoleLine(console, timestamp, message);
			line.type = LogType.VALUES[buf.readByte()];
			line.group = "";
			line.sourceLines = KubeJSCodecs.readList(buf, SourceLine::read);
			line.stackTrace = KubeJSCodecs.readList(buf, FriendlyByteBuf::readUtf);
			return line;
		}

		@Override
		public void encode(FriendlyByteBuf buf, ConsoleLine line) {
			buf.writeByte(line.console.scriptType.ordinal());
			buf.writeVarLong(line.timestamp);
			buf.writeUtf(line.message);
			buf.writeByte(line.type.ordinal());
			KubeJSCodecs.writeCollection(buf, line.sourceLines, SourceLine::write);
			KubeJSCodecs.writeCollection(buf, line.stackTrace, FriendlyByteBuf::writeUtf);
		}
	};

	public final ConsoleJS console;
	public final long timestamp;
	public String message;
	public LogType type = LogType.INFO;
	public String group = "";
	public Collection<SourceLine> sourceLines = Set.of();
	public @Nullable Path externalFile = null;
	public List<String> stackTrace = List.of();
	private @Nullable String cachedText;
	private @Nullable JsonObject customData;

	public ConsoleLine(ConsoleJS console, long timestamp, String message) {
		this.console = console;
		this.timestamp = timestamp;
		this.message = message;
	}

	public String getText() {
		if (cachedText == null) {
			var builder = new StringBuilder();

			if (!sourceLines.isEmpty()) {
				for (var line : sourceLines) {
					if (!line.isUnknown()) {
						builder.append(line.source()).append('#').append(line.line()).append(':').append(' ');
						break;
					}
				}
			}

			if (!group.isEmpty()) {
				builder.append(group);
			}

			builder.append(message);
			cachedText = builder.toString();
		}

		return cachedText;
	}

	public ConsoleLine withSourceLine(String source, int line) {
		if (source == null) {
			source = "";
		}

		if (!source.isEmpty() && source.startsWith(console.scriptType.nameStrip)) {
			source = source.substring(console.scriptType.nameStrip.length());
		}

		if (line < 0) {
			line = 0;
		}

		return source.isEmpty() && line == 0 ? this : withSourceLine(SourceLine.of(source, line));
	}

	public ConsoleLine withSourceLine(SourceLine sourceLine) {
		if (sourceLine.isUnknown()) {
			return this;
		}

		if (sourceLines.isEmpty()) {
			sourceLines = Set.of(sourceLine);
			return this;
		} else if (sourceLines.size() == 1) {
			var line0 = sourceLines.iterator().next();
			sourceLines = new LinkedHashSet<>();
			sourceLines.add(line0);
		}

		sourceLines.add(sourceLine);
		return this;
	}

	public ConsoleLine withExternalFile(Path path) {
		externalFile = path;
		sourceLines = Set.of(SourceLine.of(path.getFileName().toString(), 0));
		return this;
	}

	@Override
	public String toString() {
		return getText();
	}

	public ConsoleLine customData(String key, JsonElement data, boolean override) {
		if (customData == null) {
			customData = new JsonObject();
		}

		if (override || !customData.has(key)) {
			customData.add(key, data);
		}

		return this;
	}

	public JsonObject toJson() {
		var json = new JsonObject();
		json.addProperty("type", type.id);
		json.addProperty("message", getText());
		json.addProperty("timestamp", timestamp);

		if (customData != null) {
			json.add("custom_data", customData);
		}

		var ssls = new JsonArray();
		var asls = new JsonArray();
		json.add("script_source_lines", ssls);
		json.add("all_source_lines", asls);

		for (var l : sourceLines) {
			var sourceLine = new JsonObject();
			sourceLine.addProperty("source", l.source());
			sourceLine.addProperty("line", l.line());
			asls.add(sourceLine);

			if (l.source().endsWith(".js")) {
				ssls.add(sourceLine);
			}
		}

		var st = new JsonArray();
		json.add("stack_trace", st);

		for (var s : stackTrace) {
			st.add(s);
		}

		return json;
	}

	@Override
	public JsonElement get() {
		return toJson();
	}
}
