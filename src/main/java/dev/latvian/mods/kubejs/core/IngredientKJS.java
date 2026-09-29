package dev.latvian.mods.kubejs.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.holder.KubeJSHolderSet;
import dev.latvian.mods.kubejs.ingredient.CreativeTabIngredient;
import dev.latvian.mods.kubejs.item.ItemPredicate;
import dev.latvian.mods.kubejs.item.ItemStackSet;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.Replaceable;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.WithCodec;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.registries.holdersets.AnyHolderSet;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@RemapPrefixForJS("kjs$")
public interface IngredientKJS extends ItemPredicate, Replaceable, WithCodec, ItemMatch {
	default Ingredient kjs$self() {
		throw new NoMixinException();
	}

	@Override
	default ItemStack[] kjs$getStackArray() {
		return kjs$self().items()
			.map(ItemStack::new)
			.toArray(ItemStack[]::new);
	}

	@Override
	default ItemStackSet kjs$getDisplayStacks() {
		var set = new ItemStackSet();

		kjs$self().display()
			.resolve(ContextMap.EMPTY, SlotDisplay.ItemStackContentsFactory.INSTANCE)
			.forEachOrdered(set::add);

		return set;
	}

	default Ingredient kjs$and(Ingredient ingredient) {
		return IntersectionIngredient.of(kjs$self(), ingredient);
	}

	default Ingredient kjs$or(Ingredient ingredient) {
		return CompoundIngredient.of(kjs$self(), ingredient);
	}

	default Ingredient kjs$except(Ingredient subtracted) {
		return DifferenceIngredient.of(kjs$self(), subtracted);
	}

	default SizedIngredient kjs$asStack() {
		return new SizedIngredient(kjs$self(), 1);
	}

	default SizedIngredient kjs$withCount(int count) {
		return new SizedIngredient(kjs$self(), count);
	}

	@Override
	default boolean kjs$isWildcard() {
		return kjs$self().values instanceof AnyHolderSet<?>;
	}

	default Ingredient kjs$asIngredient() {
		return kjs$self();
	}

	@Override
	default Codec<?> getCodec(Context cx) {
		return Ingredient.CODEC;
	}

	@Override
	default Object replaceThisWith(RecipeScriptContext cx, Object with) {
		var t = kjs$self();
		var r = IngredientWrapper.wrap(cx.cx(), with);

		if (!r.equals(t)) {
			return r;
		}

		return this;
	}

	@Override
	default boolean matches(RecipeMatchContext cx, ItemStack item, boolean exact) {
		if (item.isEmpty()) {
			return false;
		} else if (exact) {
			var display = kjs$self().display();
			// TODO: non-empty context map?
			var stacks = display.resolveForStacks(ContextMap.EMPTY);
			return stacks.size() == 1 && ItemStack.isSameItemSameComponents(stacks.getFirst(), item);
		} else {
			return test(item);
		}
	}

	@Override
	default boolean matches(RecipeMatchContext cx, Ingredient in, boolean exact) {
		if (exact) {
			return Objects.equals(kjs$self(), in);
		}

		try {
			var items = in.items()
				.map(ItemStack::new)
				.toArray(ItemStack[]::new);
			for (var stack : items) {
				if (test(stack)) {
					return true;
				}
			}
		} catch (Exception ex) {
			throw new KubeRuntimeException("Failed to test ingredient " + in, ex);
		}

		return false;
	}

	@Nullable
	default TagKey<Item> kjs$getTagKey() {
		return IngredientWrapper.tagKeyOf(kjs$self());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static @Nullable String setToString(HolderSet<Item> in, DataComponentPatch components, @Nullable DynamicOps<Tag> ops) {
		Function<Holder<?>, String> holderToString = holder ->
			new ItemStack(Cast.to(holder), 1, components).kjs$toItemString0(ops);
		return switch (in) {
			case HolderSet.Direct direct -> {
				List<String> list = direct.stream().map(holderToString).toList();
				yield list.size() == 1 ? list.getFirst() : list.toString();
			}
			case HolderSet.Named<?> tag -> "#" + ID.reduce(tag.key().location());
			case OrHolderSet<?> or -> {
				List<String> children = new ArrayList<>();
				for (HolderSet child : or.getComponents()) {
					var result = setToString(child, components, ops);
					if (result == null) {
						yield null;
					}

					children.add(result);
				}
				yield children.size() == 1 ? children.getFirst() : children.toString();
			}
			case AnyHolderSet<?> any -> "*";
			case KubeJSHolderSet kjs -> kjs.kjs$toIngredientString(holderToString);
			default -> null;
		};
	}

	default ItemStack kjs$getFirst() {
		// TODO (again!): context?
		return kjs$self().display().resolveForFirstStack(ContextMap.EMPTY);
	}

	default String kjs$toIngredientString(@Nullable DynamicOps<Tag> ops) {
		var in = kjs$self();

		switch (in.getCustomIngredient()) {
			case DataComponentIngredient dci -> {
				var string = setToString(dci.itemSet(), dci.components(), ops);
				if (string != null) {
					return string;
				}
			}
			case null -> {
				var string = setToString(in.values, DataComponentPatch.EMPTY, ops);
				if (string != null) {
					return string;
				}
			}
			case CreativeTabIngredient(var tab) -> {
				return "%" + tab;
			}
			default -> {
			}
		}

		return Ingredient.CODEC.encodeStart(ops, in).getOrThrow().toString();
	}
}
