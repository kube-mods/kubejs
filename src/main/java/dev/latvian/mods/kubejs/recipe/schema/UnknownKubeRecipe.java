package dev.latvian.mods.kubejs.recipe.schema;

import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

public class UnknownKubeRecipe extends KubeRecipe {
	public static final KubeRecipeFactory RECIPE_FACTORY = new KubeRecipeFactory(KubeJS.id("unknown"), UnknownKubeRecipe.class, UnknownKubeRecipe::new);

	@Override
	public void deserialize(boolean merge) {
	}

	@Override
	public void serialize() {
	}

	@Override
	public boolean hasInput(RecipeMatchContext cx, ReplacementMatchInfo match) {
		if (CommonProperties.get().matchJsonRecipes && match.match() instanceof ItemMatch m) {
			var recipeJson = json;
			if (recipeJson == null) {
				return false;
			}

			if (recipeJson.has("ingredients") && recipeJson.get("ingredients").isJsonArray()) {
				var arr = recipeJson.getAsJsonArray("ingredients");
				if (arr.isEmpty()) {
					return false;
				}

				for (var el : arr) {
					if (el == null || el.isJsonNull()) {
						continue;
					}

					var ingredient = Ingredient.CODEC.parse(JsonOps.INSTANCE, el).result().orElse(null);
					if (ingredient != null && !ingredient.isEmpty() && ingredient.kjs$canBeUsedForMatching() && m.matches(cx, ingredient, match.exact())) {
						return true;
					}
				}

				return false;
			}

			if (recipeJson.has("ingredient")) {
				var ingredient = Ingredient.CODEC.parse(JsonOps.INSTANCE, recipeJson.get("ingredient")).result().orElse(null);
				return ingredient != null && !ingredient.isEmpty() && ingredient.kjs$canBeUsedForMatching() && m.matches(cx, ingredient, match.exact());
			}
		}

		return false;
	}

	@Override
	public boolean replaceInput(RecipeScriptContext cx, ReplacementMatchInfo match, Object with) {
		return false;
	}

	@Override
	public boolean hasOutput(RecipeMatchContext cx, ReplacementMatchInfo match) {
		if (CommonProperties.get().matchJsonRecipes && match.match() instanceof ItemMatch m) {
			var original = getOriginalRecipe();
			if (original == null) {
				return false;
			}

			var ctx = new ContextMap.Builder()
				.set(SlotDisplayContext.REGISTRIES, type.event.registries.registryAccess())
				.buildAndValidate(SlotDisplayContext.CONTEXT);

			var displays = original.display();
			if (displays.isEmpty()) {
				return false;
			}

			for (var d : displays) {
				for (var stack : d.result().resolveForStacks(ctx)) {
					if (!stack.isEmpty() && m.matches(cx, stack, match.exact())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean replaceOutput(RecipeScriptContext cx, ReplacementMatchInfo match, Object with) {
		return false;
	}
}