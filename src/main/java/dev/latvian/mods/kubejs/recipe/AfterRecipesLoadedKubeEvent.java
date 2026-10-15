package dev.latvian.mods.kubejs.recipe;

import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.core.RecipeLikeKJS;
import dev.latvian.mods.kubejs.core.RecipeManagerKJS;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.recipe.filter.ConstantFilter;
import dev.latvian.mods.kubejs.recipe.filter.RecipeFilter;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.kubejs.util.RegistryOpsContainer;
import dev.latvian.mods.rhino.Context;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AfterRecipesLoadedKubeEvent implements KubeEvent {
	public record MatchCx(
		Context cx,
		RegistryAccessContainer registries,
		RegistryOpsContainer ops,
		RecipeLikeKJS recipe
	) implements RecipeMatchContext {
		public MatchCx(Context cx, AfterRecipesLoadedKubeEvent event, RecipeLikeKJS recipe) {
			this(cx, event.registries, event.registries, recipe);
		}
	}

	private final RecipeManagerKJS recipeManager;
	private final RegistryAccessContainer registries;
	private @Nullable List<RecipeLikeKJS> originalRecipes;
	private boolean changed;

	public AfterRecipesLoadedKubeEvent(ReloadableServerResources resources) {
		this.recipeManager = resources.getRecipeManager();
		this.registries = resources.kjs$getServerScriptManager().getRegistries();
		this.originalRecipes = null;
		this.changed = false;
	}

	private List<RecipeLikeKJS> getOriginalRecipes() {
		if (originalRecipes == null) {
			originalRecipes = new ArrayList<>(recipeManager.kjs$getRecipes().stream()
				.map(r -> (RecipeLikeKJS) r)
				.toList());
		}
		return originalRecipes;
	}

	public void forEachRecipe(Context cx, RecipeFilter filter, Consumer<RecipeLikeKJS> consumer) {
		if (filter == ConstantFilter.TRUE) {
			getOriginalRecipes().forEach(consumer);
		} else if (filter != ConstantFilter.FALSE) {
			getOriginalRecipes().stream().filter(r -> filter.test(new MatchCx(cx, this, r))).forEach(consumer);
		}
	}

	public int countRecipes(Context cx, RecipeFilter filter) {
		if (filter == ConstantFilter.TRUE) {
			return getOriginalRecipes().size();
		} else if (filter != ConstantFilter.FALSE) {
			return (int) getOriginalRecipes().stream().filter(r -> filter.test(new MatchCx(cx, this, r))).count();
		}

		return 0;
	}

	public int remove(Context cx, RecipeFilter filter) {
		int count = 0;
		var itr = getOriginalRecipes().iterator();

		while (itr.hasNext()) {
			var r = itr.next();

			if (filter.test(new MatchCx(cx, this, r))) {
				itr.remove();
				count++;
				changed = true;

				if (DevProperties.get().logRemovedRecipes) {
					ConsoleJS.SERVER.info("- " + r);
				} else if (ConsoleJS.SERVER.shouldPrintDebug()) {
					ConsoleJS.SERVER.debug("- " + r);
				}
			}
		}

		return count;
	}

	@Override
	public void afterPosted(EventResult result) {
		if (changed) {
			var holders = getOriginalRecipes().stream()
				.<RecipeHolder<?>>map(r -> (RecipeHolder<?>) r)
				.toList();
			recipeManager.kjs$replaceRecipes(RecipeMap.createClient(holders));
		}
	}
}