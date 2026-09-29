package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import dev.latvian.mods.kubejs.util.Lazy;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public interface SDLInputWrapper {
	Lazy<Map<String, Integer>> MAP = Lazy.map(map -> {
		try {
			for (var clazz : List.of(SDLKeycode.class, SDLMouse.class, SDLGamepad.class)) {
				for (var field : clazz.getFields()) { // public fields only
					int mod = field.getModifiers();

					if (field.getType() != int.class || !Modifier.isStatic(mod) || !Modifier.isFinal(mod)) {
						continue;
					}

					var name = mapName(field.getName());

					if (name != null) {
						map.put(name, field.getInt(null));
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	});

	private static @Nullable String mapName(String n) {
		return switch (n) {
			case String s when s.startsWith("SDLK_") -> "KEY_" + s.substring(5);
			case String s when s.startsWith("SDL_KMOD_") -> "MOD_" + s.substring(9);
			case String s when s.startsWith("SDL_BUTTON_") -> "MOUSE_" + s.substring(11);
			case String s when s.startsWith("SDL_SYSTEM_CURSOR_") -> "CURSOR_" + s.substring(18);
			case String s when s.startsWith("SDL_GAMEPAD_BUTTON_") || s.startsWith("SDL_GAMEPAD_AXIS_") -> "GAMEPAD_" + s.substring(12);
			default -> null;
		};
	}

	static int get(String name) {
		return MAP.get().getOrDefault(name, -1);
	}
}