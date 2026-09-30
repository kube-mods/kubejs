package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(WorldLoader.PackConfig.class)
public abstract class WorldLoaderPackConfigMixin {
	@WrapOperation(method = "createResourceManager", at = @At(
		value = "NEW",
		target = "(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)Lnet/minecraft/server/packs/resources/MultiPackResourceManager;")
	)
	private MultiPackResourceManager kjs$createResourceManager(PackType type, List<PackResources> packs, Operation<MultiPackResourceManager> ctor) {
		return ServerScriptManager.bindServerResources(packs, true, ctor::call);
	}
}
