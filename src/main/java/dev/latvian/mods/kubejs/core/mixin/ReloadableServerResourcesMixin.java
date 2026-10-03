package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.latvian.mods.kubejs.core.ReloadableServerResourcesKJS;
import dev.latvian.mods.kubejs.core.ScriptManagerHolderKJS;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin implements ReloadableServerResourcesKJS {
	@Unique
	@Nullable
	private ServerScriptManager kjs$serverScriptManager;

	@Shadow
	@Final
	private RecipeManager recipes;

	@Shadow
	@Final
	private List<Registry.PendingTags<?>> postponedTags;
	@Shadow
	@Final
	private List<DataComponentInitializers.PendingComponents<?>> newComponents;

	@Override
	public List<DataComponentInitializers.PendingComponents<?>> kjs$getNewComponents() {
		return newComponents;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void kjs$init(
		ReloadableServerRegistries.LoadResult loadingContext, FeatureFlagSet enabledFeatures, Commands.CommandSelection commandSelection, List<?> postponedTags, PermissionSet functionCompilationPermissions, List<?> newComponents, CallbackInfo ci
	) {
		recipes.kjs$setResources(this);
	}

	@Inject(method = "loadResources", at = @At("HEAD"))
	private static void kjs$injectKubeJSPacks(
		ResourceManager resourceManager,
		LayeredRegistryAccess<RegistryLayer> contextLayers,
		List<Registry.PendingTags<?>> updatedContextTags,
		FeatureFlagSet enabledFeatures,
		Commands.CommandSelection commandSelection,
		PermissionSet functionCompilationPermissions,
		Executor backgroundExecutor,
		Executor mainThreadExecutor,
		CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir
	) {
		RegistryAccessContainer.current = new RegistryAccessContainer(contextLayers.compositeAccess());
	}

	@Inject(
		method = "lambda$loadResources$2",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/event/EventHooks;onResourceReload(Lnet/minecraft/server/ReloadableServerResources;Ljava/util/Map;)Ljava/util/List;",
			shift = At.Shift.BEFORE
		)
	)
	private static void kjs$bindServerScriptManager(
		ReloadableServerRegistries.LoadResult fullRegistries,
		FeatureFlagSet enabledFeatures,
		Commands.CommandSelection commandSelection,
		List<?> updatedContextTags,
		PermissionSet functionCompilationPermissions,
		ResourceManager resourceManager,
		Executor backgroundExecutor,
		Executor mainThreadExecutor,
		List<?> pendingComponents,
		CallbackInfoReturnable<CompletionStage<?>> cir,
		@Local(name = "result") ReloadableServerResources result
	) {
		var ssm = ((ScriptManagerHolderKJS) resourceManager).kjs$getScriptManager();
		result.kjs$setServerScriptManager(Objects.requireNonNull(ssm, "Missing ServerScriptManager for resource reload!"));
	}

	@Override
	@Nullable
	public ServerScriptManager kjs$getServerScriptManager() {
		return kjs$serverScriptManager;
	}

	@Override
	public void kjs$setServerScriptManager(ServerScriptManager serverScriptManager) {
		kjs$serverScriptManager = serverScriptManager;
	}

	@Override
	public List<Registry.PendingTags<?>> kjs$getPostponedTags() {
		return postponedTags;
	}
}
