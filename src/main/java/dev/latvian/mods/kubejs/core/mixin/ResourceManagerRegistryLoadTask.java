package dev.latvian.mods.kubejs.core.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import dev.latvian.mods.kubejs.core.ScriptManagerHolderKJS;
import dev.latvian.mods.kubejs.recipe.RecipeResourceLoader;
import dev.latvian.mods.kubejs.util.Cast;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/// 26.3 loads recipes as a reloadable registry. Before the listed recipe files are parsed in parallel, post the recipe
/// event over their JSON and parse the result instead (replaces 26.1's `ModifyRecipeJsonsEvent` hook in `RecipeManager#prepare`).
@Mixin(ResourceManagerRegistryLoadTask.class)
public abstract class ResourceManagerRegistryLoadTaskMixin<T> extends RegistryLoadTask<T> {
	@Shadow
	@Final
	private ResourceManager resourceManager;

	@Shadow
	@Final
	private List<Registry.PendingTags<?>> pendingTags;

	private ResourceManagerRegistryLoadTaskMixin(RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> loadingErrors) {
		super(data, lifecycle, loadingErrors);
	}

	@WrapOperation(
		method = "lambda$load$1",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/thread/ParallelMapTransform;schedule(Ljava/util/Map;Ljava/util/function/BiFunction;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
		)
	)
	private CompletableFuture<?> kjs$postRecipeEvent(
		Map<?, ?> registryResources,
		BiFunction<?, ?, ?> parser,
		Executor executor,
		Operation<CompletableFuture<?>> original,
		@Local(argsOnly = true) FileToIdConverter lister,
		@Local RegistryOps<JsonElement> ops
	) {
		if (!registryKey().equals(Registries.RECIPE) || !(resourceManager instanceof ScriptManagerHolderKJS holder) || holder.kjs$getScriptManager() == null) {
			return original.call(registryResources, parser, executor);
		}

		var modified = RecipeResourceLoader.postRecipeEvent(holder.kjs$getScriptManager(), resourceManager, pendingTags, lister, ops, Cast.to(registryResources));
		return original.call(modified, parser, executor).whenComplete((result, error) -> RecipeResourceLoader.finishParsing());
	}
}
