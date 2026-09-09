package dev.latvian.mods.kubejs.recipe.component;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.Wrapper;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RecipeComponentValueMap extends AbstractMap<RecipeKey<?>, Object> {
	public static final RecipeComponentValueMap EMPTY = new RecipeComponentValueMap(RecipeComponentValue.EMPTY_ARRAY);

	public final RecipeComponentValue<?>[] holders;
	private Set<Entry<RecipeKey<?>, Object>> holderSet;

	public RecipeComponentValueMap(RecipeComponentValue<?>[] holders) {
		this.holders = new RecipeComponentValue[holders.length];

		for (int i = 0; i < holders.length; i++) {
			this.holders[i] = holders[i].copy();
		}
	}

	public RecipeComponentValueMap(List<RecipeKey<?>> keys) {
		this.holders = new RecipeComponentValue[keys.size()];

		for (int i = 0; i < holders.length; i++) {
			this.holders[i] = new RecipeComponentValue<>(keys.get(i), i);
		}
	}

	@NotNull
	@Override
	public Set<Map.Entry<RecipeKey<?>, Object>> entrySet() {
		if (holderSet == null) {
			holderSet = Cast.to(new LinkedHashSet<>(Arrays.asList(holders)));
		}

		return holderSet;
	}

	@Override
	public Object put(RecipeKey<?> key, Object value) {
		for (var holder : holders) {
			if (holder.key == key) {
				return holder.setValue(Cast.to(Wrapper.unwrapped(value)));
			}
		}

		throw new IllegalArgumentException("Key " + key + " is not in this map!");
	}

	public RecipeComponentValue<?> getHolder(Object key) {
		for (var holder : holders) {
			if (holder.key == key) {
				return holder;
			}
		}

		return null;
	}

	@Override
	public Object get(Object key) {
		var h = getHolder(key);
		return h == null ? null : h.value;
	}

	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		var v = get(key);
		return v == null ? defaultValue : v;
	}

	@Override
	public int hashCode() {
		int i = 1;

		for (var holder : holders) {
			i = 31 * i + holder.key.hashCode();
			i = 31 * i + Objects.hashCode(holder.value);
		}

		return i;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof RecipeComponentValueMap map) {
			if (holders.length != map.holders.length) {
				return false;
			} else {
				for (int i = 0; i < holders.length; i++) {
					if (holders[i].key != map.holders[i].key || !Objects.equals(holders[i].value, map.holders[i].value)) {
						return false;
					}
				}

				return true;
			}
		} else {
			return false;
		}
	}
}