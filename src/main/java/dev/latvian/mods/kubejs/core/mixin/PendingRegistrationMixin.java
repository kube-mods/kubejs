package dev.latvian.mods.kubejs.core.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Decoder;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.Reader;

@Mixin(RegistryLoadTask.PendingRegistration.class)
public abstract class PendingRegistrationMixin {
	@Unique
	private static final ThreadLocal<@Nullable JsonElement> KJS$JSON = new ThreadLocal<>();

	@Inject(method = "loadFromResource", at = @At("HEAD"))
	private static <T> void kjs$clear(Decoder<T> elementDecoder, RegistryOps<JsonElement> ops, ResourceKey<T> elementKey, Resource thunk, CallbackInfoReturnable<Either<T, Exception>> cir) {
		KJS$JSON.remove();
	}

	@WrapOperation(method = "loadFromResource", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StrictJsonParser;parse(Ljava/io/Reader;)Lcom/google/gson/JsonElement;"))
	private static JsonElement kjs$capture(Reader reader, Operation<JsonElement> original) {
		var called = original.call(reader);
		KJS$JSON.set(called);
		return called;
	}

	@Inject(method = "loadFromResource", at = @At("RETURN"))
	private static <T> void kjs$report(Decoder<T> elementDecoder, RegistryOps<JsonElement> ops, ResourceKey<T> elementKey, Resource thunk, CallbackInfoReturnable<Either<T, Exception>> cir) {
		var json = KJS$JSON.get();
		if (json == null || !elementKey.isFor(Registries.RECIPE) || !RecipesKubeEvent.INSTANCE.isBound()) {
			return;
		}

		cir.getReturnValue().right()
			.filter(IllegalStateException.class::isInstance)
			.ifPresent(ex -> RecipesKubeEvent.INSTANCE.get().handleFailedRecipe(elementKey.identifier(), json, ex));
	}
}
