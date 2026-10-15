package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;

@RemapPrefixForJS("kjs$")
public interface RecipeLikeKJS {
	String kjs$getGroup();

	void kjs$setGroup(String group);

	Identifier kjs$getOrCreateId();

	RecipeSchema kjs$getSchema(Context cx);

	default String kjs$getMod() {
		return kjs$getOrCreateId().getNamespace();
	}

	ResourceKey<RecipeSerializer<?>> kjs$getTypeKey();

	default Identifier kjs$getType() {
		return kjs$getTypeKey().identifier();
	}

	RecipeSerializer<?> kjs$getSerializer();

	default boolean hasInput(RecipeMatchContext cx, ReplacementMatchInfo match) {
		return false;
	}

	default boolean replaceInput(RecipeScriptContext cx, ReplacementMatchInfo match, Object with) {
		return false;
	}

	default boolean hasOutput(RecipeMatchContext cx, ReplacementMatchInfo match) {
		return false;
	}

	default boolean replaceOutput(RecipeScriptContext cx, ReplacementMatchInfo match, Object with) {
		return false;
	}
}
