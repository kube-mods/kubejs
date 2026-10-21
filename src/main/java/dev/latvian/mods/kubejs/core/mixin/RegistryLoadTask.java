package dev.latvian.mods.kubejs.core.mixin;

import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RegistryLoadTask.class)
public abstract class RegistryLoadTaskMixin<T> {
	@Shadow
	@Final
	WritableRegistry<T> registry;

	/// Binds recipes removed by scripts that other data still references, see [dev.latvian.mods.kubejs.recipe.RemovedRecipe]
	@Inject(method = "freezeRegistry", at = @At("HEAD"))
	private void kjs$bindRemovedRecipes(Map<ResourceKey<?>, Exception> loadingErrors, CallbackInfoReturnable<Boolean> cir) {
		if (registry.key().equals(Registries.RECIPE)) {
			RecipeResourceLoader.bindRemovedRecipes(registry);
		}
	}
}
