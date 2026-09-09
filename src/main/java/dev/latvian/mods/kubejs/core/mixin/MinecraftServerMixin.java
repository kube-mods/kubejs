package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.core.MinecraftServerKJS;
import dev.latvian.mods.kubejs.gui.chest.CustomChestMenu;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ScheduledServerEvent;
import dev.latvian.mods.kubejs.server.ServerKubeEvent;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.util.AttachedData;
import dev.latvian.mods.kubejs.util.ScheduledEvents;
import dev.latvian.mods.rhino.util.RemapForJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
@RemapPrefixForJS("kjs$")
public abstract class MinecraftServerMixin implements MinecraftServerKJS {
	@Shadow
	protected abstract boolean initServer() throws IOException;

	@Shadow
	public abstract void invalidateStatus();

	@Unique
	private final CompoundTag kjs$persistentData = new CompoundTag();

	@Unique
	private ServerLevel kjs$overworld;

	@Unique
	private AttachedData<MinecraftServer> kjs$attachedData;

	@Unique
	private final Map<UUID, Map<Integer, ItemStack>> kjs$restoreInventories = new HashMap<>(1);

	@Inject(method = "<init>", at = @At("RETURN"))
	private void kjs$init(CallbackInfo ci) {
		CompletableFuture.runAsync(() -> kjs$afterResourcesLoaded(false), kjs$self());
	}

	@Shadow
	public abstract RegistryAccess.Frozen registryAccess();

	@Override
	public CompoundTag kjs$getPersistentData() {
		return kjs$persistentData;
	}

	@Override
	public AttachedData<MinecraftServer> kjs$getData() {
		if (kjs$attachedData == null) {
			kjs$attachedData = new AttachedData<>(kjs$self());
			KubeJSPlugins.forEachPlugin(kjs$attachedData, KubeJSPlugin::attachServerData);
		}

		return kjs$attachedData;
	}

	@Override
	public ServerLevel kjs$getOverworld() {
		if (kjs$overworld == null) {
			kjs$overworld = kjs$self().overworld();
		}

		return kjs$overworld;
	}

	@Inject(method = "tickServer", at = @At("RETURN"))
	private void kjs$postTickServer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
		ScheduledServerEvent.EVENTS.tickAll(kjs$getOverworld().getGameTime());

		if (!kjs$restoreInventories.isEmpty()) {
			for (var player : kjs$self().getPlayerList().getPlayers()) {
				if (kjs$restoreInventories.containsKey(player.getUUID()) && player.isAlive() && !player.hasDisconnected() && !(player.containerMenu instanceof CustomChestMenu)) {
					player.kjs$restoreCapturedInventory();
				}
			}
		}

		if (ServerEvents.TICK.hasListeners()) {
			ServerEvents.TICK.post(ScriptType.SERVER, new ServerKubeEvent(kjs$self()));
		}
	}

	@Override
	public ScheduledEvents kjs$getScheduledEvents() {
		return ScheduledServerEvent.EVENTS;
	}

	@Override
	public Map<UUID, Map<Integer, ItemStack>> kjs$restoreInventories() {
		return kjs$restoreInventories;
	}

	@Shadow
	@RemapForJS("isDedicated")
	public abstract boolean isDedicatedServer();

	@Shadow
	@RemapForJS("stop")
	public abstract void stopServer();

	@Inject(method = "reloadResources", at = @At("TAIL"))
	private void kjs$endResourceReload(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		CompletableFuture.runAsync(() -> kjs$afterResourcesLoaded(true), kjs$self());
	}

	// There's a good chance this will break in the future, but currently that's the only reliable way to both inject packs and reload scripts before resources that I could find
	@Redirect(method = "lambda$reloadResources$29", at = @At(value = "NEW", target = "(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)Lnet/minecraft/server/packs/resources/MultiPackResourceManager;"))
	private MultiPackResourceManager kjs$modifyResourceReload(PackType type, List<PackResources> original) {
		return new MultiPackResourceManager(type, ServerScriptManager.createPackResources(original));
	}
}
