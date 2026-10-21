package dev.latvian.mods.kubejs.client;

import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.util.PlatformUtil;

import java.net.URI;
import java.nio.file.Path;

public class EditorExt {
	public static final String VSCODE = vsLikeScheme("vscode");
	public static final String VSCODIUM = vsLikeScheme("vscodium");
	public static final String VSCODE_OSS = vsLikeScheme("vscode-oss");

	private static String vsLikeScheme(String prefix) {
		return prefix + "://file{path}:{line}:{col}";
	}

	public static boolean isKnownVSCode() {
		var custom = DevProperties.get().openUriFormat;
		return !custom.isEmpty() && (custom.equals(VSCODE) || custom.equals(VSCODIUM) || custom.equals(VSCODE_OSS));
	}

	private static URI format(String scheme, Path path, int line, int column) {
		return URI.create(scheme
			.replace("{path}", path.toAbsolutePath().toUri().getRawPath())
			.replace("{line}", String.valueOf(line))
			.replace("{col}", String.valueOf(column))
		);
	}

	public static void openFile(Path path, int line, int column) {
		var custom = DevProperties.get().openUriFormat;
		if (!custom.isBlank()) {
			PlatformUtil.openUri(format(custom, path, line, column));
		} else {
			PlatformUtil.openPath(path);
		}
	}
}