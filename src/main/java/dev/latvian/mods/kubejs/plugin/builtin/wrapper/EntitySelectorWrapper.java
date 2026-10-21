package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.mojang.brigadier.StringReader;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EntitySelectorWrapper {
	private static final Map<String, EntitySelector> ENTITY_SELECTOR_CACHE = new HashMap<>();
	private static final EntitySelector ALL_ENTITIES_SELECTOR = new EntitySelector(EntitySelector.INFINITE, true, false, List.of(), MinMaxBounds.Doubles.ANY, Function.identity(), null, EntitySelectorParser.ORDER_RANDOM, false, null, null, null, true);

	public static EntitySelector of(EntitySelector selector) {
		return selector;
	}

	@HideFromJS
	public static EntitySelector wrap(Context cx, @Nullable Object o) {
		if (o == null) {
			throw new KubeRuntimeException("EntitySelector cannot be null!").source(SourceLine.of(cx));
		} else if (o instanceof EntitySelector s) {
			return s;
		}

		String s = o.toString();

		if (s.isBlank()) {
			throw new KubeRuntimeException("EntitySelector cannot be blank!").source(SourceLine.of(cx));
		}

		EntitySelector sel;

		if (ENTITY_SELECTOR_CACHE.containsKey(s)) {
			sel = ENTITY_SELECTOR_CACHE.get(s);
		} else {
			try {
				sel = new EntitySelectorParser(new StringReader(s), true).parse();
				ENTITY_SELECTOR_CACHE.put(s, sel);
			} catch (Exception ex) {
				throw new KubeRuntimeException("Failed to parse entity selector '%s'".formatted(s), ex).source(SourceLine.of(cx));
			}
		}

		return sel;
	}
}
