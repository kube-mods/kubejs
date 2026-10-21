package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import dev.latvian.mods.kubejs.color.KubeColor;
import dev.latvian.mods.kubejs.color.NoColor;
import dev.latvian.mods.kubejs.color.SimpleColor;
import dev.latvian.mods.kubejs.color.SimpleColorWithAlpha;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public interface ColorWrapper {
	Map<String, KubeColor> MAP = new HashMap<>();
	Map<String, ChatFormatting> TEXT = Util.make(new HashMap<>(), map -> {
		for (ChatFormatting c : ChatFormatting.values()) {
			map.put(c.name(), c);
		}
	});
	Map<String, DyeColor> DYE = Util.make(new HashMap<>(), map -> {
		for (DyeColor c : DyeColor.values()) {
			map.put(c.getName(), c);
		}
	});

	static KubeColor wrap(@Nullable Object o) {
		return switch (o) {
			case KubeColor kubeColor -> kubeColor;
			case CharSequence cs -> {
				String s = cs.toString();
				KubeColor c = MAP.get(s);

				if (c != null) {
					yield c;
				}

				if (s.startsWith("#")) {
					int col = Long.decode(s).intValue();
					yield s.length() == 7 ? new SimpleColor(col) : new SimpleColorWithAlpha(col);
				}

				yield NONE;
			}
			case Number number when number.intValue() != 0 -> new SimpleColor(number.intValue());
			case null, default -> NONE;
		};
	}

	static TextColor wrapTextColor(Object o) {
		return wrap(o).kjs$createTextColor();
	}

	static ColorRGBA wrapColorRGBA(Object o) {
		return new ColorRGBA(wrap(o).kjs$getARGB());
	}

	static KubeColor createMapped(Object o, String... names) {
		KubeColor c = wrap(o);

		for (String s : names) {
			MAP.put(s, c);
		}

		return c;
	}

	KubeColor NONE = createMapped(new NoColor(), "NONE", "none", "", "-", "transparent");

	static KubeColor rgba(int r, int g, int b, int a) {
		return new SimpleColor((r << 16) | (g << 8) | b | (a << 24));
	}

	KubeColor BLACK = createMapped(ChatFormatting.BLACK, "BLACK", "black");
	KubeColor DARK_BLUE = createMapped(ChatFormatting.DARK_BLUE, "DARK_BLUE", "dark_blue", "darkBlue");
	KubeColor DARK_GREEN = createMapped(ChatFormatting.DARK_GREEN, "DARK_GREEN", "dark_green", "darkGreen");
	KubeColor DARK_AQUA = createMapped(ChatFormatting.DARK_AQUA, "DARK_AQUA", "dark_aqua", "darkAqua");
	KubeColor DARK_RED = createMapped(ChatFormatting.DARK_RED, "DARK_RED", "dark_red", "darkRed");
	KubeColor DARK_PURPLE = createMapped(ChatFormatting.DARK_PURPLE, "DARK_PURPLE", "dark_purple", "darkPurple");
	KubeColor GOLD = createMapped(ChatFormatting.GOLD, "GOLD", "gold");
	KubeColor GRAY = createMapped(ChatFormatting.GRAY, "GRAY", "gray");
	KubeColor DARK_GRAY = createMapped(ChatFormatting.DARK_GRAY, "DARK_GRAY", "dark_gray", "darkGray");
	KubeColor BLUE = createMapped(ChatFormatting.BLUE, "BLUE", "blue");
	KubeColor GREEN = createMapped(ChatFormatting.GREEN, "GREEN", "green");
	KubeColor AQUA = createMapped(ChatFormatting.AQUA, "AQUA", "aqua");
	KubeColor RED = createMapped(ChatFormatting.RED, "RED", "red");
	KubeColor LIGHT_PURPLE = createMapped(ChatFormatting.LIGHT_PURPLE, "LIGHT_PURPLE", "light_purple", "lightPurple");
	KubeColor YELLOW = createMapped(ChatFormatting.YELLOW, "YELLOW", "yellow");
	KubeColor WHITE = createMapped(ChatFormatting.WHITE, "WHITE", "white");

	KubeColor WHITE_DYE = createMapped(DyeColor.WHITE, "WHITE_DYE", "white_dye", "whiteDye");
	KubeColor ORANGE_DYE = createMapped(DyeColor.ORANGE, "ORANGE_DYE", "orange_dye", "orangeDye");
	KubeColor MAGENTA_DYE = createMapped(DyeColor.MAGENTA, "MAGENTA_DYE", "magenta_dye", "magentaDye");
	KubeColor LIGHT_BLUE_DYE = createMapped(DyeColor.LIGHT_BLUE, "LIGHT_BLUE_DYE", "light_blue_dye", "lightBlueDye");
	KubeColor YELLOW_DYE = createMapped(DyeColor.YELLOW, "YELLOW_DYE", "yellow_dye", "yellowDye");
	KubeColor LIME_DYE = createMapped(DyeColor.LIME, "LIME_DYE", "lime_dye", "limeDye");
	KubeColor PINK_DYE = createMapped(DyeColor.PINK, "PINK_DYE", "pink_dye", "pinkDye");
	KubeColor GRAY_DYE = createMapped(DyeColor.GRAY, "GRAY_DYE", "gray_dye", "grayDye");
	KubeColor LIGHT_GRAY_DYE = createMapped(DyeColor.LIGHT_GRAY, "LIGHT_GRAY_DYE", "light_gray_dye", "lightGrayDye");
	KubeColor CYAN_DYE = createMapped(DyeColor.CYAN, "CYAN_DYE", "cyan_dye", "cyanDye");
	KubeColor PURPLE_DYE = createMapped(DyeColor.PURPLE, "PURPLE_DYE", "purple_dye", "purpleDye");
	KubeColor BLUE_DYE = createMapped(DyeColor.BLUE, "BLUE_DYE", "blue_dye", "blueDye");
	KubeColor BROWN_DYE = createMapped(DyeColor.BROWN, "BROWN_DYE", "brown_dye", "brownDye");
	KubeColor GREEN_DYE = createMapped(DyeColor.GREEN, "GREEN_DYE", "green_dye", "greenDye");
	KubeColor RED_DYE = createMapped(DyeColor.RED, "RED_DYE", "red_dye", "redDye");
	KubeColor BLACK_DYE = createMapped(DyeColor.BLACK, "BLACK_DYE", "black_dye", "blackDye");
}
