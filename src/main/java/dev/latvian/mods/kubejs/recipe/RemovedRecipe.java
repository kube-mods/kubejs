package dev.latvian.mods.kubejs.recipe;

import com.mojang.serialization.MapCodec;
import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/// Inert recipe bound to the id of a recipe that a script removed while other data still refers to it.
///
/// Since 26.3 recipes are a reloadable registry and advancements (`recipe_unlocked`, `recipe_crafted`) hold registry
/// references to them, so a removed recipe that is still referenced would be an unbound holder and fail the reload.
/// It has its own recipe type, never matches and is hidden from the recipe book.
///
/// @param id the removed recipe's id (each registry entry needs its own value instance)
public record RemovedRecipe(Identifier id) implements Recipe<RecipeInput> {
	public static final RecipeType<RemovedRecipe> TYPE = RecipeType.simple(KubeJS.id("removed"));
	public static final MapCodec<RemovedRecipe> CODEC = Identifier.CODEC.fieldOf("removed").xmap(RemovedRecipe::new, RemovedRecipe::id);
	public static final RecipeSerializer<RemovedRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, Identifier.STREAM_CODEC.map(RemovedRecipe::new, RemovedRecipe::id).cast());

	@Override
	public boolean matches(RecipeInput input, Level level) {
		return false;
	}

	@Override
	public ItemStack assemble(RecipeInput input) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<RemovedRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<RemovedRecipe> getType() {
		return TYPE;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}
}
