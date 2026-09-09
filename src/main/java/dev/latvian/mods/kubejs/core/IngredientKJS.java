package dev.latvian.mods.kubejs.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.ingredient.WildcardIngredient;
import dev.latvian.mods.kubejs.item.ItemPredicate;
import dev.latvian.mods.kubejs.item.ItemStackSet;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.SizedIngredientWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.Replaceable;
import dev.latvian.mods.kubejs.util.WithCodec;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

@RemapPrefixForJS("kjs$")
public interface IngredientKJS extends ItemPredicate, Replaceable, WithCodec, ItemMatch {
	default Ingredient kjs$self() {
		throw new NoMixinException();
	}

	@Override
	default ItemStack[] kjs$getStackArray() {
		return kjs$self().getItems();
	}

	@Override
	default ItemStackSet kjs$getDisplayStacks() {
		return new ItemStackSet(kjs$self().getItems());
	}

	default Ingredient kjs$and(Ingredient ingredient) {
		return ingredient == Ingredient.EMPTY ? kjs$self() : this == Ingredient.EMPTY ? ingredient : IntersectionIngredient.of(kjs$self(), ingredient);
	}

	default Ingredient kjs$or(Ingredient ingredient) {
		return ingredient == Ingredient.EMPTY ? kjs$self() : this == Ingredient.EMPTY ? ingredient : CompoundIngredient.of(kjs$self(), ingredient);
	}

	default Ingredient kjs$except(Ingredient subtracted) {
		return DifferenceIngredient.of(kjs$self(), subtracted);
	}

	default SizedIngredient kjs$asStack() {
		if (kjs$self().isEmpty()) {
			return SizedIngredientWrapper.empty;
		}

		return new SizedIngredient(kjs$self(), 1);
	}

	default SizedIngredient kjs$withCount(int count) {
		return new SizedIngredient(kjs$self(), count);
	}

	@Override
	default boolean kjs$isWildcard() {
		return kjs$self().getCustomIngredient() == WildcardIngredient.INSTANCE;
	}

	@Override
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
			var stacks = kjs$getStacks();
			return stacks.size() == 1 && ItemStack.isSameItemSameComponents(stacks.getFirst(), item);
		} else {
			return test(item);
		}
	}

	@Override
	default boolean matches(RecipeMatchContext cx, Ingredient in, boolean exact) {
		if (in == Ingredient.EMPTY) {
			return false;
		}

		if (exact) {
			var t1 = IngredientWrapper.tagKeyOf(kjs$self());
			var t2 = IngredientWrapper.tagKeyOf(in);

			if (t1 != null && t2 != null) {
				return t1 == t2;
			} else {
				return equals(in);
			}
		}

		try {
			for (var stack : in.getItems()) {
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

	default boolean kjs$containsAnyTag() {
		return IngredientWrapper.containsAnyTag(kjs$self());
	}

	default String kjs$toIngredientString(@Nullable DynamicOps<Tag> ops) {
		var in = kjs$self();

		if (in.isEmpty()) {
			return "air";
		}

		var items = kjs$getStackArray();

		if (items.length == 0) {
			return "air";
		} else if (items.length == 1) {
			return items[0].kjs$toItemString0(null);
		} else {
			return Ingredient.CODEC.encodeStart(ops, in).getOrThrow().toString();
		}
	}
}
