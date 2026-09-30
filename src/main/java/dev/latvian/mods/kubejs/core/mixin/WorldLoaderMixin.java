package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletionStage;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {
	@Inject(
		method = "lambda$load$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;",
			shift = At.Shift.BEFORE
		)
	)
	private static void kjs$load(
		CallbackInfoReturnable<CompletionStage<?>> cir,
		//TODO @Local does not match any or matched multiple local variables in the target method
		@Local(name = "worldgenLoadContext") RegistryAccess.Frozen worldgenLoadContext
	) {
		RegistryAccessContainer.current = new RegistryAccessContainer(worldgenLoadContext);
	}

	@Inject(
		method = "lambda$load$2",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/datafixers/util/Pair;getFirst()Ljava/lang/Object;",
			shift = At.Shift.BEFORE
		)
	)
	private static void kjs$load2(
		CallbackInfoReturnable<CompletionStage<?>> cir,
		@Local(argsOnly = true, name = "initialWorldgenDimensions") RegistryAccess.Frozen initialWorldgenDimensions
	) {
		RegistryAccessContainer.current = new RegistryAccessContainer(initialWorldgenDimensions);
	}
}
