package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.codec.KubeJSCodecUtil;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.JSObjectType;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.kubejs.util.UtilsJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Info("The hub for all things text components. Format text to your hearts content!")
public interface TextWrapper {
	TypeInfo TYPE_INFO = TypeInfo.of(MutableComponent.class);

	@Info("Returns a Component of the input")
	static MutableComponent of(MutableComponent component) {
		return component;
	}

	static ItemLore lore(List<Component> lore) {
		return new ItemLore(lore);
	}

	@HideFromJS
	static MutableComponent wrap(Context cx, @Nullable Object o) {
		var from = UtilsJS.wrap(o, JSObjectType.ANY);

		return switch (from) {
			case null -> Component.literal("null");
			case MutableComponent component -> component;
			case Component component -> component.copy();
			case Enum<?> e -> ofString(e.name());
			case StringTag tag -> {
				var s = tag.getAsString();
				if (s.startsWith("{") && s.endsWith("}")) {
					yield ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, JsonUtils.GSON.fromJson(s, JsonObject.class))
						.mapOrElse(Pair::getFirst, error -> Component.literal("Error: " + error))
						.copy();
				} else {
					yield ofString(s);
				}
			}
			case Tag tag -> (MutableComponent) NbtUtils.toPrettyComponent(tag);
			case JsonElement json -> ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, json)
				.mapOrElse(Pair::getFirst, error -> Component.literal("Error: " + error))
				.copy();
			case Map<?, ?> map when map.containsKey("text") || map.containsKey("translate") -> {
				MutableComponent text;

				if (map.containsKey("text")) {
					text = ofString(map.get("text").toString());
				} else {
					Object[] with;

					if (map.get("with") instanceof Collection<?> a) {
						with = new Object[a.size()];
						var i = 0;

						for (var e1 : a) {
							with[i] = e1;

							if (with[i] instanceof Map<?, ?> || with[i] instanceof Collection<?>) {
								with[i] = wrap(cx, e1);
							}

							i++;
						}
					} else {
						with = new Object[0];
					}

					text = Component.translatable(map.get("translate").toString(), with);
				}

				if (map.containsKey("color")) {
					text.kjs$color(ColorWrapper.wrap(map.get("color")));
				}

				text.kjs$bold((Boolean) map.getOrDefault("bold", null));
				text.kjs$italic((Boolean) map.getOrDefault("italic", null));
				text.kjs$underlined((Boolean) map.getOrDefault("underlined", null));
				text.kjs$strikethrough((Boolean) map.getOrDefault("strikethrough", null));
				text.kjs$obfuscated((Boolean) map.getOrDefault("obfuscated", null));

				text.kjs$insertion((String) map.getOrDefault("insertion", null));
				text.kjs$font(map.containsKey("font") ? ResourceLocation.parse(map.get("font").toString()) : null);
				text.kjs$click(map.containsKey("click") ? wrapClickEvent(cx, map.get("click")) : null);
				text.kjs$hover(map.containsKey("hover") ? wrap(cx, map.get("hover")) : null);

				if (map.get("extra") instanceof Iterable<?> itr) {
					for (var e : itr) {
						text.append(wrap(cx, e));
					}
				}

				yield text;
			}
			case Map<?, ?> map -> wrap(cx, JsonUtils.of(cx, map));
			case Iterable<?> list -> {
				var text = Component.empty();
				for (var e1 : list) {
					text.append(wrap(cx, e1));
				}
				yield text;
			}
			default -> ofString(from.toString());
		};

	}

	static Component ofTag(Tag tag) {
		try {
			return ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow().getFirst();
		} catch (JsonParseException ex) {
			return Component.literal("Error: " + ex);
		}
	}

	@Info("Returns a plain component of the string, or empty if it is an empty string")
	static MutableComponent ofString(String s) {
		return s.isEmpty() ? Component.empty() : Component.literal(s);
	}

	@Info("Checks if the passed in component, and all its children are empty")
	static boolean isEmpty(Component component) {
		return component.getContents() == PlainTextContents.EMPTY && component.getSiblings().isEmpty();
	}

	@Info("Returns a ClickEvent of the input")
	static ClickEvent clickEventOf(ClickEvent event) {
		return event;
	}

	@HideFromJS
	static ClickEvent wrapClickEvent(Context cx, Object o) {
		if (o == null) {
			return null;
		} else if (o instanceof ClickEvent ce) {
			return ce;
		}

		var json = JsonUtils.objectOf(cx, o);

		if (json != null) {
			var action = GsonHelper.getAsString(json, "action");
			var value = GsonHelper.getAsString(json, "value");
			return KubeJSCodecUtil.fromJsonOrThrow(json, ClickEvent.CODEC);
		}

		var s = o.toString();

		var split = s.split(":", 2);

		return switch (split[0]) {
			case "command" -> new ClickEvent(ClickEvent.Action.RUN_COMMAND, split[1]);
			case "suggest_command" -> new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, split[1]);
			case "copy" -> new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, split[1]);
			case "file" -> new ClickEvent(ClickEvent.Action.OPEN_FILE, split[1]);
			default -> {
				for (var a : ClickEvent.Action.values()) {
					if (a.getSerializedName().equals(split[0])) {
						yield new ClickEvent(a, split[1]);
					}
				}

				yield new ClickEvent(ClickEvent.Action.OPEN_URL, s);
			}
		};
	}

	@Info("Returns a colorful representation of the input nbt. Useful for displaying NBT to the player")
	static Component prettyPrintNbt(Tag tag) {
		return NbtUtils.toPrettyComponent(tag);
	}

	@Info("Joins all components in the list with the separator component")
	static MutableComponent joinSep(MutableComponent separator, Iterable<? extends Component> texts) {
		var joined = Component.empty();
		var first = true;

		for (var t : texts) {
			if (first) {
				first = false;
			} else if (!isEmpty(separator)) {
				joined.append(separator);
			}

			joined.append(t);
		}

		return joined;
	}

	@Info("Joins all components without a separator")
	static MutableComponent join(Component... texts) {
		return joinSep(Component.empty(), Arrays.asList(texts));
	}

	@Info("Returns an empty component")
	static MutableComponent empty() {
		return Component.empty();
	}

	@Info("Returns a plain component of the passed in string, even if empty")
	static MutableComponent string(String text) {
		return Component.literal(text);
	}

	@Info("Returns a plain component of the input")
	static MutableComponent literal(String text) {
		return Component.literal(text);
	}

	@Info("Returns a translatable component of the input key")
	static MutableComponent translate(String key) {
		return Component.translatable(key);
	}

	@Info("Returns a translatable component of the input key, with args of the objects")
	static MutableComponent translate(String key, Object... objects) {
		return Component.translatable(key, objects);
	}

	@Info("Returns a translatable component of the input key, with args of the objects and a fallback translation in case the client does not have one")
	static MutableComponent translateWithFallback(String key, String fallback) {
		return Component.translatableWithFallback(key, fallback);
	}

	@Info("Returns a translatable component of the input key, with args of the objects and a fallback translation in case the client does not have one")
	static MutableComponent translateWithFallback(String key, String fallback, Object... objects) {
		return Component.translatableWithFallback(key, fallback, objects);
	}

	@Info("Returns a translatable component of the input key")
	static MutableComponent translatable(String key) {
		return Component.translatable(key);
	}

	@Info("Returns a translatable component of the input key, with args of the objects")
	static MutableComponent translatable(String key, Object... objects) {
		return Component.translatable(key, objects);
	}

	@Info("Returns a translatable component of the input key, with args of the objects and a fallback translation in case the client does not have one")
	static MutableComponent translatableWithFallback(String key, String fallback) {
		return Component.translatableWithFallback(key, fallback);
	}

	@Info("Returns a translatable component of the input key, with args of the objects and a fallback translation in case the client does not have one")
	static MutableComponent translatableWithFallback(String key, String fallback, Object... objects) {
		return Component.translatableWithFallback(key, fallback, objects);
	}

	@Info("Returns a keybinding component of the input keybinding descriptor")
	static MutableComponent keybind(String keybind) {
		return Component.keybind(keybind);
	}

	@Info("Returns a score component of the input objective, for the provided selector")
	static MutableComponent score(String selector, String objective) {
		return MutableComponent.create(new ScoreContents(selector, objective));
	}

	@Info("Returns a component displaying all entities matching the input selector")
	static MutableComponent selector(String selector) {
		return MutableComponent.create(new SelectorContents(selector, Optional.empty()));
	}

	@Info("Returns a component displaying all entities matching the input selector, with a custom separator")
	static MutableComponent selector(String selector, Component separator) {
		return MutableComponent.create(new SelectorContents(selector, Optional.of(separator)));
	}

	@Info("Returns a component of the input, colored black")
	static MutableComponent black(MutableComponent text) {
		return text.kjs$black();
	}

	@Info("Returns a component of the input, colored dark blue")
	static MutableComponent darkBlue(MutableComponent text) {
		return text.kjs$darkBlue();
	}

	@Info("Returns a component of the input, colored dark green")
	static MutableComponent darkGreen(MutableComponent text) {
		return text.kjs$darkGreen();
	}

	@Info("Returns a component of the input, colored dark aqua")
	static MutableComponent darkAqua(MutableComponent text) {
		return text.kjs$darkAqua();
	}

	@Info("Returns a component of the input, colored dark red")
	static MutableComponent darkRed(MutableComponent text) {
		return text.kjs$darkRed();
	}

	@Info("Returns a component of the input, colored dark purple")
	static MutableComponent darkPurple(MutableComponent text) {
		return text.kjs$darkPurple();
	}

	@Info("Returns a component of the input, colored gold")
	static MutableComponent gold(MutableComponent text) {
		return text.kjs$gold();
	}

	@Info("Returns a component of the input, colored gray")
	static MutableComponent gray(MutableComponent text) {
		return text.kjs$gray();
	}

	@Info("Returns a component of the input, colored dark gray")
	static MutableComponent darkGray(MutableComponent text) {
		return text.kjs$darkGray();
	}

	@Info("Returns a component of the input, colored blue")
	static MutableComponent blue(MutableComponent text) {
		return text.kjs$blue();
	}

	@Info("Returns a component of the input, colored green")
	static MutableComponent green(MutableComponent text) {
		return text.kjs$green();
	}

	@Info("Returns a component of the input, colored aqua")
	static MutableComponent aqua(MutableComponent text) {
		return text.kjs$aqua();
	}

	@Info("Returns a component of the input, colored red")
	static MutableComponent red(MutableComponent text) {
		return text.kjs$red();
	}

	@Info("Returns a component of the input, colored light purple")
	static MutableComponent lightPurple(MutableComponent text) {
		return text.kjs$lightPurple();
	}

	@Info("Returns a component of the input, colored yellow")
	static MutableComponent yellow(MutableComponent text) {
		return text.kjs$yellow();
	}

	@Info("Returns a component of the input, colored white")
	static MutableComponent white(MutableComponent text) {
		return text.kjs$white();
	}

	static MutableComponent info(Component text) {
		return Component.empty().append(TextIcons.info()).append(TextIcons.smallSpace()).append(text);
	}

	static MutableComponent warn(Component text) {
		return Component.empty().append(TextIcons.info()).append(TextIcons.smallSpace()).append(text);
	}
}