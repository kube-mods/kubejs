package dev.latvian.mods.kubejs.integration.emi;

import dev.latvian.mods.kubejs.recipe.viewer.RemoveCategoriesKubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class EMIRemoveCategoriesKubeEvent implements RemoveCategoriesKubeEvent {
	private final Set<ResourceLocation> categoriesRemoved;

	public EMIRemoveCategoriesKubeEvent(Set<ResourceLocation> categoriesRemoved) {
		this.categoriesRemoved = categoriesRemoved;
	}

	@Override
	public void remove(Context cx, ResourceLocation[] categories) {
		categoriesRemoved.addAll(Set.of(categories));
	}
}
