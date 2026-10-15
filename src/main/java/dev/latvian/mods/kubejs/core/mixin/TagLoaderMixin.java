package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.latvian.mods.kubejs.core.ScriptManagerHolderKJS;
import dev.latvian.mods.kubejs.core.TagLoaderKJS;
import dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagLoader.EntryWithSource;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin<T> implements TagLoaderKJS<T> {
	@Unique
	private @Nullable ServerScriptManager kjs$serverScriptManager;

	@Unique
	private @Nullable Registry<T> kjs$storedRegistry;

	@Override
	public void kjs$init(@Nullable ServerScriptManager serverScriptManager, Registry<T> registry) {
		kjs$serverScriptManager = serverScriptManager;
		kjs$storedRegistry = registry;
	}

	@Inject(
		method = "loadPendingTags(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/Registry;)Ljava/util/Optional;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/tags/TagLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;",
			shift = At.Shift.BEFORE
		)
	)
	private static <T> void kjs$initLoaderBeforeLoad(
		ResourceManager manager,
		Registry<T> registry,
		CallbackInfoReturnable<?> cir,
		@Local(name = "loader") TagLoader<T> loader
	) {
		loader.kjs$init(((ScriptManagerHolderKJS) manager).kjs$getScriptManager(), registry);
	}

	@Inject(method = "load", at = @At("RETURN"))
	private void kjs$modifyLoadedTags(
		ResourceManager resourceManager,
		CallbackInfoReturnable<Map<Identifier, List<EntryWithSource>>> cir
	) {
		var ssm = kjs$getServerScriptManager();

		if (ssm != null && kjs$storedRegistry != null) {
			Map<Identifier, List<EntryWithSource>> map = cir.getReturnValue();
			var reg = kjs$getRegistry();

			if (reg == null) {
				return;
			}

			boolean needsCustomTags = false;
			var objStorage = RegistryObjectStorage.of(reg.key());

			for (var builder : objStorage.objects.values()) {
				if (!builder.defaultTags.isEmpty()) {
					needsCustomTags = true;
					break;
				}
			}

			needsCustomTags |= !ssm.serverRegistryTags.isEmpty() || ServerEvents.TAGS.hasListeners(objStorage.key);

			if (needsCustomTags) {
				kjs$customTags(map);
			}

			ssm.getRegistries().cacheTags(reg, map);
		}
	}

	@Override
	@Nullable
	public ServerScriptManager kjs$getServerScriptManager() {
		return kjs$serverScriptManager;
	}

	@Override
	public @Nullable Registry<T> kjs$getRegistry() {
		return kjs$storedRegistry;
	}
}
