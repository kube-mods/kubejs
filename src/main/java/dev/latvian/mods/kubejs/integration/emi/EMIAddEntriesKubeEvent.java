package dev.latvian.mods.kubejs.integration.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.latvian.mods.kubejs.recipe.viewer.AddEntriesKubeEvent;
import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.rhino.Context;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class EMIAddEntriesKubeEvent implements AddEntriesKubeEvent {
	private final RecipeViewerEntryType type;
	private final EmiRegistry registry;

	public EMIAddEntriesKubeEvent(RecipeViewerEntryType type, EmiRegistry registry) {
		this.type = type;
		this.registry = registry;
	}

	@Override
	public void add(Context cx, Object[] items) {
		for (var item : items) {
			var entry = type.wrapEntry(cx, item);

			if (type == RecipeViewerEntryType.ITEM) {
				registry.addEmiStack(EmiStack.of((ItemStack) entry));
			} else if (type == RecipeViewerEntryType.FLUID) {
				registry.addEmiStack(EMIIntegration.fluid((FluidStack) entry));
			}
		}
	}
}
