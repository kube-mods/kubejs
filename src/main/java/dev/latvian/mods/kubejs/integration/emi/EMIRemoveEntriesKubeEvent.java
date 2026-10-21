package dev.latvian.mods.kubejs.integration.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.latvian.mods.kubejs.item.ItemPredicate;
import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.kubejs.recipe.viewer.RemoveEntriesKubeEvent;
import dev.latvian.mods.rhino.Context;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class EMIRemoveEntriesKubeEvent implements RemoveEntriesKubeEvent {
	private final RecipeViewerEntryType type;
	private final EmiRegistry registry;

	public EMIRemoveEntriesKubeEvent(RecipeViewerEntryType type, EmiRegistry registry) {
		this.type = type;
		this.registry = registry;
	}

	@Override
	public void remove(Context cx, Object filter) {
		var predicate = type.wrapPredicate(cx, filter);

		if (type == RecipeViewerEntryType.ITEM) {
			registry.removeEmiStacks(EMIIntegration.predicate((ItemPredicate) predicate));
		} else if (type == RecipeViewerEntryType.FLUID) {
			registry.removeEmiStacks(EMIIntegration.predicate((FluidIngredient) predicate));
		}
	}
}
