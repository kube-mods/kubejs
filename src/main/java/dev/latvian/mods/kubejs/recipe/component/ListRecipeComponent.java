package dev.latvian.mods.kubejs.recipe.component;

import com.google.gson.JsonArray;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.codec.KubeJSCodecUtil;
import dev.latvian.mods.kubejs.error.RecipeComponentTooLargeException;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record ListRecipeComponent<T>(
	RecipeComponent<T> component,
	boolean canWriteSelf,
	TypeInfo listTypeInfo,
	Codec<List<T>> listCodec,
	boolean conditional,
	IntBounds bounds,
	Optional<RecipeComponent<?>> spread,
	Optional<RecipeComponent<?>> spreadWrap
) implements RecipeComponent<List<T>> {
	public static <L> ListRecipeComponent<L> create(RecipeComponent<L> component, boolean canWriteSelf, boolean conditional) {
		return create(component, canWriteSelf, conditional, IntBounds.DEFAULT, Optional.empty());
	}

	public static <L> ListRecipeComponent<L> create(RecipeComponent<L> component, boolean canWriteSelf, boolean conditional, IntBounds bounds, Optional<RecipeComponent<?>> spread) {
		var typeInfo = component.typeInfo();
		var codec = component.codec();
		var listCodec = conditional ? NeoForgeExtraCodecs.listWithOptionalElements(ConditionalOps.createConditionalCodec(codec)) : codec.listOf();
		var listTypeInfo = TypeInfo.RAW_LIST.withParams(typeInfo);

		if (canWriteSelf) {
			listCodec = KubeJSCodecUtil.listOfOrSelf(listCodec, codec);
			listTypeInfo = listTypeInfo.or(typeInfo);
		}

		Optional<RecipeComponent<?>> spreadWrap = wrapSpread(component, spread);

		return new ListRecipeComponent<>(component, canWriteSelf, listTypeInfo, listCodec, conditional, bounds, spread, spreadWrap);
	}

	private static <L> @NotNull Optional<RecipeComponent<?>> wrapSpread(RecipeComponent<L> component, Optional<RecipeComponent<?>> spread) {
		Optional<RecipeComponent<?>> spreadWrap = spread;

		if (spread.isPresent()) {
			if (component instanceof EitherRecipeComponent<?, ?> either && spread.get() instanceof EitherRecipeComponent<?, ?> seither && (seither.left().isIgnored() || seither.right().isIgnored())) {
				spreadWrap = Optional.of(seither.left().isIgnored() ? either.left().or(seither.right()) : seither.left().or(either.right()));
			}
		}
		return spreadWrap;
	}

	public static final RecipeComponentType<?> TYPE = RecipeComponentType.<ListRecipeComponent<?>>dynamic(KubeJS.id("list"), (type, ctx) -> RecordCodecBuilder.mapCodec(instance -> instance.group(
		ctx.recipeComponentCodec().fieldOf("component").forGetter(dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent::component),
		Codec.BOOL.optionalFieldOf("can_write_self", false).forGetter(dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent::canWriteSelf),
		Codec.BOOL.optionalFieldOf("conditional", false).forGetter(dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent::conditional),
		IntBounds.MAP_CODEC.forGetter(dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent::bounds),
		ctx.recipeComponentCodec().optionalFieldOf("spread").forGetter(dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent::spread)
	).apply(instance, dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent::create)));

	@Override
	public RecipeComponentType<?> type() {
		return TYPE;
	}

	@Override
	public Codec<List<T>> codec() {
		return listCodec;
	}

	@Override
	public TypeInfo typeInfo() {
		return listTypeInfo;
	}

	@Override
	public boolean hasPriority(RecipeMatchContext cx, Object from) {
		return from instanceof Iterable<?> || from != null && from.getClass().isArray();
	}

	public static <T> List<T> wrap0(RecipeScriptContext cx, RecipeComponent<T> component, Object from) {
		if (from instanceof Iterable<?> iterable) {
			int size;

			if (iterable instanceof Collection<?> c) {
				size = c.size();
			} else if (iterable instanceof JsonArray a) {
				size = a.size();
			} else {
				size = -1;
			}

			if (size == 0) {
				return List.of();
			} else if (size == 1) {
				return List.of(component.wrap(cx, iterable.iterator().next()));
			} else if (size == 2) {
				var itr = iterable.iterator();
				return List.of(component.wrap(cx, itr.next()), component.wrap(cx, itr.next()));
			} else if (size > 0) {
				var arr = new ArrayList<T>(size);

				for (var e : iterable) {
					arr.add(component.wrap(cx, e));
				}

				return arr;
			} else {
				var list = new ArrayList<T>();

				for (var e : iterable) {
					list.add(component.wrap(cx, e));
				}

				return list;
			}
		} else if (from.getClass().isArray()) {
			int length = Array.getLength(from);

			if (length == 0) {
				return List.of();
			}

			var arr = new ArrayList<T>(length);

			for (int i = 0; i < length; i++) {
				arr.add(component.wrap(cx, Array.get(from, i)));
			}

			return arr;
		}

		return List.of(component.wrap(cx, from));
	}

	@Override
	public List<T> wrap(RecipeScriptContext cx, Object from) {
		var spreadComponent = spread.orElse(null);

		if (spreadComponent != null && spreadWrap.isPresent()) {
			var original = wrap0(cx, spreadWrap.get(), from);
			var result = new ArrayList<T>();

			for (var o : original) {
				for (var s : spreadComponent.spread(Cast.to(o))) {
					result.add(component.wrap(cx, s));
				}
			}

			return result;
		} else {
			return wrap0(cx, component, from);
		}
	}

	@Override
	public boolean matches(RecipeMatchContext cx, List<T> value, ReplacementMatchInfo match) {
		for (var v : value) {
			if (component.matches(cx, v, match)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public List<T> replace(RecipeScriptContext cx, List<T> original, ReplacementMatchInfo match, Object with) {
		var arr = original;

		for (int i = 0; i < original.size(); i++) {
			var r = component.replace(cx, original.get(i), match, with);

			if (arr.get(i) != r) {
				if (arr == original) {
					arr = new ArrayList<>(original);
				}

				if (arr != original) {
					arr.set(i, r);
				}
			}
		}

		return arr;
	}

	@Override
	public void buildUniqueId(UniqueIdBuilder builder, List<T> value) {
		for (int i = 0; i < value.size(); i++) {
			if (i > 0) {
				builder.appendSeparator();
			}

			component.buildUniqueId(builder, value.get(i));
		}
	}

	@Override
	public String toString() {
		return component + (canWriteSelf ? "[?]" : "[]") + (conditional ? "?" : "");
	}

	@Override
	public void validate(RecipeValidationContext ctx, List<T> value) {
		RecipeComponent.super.validate(ctx, value);

		if (value.size() > bounds.max()) {
			throw new RecipeComponentTooLargeException(this, value, value.size(), bounds.max());
		}

		ctx.errors().push(this);

		for (int i = 0; i < value.size(); i++) {
			ctx.errors().setKey(i);
			component.validate(ctx, value.get(i));
		}

		ctx.errors().pop();
	}

	@Override
	public boolean allowEmpty() {
		return bounds.min() <= 0;
	}

	@Override
	public boolean isEmpty(List<T> value) {
		return value.isEmpty();
	}

	@Override
	public List<?> spread(List<T> value) {
		return value;
	}

	public ListRecipeComponent<T> withBounds(IntBounds bounds) {
		return ListRecipeComponent.create(component, canWriteSelf, conditional, bounds, spread);
	}

	public ListRecipeComponent<T> orSelf() {
		return ListRecipeComponent.create(component, true, conditional, bounds, spread);
	}

	public ListRecipeComponent<T> asConditional() {
		return ListRecipeComponent.create(component, canWriteSelf, true, bounds, spread);
	}

	public ListRecipeComponent<T> withSpread(Optional<RecipeComponent<?>> spread) {
		return ListRecipeComponent.create(component, canWriteSelf, conditional, bounds, spread);
	}
}
