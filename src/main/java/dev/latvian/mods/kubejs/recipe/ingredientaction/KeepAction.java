package dev.latvian.mods.kubejs.recipe.ingredientaction;

import com.mojang.serialization.MapCodec;
import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

public class KeepAction implements IngredientAction {
	public static final KeepAction INSTANCE = new KeepAction();
	public static final IngredientActionType<KeepAction> TYPE = new IngredientActionType<>(KubeJS.id("keep"), MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	@Override
	public IngredientActionType<?> getType() {
		return TYPE;
	}

	@Override
	public ItemStack transform(ItemStack old, int index, CraftingInput input) {
		return old.copyWithCount(1);
	}
}
