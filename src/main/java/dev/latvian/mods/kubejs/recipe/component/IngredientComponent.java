package dev.latvian.mods.kubejs.recipe.component;

import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.util.OpsContainer;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.world.item.crafting.Ingredient;

public record IngredientComponent(RecipeComponentType<?> type, Codec<Ingredient> codec, boolean allowEmpty) implements RecipeComponent<Ingredient> {
	public static final RecipeComponentType<Ingredient> INGREDIENT = RecipeComponentType.unit(KubeJS.id("ingredient"), type -> new IngredientComponent(type, Ingredient.CODEC_NONEMPTY, false));
	public static final RecipeComponentType<Ingredient> OPTIONAL_INGREDIENT = RecipeComponentType.unit(KubeJS.id("optional_ingredient"), type -> new IngredientComponent(type, Ingredient.CODEC, true));

	@Override
	public TypeInfo typeInfo() {
		return IngredientWrapper.TYPE_INFO;
	}

	@Override
	public boolean hasPriority(RecipeMatchContext cx, Object from) {
		return IngredientWrapper.isIngredientLike(from);
	}

	@Override
	public boolean matches(RecipeMatchContext cx, Ingredient value, ReplacementMatchInfo match) {
		return match.match() instanceof ItemMatch m && !value.isEmpty() && m.matches(cx, value, match.exact());
	}

	@Override
	public boolean isEmpty(Ingredient value) {
		return value.isEmpty() || value.hasNoItems();
	}

	@Override
	public void buildUniqueId(UniqueIdBuilder builder, Ingredient value) {
		var tag = IngredientWrapper.tagKeyOf(value);

		if (tag != null) {
			builder.append(tag.location());
		} else {
			var first = value.kjs$getFirst();

			if (!first.isEmpty()) {
				builder.append(first.kjs$getIdLocation());
			}
		}
	}

	@Override
	public String toString() {
		return type.toString();
	}

	@Override
	public String toString(OpsContainer ops, Ingredient value) {
		return value.kjs$toIngredientString(ops.nbt());
	}
}
