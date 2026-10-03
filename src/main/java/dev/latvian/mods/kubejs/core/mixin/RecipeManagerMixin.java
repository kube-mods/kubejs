package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.core.RecipeManagerKJS;
import dev.latvian.mods.kubejs.core.ReloadableServerResourcesKJS;
import dev.latvian.mods.kubejs.net.KubeServerData;
import dev.latvian.mods.kubejs.net.SyncServerDataPayload;
import dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents;
import dev.latvian.mods.kubejs.recipe.special.SpecialRecipeSerializerManager;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.util.Cast;
import net.minecraft.core.MappedRegistry;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Objects;

@Mixin(value = RecipeManager.class, priority = 1100)
public abstract class RecipeManagerMixin implements RecipeManagerKJS {
	@Unique
	private RecipeManager kjs$self() {
		return (RecipeManager) (Object) this;
	}

	@Final
	@Shadow
	@Mutable
	private RecipeMap recipes;

	@Final
	@Shadow
	@Mutable
	private Collection<RecipeHolder<?>> learnableRecipes;

	@Unique
	private @Nullable ReloadableServerResourcesKJS kjs$resources;

	@Unique
	public void kjs$preRecipeLoad(net.minecraft.server.packs.resources.ResourceManager manager) {
		if (kjs$resources == null) {
			return;
		}

		var ssm = Objects.requireNonNull(kjs$resources.kjs$getServerScriptManager());

		// TODO: i would like to be able to live without these two calls
		for (var pending : kjs$resources.kjs$getPostponedTags()) {
			pending.apply();
		}

		for (var pending : kjs$resources.kjs$getNewComponents()) {
			pending.apply();
		}

		for (var entry : ssm.getRegistries().cachedRegistryTags.values()) {
			if (entry.registry() instanceof MappedRegistry<?> mappedRegistry) {
				mappedRegistry.bindTags(Cast.to(entry.lookup().bindingMap()));
			}
		}

		ssm.recipeSchemaStorage.fireEvents(ssm.getRegistries(), manager);

		SpecialRecipeSerializerManager.INSTANCE.reset();
		ServerEvents.SPECIAL_RECIPES.post(ScriptType.SERVER, SpecialRecipeSerializerManager.INSTANCE);
	}

	@Inject(method = "finalizeRecipeLoading", at = @At("TAIL"))
	private void kjs$finalizeTail(FeatureFlagSet enabledFlags, CallbackInfo ci) {
		if (!CommonProperties.get().serverOnly) {
			var ssm = kjs$getServerScriptManager();

			if (ssm != null) {
				ssm.serverData = new SyncServerDataPayload(KubeServerData.collect());
			}
		}
	}

	@Override
	@NullUnmarked
	public ServerScriptManager kjs$getServerScriptManager() {
		return kjs$resources != null ? kjs$resources.kjs$getServerScriptManager() : null;
	}

	@Override
	public void kjs$setResources(ReloadableServerResourcesKJS resources) {
		kjs$resources = resources;
	}

	@Override
	public void kjs$replaceRecipes(RecipeMap recipeMap) {
		recipes = recipeMap;
		// learnableRecipes is made once in the constructor, so rebuild it to match the new map
		learnableRecipes = recipeMap.values().stream()
			.filter(r -> !r.value().isSpecial())
			.toList();
		ConsoleJS.SERVER.info("Loaded " + recipeMap.values().size() + " recipes");
	}

	@Override
	public Collection<RecipeHolder<?>> kjs$getRecipes() {
		return recipes.values();
	}
}