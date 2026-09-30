package dev.latvian.mods.kubejs.client;

import dev.latvian.mods.betteradvancedtooltips.BATIcons;
import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.TextIcons;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ConsoleLine;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.LogType;
import dev.latvian.mods.kubejs.util.PlatformUtil;
import dev.latvian.mods.kubejs.util.TimeJS;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@SuppressWarnings("NotNullFieldNotInitialized") // lateinit fields
public class KubeJSErrorScreen extends Screen {
	public final @Nullable Screen lastScreen;
	public final ScriptType scriptType;
	public final @Nullable Path logFile;
	public final List<ConsoleLine> errors;
	public final List<ConsoleLine> warnings;
	public final boolean canClose;
	public List<ConsoleLine> viewing;
	private ErrorList list;
	private List<FormattedCharSequence> tooltip;

	public KubeJSErrorScreen(@Nullable Screen lastScreen, ScriptType scriptType, @Nullable Path logFile, List<ConsoleLine> errors, List<ConsoleLine> warnings, boolean canClose) {
		super(Component.empty());
		this.lastScreen = lastScreen;
		this.scriptType = scriptType;
		this.logFile = logFile;
		this.errors = errors;
		this.warnings = warnings;
		this.canClose = canClose;

		this.viewing = errors.isEmpty() && !warnings.isEmpty() ? warnings : errors;
	}

	public KubeJSErrorScreen(@Nullable Screen lastScreen, ConsoleJS console, boolean canClose) {
		this(lastScreen, console.scriptType, console.scriptType.getLogFile(), new ArrayList<>(console.errors), new ArrayList<>(console.warnings), canClose);
	}

	public void setTooltip(List<FormattedCharSequence> tooltip) {
		this.tooltip = tooltip;
	}

	@Override
	public Component getNarrationMessage() {
		return Component.literal("There were KubeJS " + scriptType.name + " errors!");
	}

	@Override
	protected void init() {
		super.init();
		this.list = new ErrorList(this, this.minecraft, this.width, this.height, 32, this.height - 32, viewing);
		this.addWidget(list);

		int i = this.height - 26;

		Button openLog;

		if (CommonProperties.get().startupErrorReportUrl.isBlank()) {
			openLog = this.addRenderableWidget(Button.builder(Component.literal("Open Log File"), this::openLog).bounds(this.width / 2 - 155, i, 150, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal(canClose ? "Close" : "Quit"), this::quit).bounds(this.width / 2 - 155 + 160, i, 150, 20).build());
		} else {
			openLog = this.addRenderableWidget(Button.builder(Component.literal("Open Log File"), this::openLog).bounds(this.width / 4 - 55, i, 100, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("Report"), this::report).bounds(this.width / 2 - 50, i, 100, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal(canClose ? "Close" : "Quit"), this::quit).bounds(this.width * 3 / 4 - 45, i, 100, 20).build());
		}

		openLog.active = logFile != null;

		var viewOther = this.addRenderableWidget(Button.builder(Component.literal((viewing == errors) ? ("View Warnings [" + warnings.size() + "]") : ("View Errors [" + errors.size() + "]")), this::viewOther).bounds(this.width - 107, 7, 100, 20).build());

		if (errors.isEmpty() || warnings.isEmpty()) {
			viewOther.active = false;
		}
	}

	private void quit(Button button) {
		if (canClose) {
			onClose();
		} else {
			minecraft.stop();
		}
	}

	private void report(Button button) {
		try {
			PlatformUtil.openUri(URI.create(CommonProperties.get().startupErrorReportUrl));
		} catch (Exception ignored) {
		}
	}

	private void openLog(Button button) {
		if (logFile != null) {
			try {
				PlatformUtil.openFile(logFile.toAbsolutePath().toFile());
			} catch (Exception ignored) {
			}
		}
	}

	private void viewOther(Button button) {
		viewing = viewing == errors ? warnings : errors;
		repositionElements();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
		tooltip = null;

		super.extractRenderState(graphics, mx, my, delta);
		graphics.centeredText(font, "KubeJS " + scriptType.name + " script " + (viewing == errors ? "errors" : "warnings"), width / 2, 12, 0xFFFFFFFF);
		list.extractRenderState(graphics, mx, my, delta);


		if (errors.isEmpty() && warnings.isEmpty()) {
			graphics.centeredText(font, "No errors or warnings found!", width / 2, height / 2 - 6, 0xFF66FF66);
		}

		if (tooltip != null && !tooltip.isEmpty()) {
			var comps = tooltip.stream()
				.map(ClientTooltipComponent::create)
				.toList();

			graphics.tooltip(font, comps, mx, my, (guiW, guiH, x, y, tipW, tipH) -> {
				int px = x + 12;
				int py = y - 12;

				if (px + tipW > guiW) {
					px = x - 12 - tipW;
				}
				if (py + tipH + 6 > guiH) {
					py = guiH - tipH - 6;
				}
				if (py < 4) {
					py = 4;
				}
				if (px < 4) {
					px = 4;
				}
				if (px + tipW > guiW - 4) {
					px = guiW - tipW - 4;
				}

				return new Vector2i(px, py);
			}, null, false);

		}

	}


	@Override
	public boolean shouldCloseOnEsc() {
		return canClose;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(lastScreen);
	}

	public static class ErrorList extends ObjectSelectionList<Entry> {
		public final KubeJSErrorScreen screen;
		public final List<ConsoleLine> lines;

		public ErrorList(KubeJSErrorScreen screen, Minecraft minecraft, int width, int height, int top, int bottom, List<ConsoleLine> lines) {
			super(minecraft, width, bottom - top, top, 48);
			this.screen = screen;
			this.lines = lines;

			var calendar = Calendar.getInstance();

			for (int i = 0; i < lines.size(); i++) {
				addEntry(new KubeJSErrorScreen.Entry(this, minecraft, i, lines.get(i), calendar));
			}
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			int key = event.key();
			if (key == 257 || key == 32 || key == 335) {
				var sel = getSelected();
				if (sel != null) {
					sel.open();
					return true;
				}
			}

			return super.keyPressed(event);
		}

		@Override
		public int getRowWidth() {
			return (int) (this.width * 0.93D);
		}
	}

	public static class Entry extends ObjectSelectionList.Entry<Entry> {
		private final ErrorList errorList;
		private final Minecraft minecraft;
		private final ConsoleLine line;
		private long lastClickTime;
		private final FormattedCharSequence indexText;
		private final FormattedCharSequence scriptLineText;
		private final FormattedCharSequence timestampText;
		private final List<FormattedCharSequence> errorText;
		private final List<FormattedCharSequence> firstStackTraceLine;
		private final List<FormattedCharSequence> stackTraceText;
		private final List<FormattedCharSequence> fullStackTraceText;
		private final int totalStackTraceSize;

		public Entry(ErrorList errorList, Minecraft minecraft, int index, ConsoleLine line, Calendar calendar) {
			this.errorList = errorList;
			this.minecraft = minecraft;
			this.line = line;

			this.indexText = Component.literal("#" + (index + 1)).getVisualOrderText();

			var scriptLineTextList = getScriptLineTextList(line);

			this.scriptLineText = Component.literal(String.join(" < ", scriptLineTextList)).getVisualOrderText();

			var sb = new StringBuilder();
			calendar.setTimeInMillis(line.timestamp);
			TimeJS.appendTimestamp(sb, calendar);
			this.timestampText = Component.literal(sb.toString()).getVisualOrderText();

			int maxWidth = minecraft.getWindow().getGuiScaledWidth() - 24;

			this.errorText = new ArrayList<>(minecraft.font.split(Component.literal(line.message), errorList.getRowWidth()).stream().limit(3L).toList());

			if (line.stackTrace.isEmpty()) {
				this.firstStackTraceLine = List.of();
				this.stackTraceText = List.of();
				this.fullStackTraceText = List.of();
				this.totalStackTraceSize = 0;
			} else {
				this.firstStackTraceLine = new ArrayList<>();
				this.stackTraceText = new ArrayList<>();
				this.fullStackTraceText = new ArrayList<>();

				for (var l1 : line.stackTrace.getFirst().split("\n")) {
					firstStackTraceLine.addAll(minecraft.font.split(Component.literal(l1).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)), maxWidth));
				}

				start:
				for (int i = 1; i < line.stackTrace.size(); i++) {
					for (var l1 : line.stackTrace.get(i).split("\n")) {

						for (var l2 : minecraft.font.split(Component.literal(l1).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)), Integer.MAX_VALUE)) {
							stackTraceText.add(l2);

							if (stackTraceText.size() >= 4) {
								break start;
							}
						}
					}
				}

				for (int i = 1; i < line.stackTrace.size(); i++) {
					for (var l1 : line.stackTrace.get(i).split("\n")) {
						fullStackTraceText.addAll(minecraft.font.split(Component.literal(l1).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)), maxWidth));
					}
				}

				this.totalStackTraceSize = firstStackTraceLine.size() + fullStackTraceText.size();
			}
		}

		private ArrayList<String> getScriptLineTextList(ConsoleLine line) {
			var sourceLines = new ArrayList<>(line.sourceLines);
			var scriptLineTextList = new ArrayList<String>();

			for (int i = 0; i < sourceLines.size(); i++) {
				if (sourceLines.get(i).source().endsWith(".java")) {
					continue;
				}

				if (i >= 3) {
					scriptLineTextList.add("...");
					break;
				} else {
					scriptLineTextList.add(sourceLines.get(i).toString());
				}
			}

			if (scriptLineTextList.isEmpty()) {
				scriptLineTextList.add(this.line.type == LogType.WARN ? "Internal Warning" : "Internal Error");
			}
			return scriptLineTextList;
		}

		@Override
		public Component getNarration() {
			return Component.empty();
		}


		@Override
		public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float delta) {
			int x = getX();
			int y = getY();
			int w = getWidth();
			int h = getHeight();

			int col = line.type == LogType.ERROR ? 0xFFFF5B63 : 0xFFFFBB5B;
			g.text(minecraft.font, indexText, x + 1, y + 1, col);
			g.centeredText(minecraft.font, scriptLineText, x + w / 2, y + 1, 0xFFFFFFFF);
			g.text(minecraft.font, timestampText, x + w - minecraft.font.width(timestampText) - 4, y + 1, 0xFF666666);

			for (int i = 0; i < errorText.size(); i++) {
				g.text(minecraft.font, errorText.get(i), x + 1, y + 13 + i * 10, col);
			}

			if (hovered && totalStackTraceSize > 0) {
				if (mouseY < y + 10 && line.sourceLines.size() >= 3) {
					var lines = new ArrayList<FormattedCharSequence>();

					int ln = 0;

					for (var sl : line.sourceLines) {
						if (sl.line() > 0 && sl.source().endsWith(".js")) {
							ln = sl.line();
							break;
						}
					}

					if (ln > 0) {
						var comp = Component.empty();
						comp.append("Double-click to open file");

						if (EditorExt.isKnownVSCode()) {
							comp.append(" in ");
							comp.append(TextIcons.VSCODE);
							comp.append(BATIcons.SMALL_SPACE);
							comp.append(Component.literal("VSCode").withColor(0x22A7F2));
						}

						lines.addAll(minecraft.font.split(comp, 1000));
					}

					for (var sl : line.sourceLines) {
						lines.add(Component.empty().append(Component.literal(sl.source()).kjs$gray()).append("#" + sl.line()).getVisualOrderText());
					}

					errorList.screen.setTooltip(lines);
				} else {
					var list = new ArrayList<>(firstStackTraceLine);

					if (Minecraft.getInstance().hasShiftDown()) {
						list.addAll(fullStackTraceText);
					} else {
						list.addAll(stackTraceText);
					}

					errorList.screen.setTooltip(list);

				}
			}
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			errorList.setSelected(this);

			if (doubleClick) {
				open();
				return true;
			}

			var mc = Minecraft.getInstance();
			mc.keyboardHandler.setClipboard(String.join("\n", line.stackTrace));
			return true;
		}

		@Nullable
		private String fixSource(@Nullable String source) {
			if (source != null && !source.isEmpty()) {
				int c = source.indexOf(':');

				if (c >= 0) {
					return source.substring(c + 1);
				}
			}

			return source;
		}

		public void open() {
			var path = line.externalFile == null ? (line.sourceLines.isEmpty() || line.sourceLines.iterator().next().source().isEmpty() ? null : line.console.scriptType.path.resolve(fixSource(line.sourceLines.iterator().next().source()))) : line.externalFile;

			if (path != null && Files.exists(path)) {
				try {
					if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
						Desktop.getDesktop().browseFileDirectory(path.toFile());
					} else {
						throw new IllegalStateException("Error");
					}
				} catch (Exception ignored) {
					if (Files.isRegularFile(path) && !path.getFileName().toString().endsWith(".js")) {
						path = path.getParent();
					}

					int ln = 1;

					for (var line : line.sourceLines) {
						if (line.line() > 0 && line.source().endsWith(".js")) {
							ln = line.line();
							break;
						}
					}

					EditorExt.openFile(path, ln, 0);
				}
			}
		}
	}
}