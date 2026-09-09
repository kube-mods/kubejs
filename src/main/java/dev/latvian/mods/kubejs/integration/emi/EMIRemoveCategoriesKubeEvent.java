package dev.latvian.mods.kubejs.integration.emi;

import dev.latvian.mods.kubejs.recipe.viewer.RemoveCategoriesKubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.resources.Identifier;

import java.util.Set;

public class EMIRemoveCategoriesKubeEvent implements RemoveCategoriesKubeEvent {
	private final Set<Identifier> categoriesRemoved;

	public EMIRemoveCategoriesKubeEvent(Set<Identifier> categoriesRemoved) {
		this.categoriesRemoved = categoriesRemoved;
	}

	@Override
	public void remove(Context cx, Identifier[] categories) {
		categoriesRemoved.addAll(Set.of(categories));
	}
}
