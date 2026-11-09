package dev.latvian.mods.kubejs.core;

import com.google.gson.JsonElement;
import dev.latvian.mods.kubejs.codec.KubeJSCodecUtil;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.Replaceable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public interface SizedIngredientKJS extends Replaceable, IngredientSupplierKJS, ItemMatch {
	default SizedIngredient kjs$self() {
		return (SizedIngredient) (Object) this;
	}

	@Override
	default Object replaceThisWith(RecipeScriptContext cx, Object with) {
		var ingredient = IngredientWrapper.wrap(cx.cx(), with);

		if (!ingredient.equals(kjs$self().ingredient())) {
			return new SizedIngredient(ingredient, kjs$self().count());
		}

		return this;
	}

	@Override
	default Ingredient kjs$asIngredient() {
		return kjs$self().ingredient();
	}

	@Override
	default boolean matches(RecipeMatchContext cx, ItemStack item, boolean exact) {
		return kjs$self().ingredient().matches(cx, item, exact);
	}

	@Override
	default boolean matches(RecipeMatchContext cx, Ingredient in, boolean exact) {
		return kjs$self().ingredient().matches(cx, in, exact);
	}

	default JsonElement kjs$toFlatJson() {
		return KubeJSCodecUtil.toJsonOrThrow(kjs$self(), SizedIngredient.FLAT_CODEC);
	}

	default JsonElement kjs$toNestedJson() {
		return KubeJSCodecUtil.toJsonOrThrow(kjs$self(), SizedIngredient.NESTED_CODEC);
	}
}
