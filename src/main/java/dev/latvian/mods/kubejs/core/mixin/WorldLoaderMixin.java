package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Mixin(WorldLoader.class)
@SuppressWarnings("rawtypes")
public class WorldLoaderMixin {
	@Inject(
		method = "lambda$load$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;",
			shift = At.Shift.BEFORE
		)
	)
	private static void kjs$load(Executor backgroundExecutor, WorldLoader.WorldDataSupplier worldDataSupplier, WorldLoader.InitConfig config, Executor mainThreadExecutor, WorldLoader.ResultFactory resultFactory, Pair packsAndResourceManager, CallbackInfoReturnable<CompletionStage> cir, @Local(name = "worldLoadContext") RegistryAccess.Frozen worldLoadContext) {
		RegistryAccessContainer.current = new RegistryAccessContainer(worldLoadContext);
	}

	@Inject(
		method = "lambda$load$2",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/datafixers/util/Pair;getFirst()Ljava/lang/Object;",
			shift = At.Shift.BEFORE
		)
	)
	private static void kjs$load2(Pair packsAndResourceManager, List dimensionContextRegistries, WorldLoader.WorldDataSupplier worldDataSupplier, CloseableResourceManager resources, LayeredRegistryAccess initialLayers, RegistryAccess.Frozen loadedWorldRegistries, List staticLayerTags, WorldLoader.InitConfig config, Executor backgroundExecutor, Executor mainThreadExecutor, WorldLoader.ResultFactory resultFactory, RegistryAccess.Frozen initialWorldgenDimensions, CallbackInfoReturnable<CompletionStage> cir) {
		RegistryAccessContainer.current = new RegistryAccessContainer(initialWorldgenDimensions);
	}
}
