package dev.latvian.mods.kubejs.util;

import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public interface PlatformUtil {
	static void openFile(File file) {
		openFile(file.toPath(), 1, 1);
	}

	static void openFile(Path path) {
		openFile(path, 1, 1);
	}

	static void openFile(File file, int line, int column) {
		openFile(file.toPath(), line, column);
	}

	static void openFile(Path path, int line, int column) {
		var custom = DevProperties.get().openUriFormat;

		if (!custom.isBlank()) {
			openUri(format(custom, path, line, column));
		} else {
			openPath(path);
		}
	}

	static void openPath(File file) {
		openPath(file.toPath());
	}

	static void openPath(Path path) {
		openUri(path.toAbsolutePath().toUri());
	}

	static void openUri(URI uri) {
		String target = uri.toString();
		String[] cmd = switch (Util.getPlatform()) {
			case WINDOWS -> new String[]{"rundll32", "url.dll,FileProtocolHandler", target};
			case OSX -> new String[]{"open", target};
			default -> new String[]{"xdg-open", target};
		};

		try {
			new ProcessBuilder(cmd).inheritIO().start();
		} catch (IOException e) {
			KubeJS.LOGGER.error("Failed to open URI {}", target, e);
		}
	}

	private static URI format(String scheme, Path path, int line, int column) {
		return URI.create(scheme
			.replace("{path}", path.toAbsolutePath().toUri().getRawPath())
			.replace("{line}", String.valueOf(line))
			.replace("{col}", String.valueOf(column))
		);
	}
}