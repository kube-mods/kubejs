package dev.latvian.mods.kubejs.integration.emi;
/*

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.latvian.mods.kubejs.client.KubeSessionData;
import dev.latvian.mods.kubejs.plugin.builtin.event.RecipeViewerEvents;
import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EmiEntrypoint
public class KubeJSEMIPlugin implements EmiPlugin {
	@Override
	public void register(EmiRegistry registry) {
		var sessionData = KubeSessionData.of(Minecraft.getInstance());
		var remote = sessionData == null ? null : sessionData.recipeViewerData;

		if (RecipeViewerEvents.REMOVE_CATEGORIES.hasListeners()) {
			var removedCategories = new HashSet<ResourceLocation>();
			RecipeViewerEvents.REMOVE_CATEGORIES.post(ScriptType.CLIENT, new EMIRemoveCategoriesKubeEvent(removedCategories));

			if (!removedCategories.isEmpty()) {
				registry.removeRecipes(r -> r.getCategory() != null && removedCategories.contains(r.getCategory().getId()));
			}
		}

		if (remote != null) {
			var removedCategories = Set.copyOf(remote.removedCategories());
			var globalRemovedRecipes = Set.copyOf(remote.removedGlobalRecipes());
			var removedRecipes = new HashMap<Identifier, Set<Identifier>>();

			for (var data : remote.categoryData()) {
				removedRecipes.put(data.category(), Set.copyOf(data.removedRecipes()));
			}

			registry.removeRecipes(r -> {
				Object cat = r.getCategory().getId();

				if (cat == null) {
					return false;
				}

				if (removedCategories.contains(cat)) {
					return true;
				}

				Object id = r.getId();

				if (id == null) {
					return false;
				}

				return globalRemovedRecipes.contains(id) || removedRecipes.getOrDefault(cat, Set.of()).contains(id);
			});
		}

		for (var type : RecipeViewerEntryType.ALL_TYPES.get()) {
			if (RecipeViewerEvents.REMOVE_ENTRIES.hasListeners(type)) {
				RecipeViewerEvents.REMOVE_ENTRIES.post(ScriptType.CLIENT, type, new EMIRemoveEntriesKubeEvent(type, registry));
			}

			if (RecipeViewerEvents.REMOVE_ENTRIES_COMPLETELY.hasListeners(type)) {
				RecipeViewerEvents.REMOVE_ENTRIES_COMPLETELY.post(ScriptType.CLIENT, type, new EMIRemoveEntriesKubeEvent(type, registry));
			}
		}

		if (remote != null) {
			for (var ingredient : remote.itemData().removedEntries()) {
				registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
			}

			for (var ingredient : remote.itemData().completelyRemovedEntries()) {
				registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
			}

			for (var ingredient : remote.fluidData().removedEntries()) {
				registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
			}

			for (var ingredient : remote.fluidData().completelyRemovedEntries()) {
				registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
			}
		}

		for (var type : RecipeViewerEntryType.ALL_TYPES.get()) {
			if (RecipeViewerEvents.ADD_ENTRIES.hasListeners(type)) {
				RecipeViewerEvents.ADD_ENTRIES.post(ScriptType.CLIENT, type, new EMIAddEntriesKubeEvent(type, registry));
			}
		}

		if (remote != null) {
			for (var stack : remote.itemData().addedEntries()) {
				registry.addEmiStack(EmiStack.of(stack));
			}

			for (var stack : remote.fluidData().addedEntries()) {
				registry.addEmiStack(EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount()));
			}
		}

		for (var type : RecipeViewerEntryType.ALL_TYPES.get()) {
			if (RecipeViewerEvents.ADD_INFORMATION.hasListeners(type)) {
				RecipeViewerEvents.ADD_INFORMATION.post(ScriptType.CLIENT, type, new EMIAddInformationKubeEvent(type, registry));
			}
		}

		if (remote != null) {
			for (var info : remote.itemData().info()) {
				registry.addRecipe(new EmiInfoRecipe(List.of(EmiIngredient.of(info.filter())), info.info(), null));
			}

			for (var info : remote.fluidData().info()) {
				registry.addRecipe(new EmiInfoRecipe(List.of(EMIIntegration.fluidIngredient(info.filter())), info.info(), null));
			}
		}
	}
}
*/
