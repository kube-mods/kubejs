package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.client.KubeSessionData;
import dev.latvian.mods.kubejs.client.NotificationToast;
import dev.latvian.mods.kubejs.net.SendDataFromClientPayload;
import dev.latvian.mods.kubejs.player.PlayerStatsJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.typings.ThisIs;
import dev.latvian.mods.kubejs.util.NotificationToastData;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

@RemapPrefixForJS("kjs$")
public interface LocalClientPlayerKJS extends ClientPlayerKJS {
	@Override
	@HideFromJS
	default LocalPlayer kjs$self() {
		return (LocalPlayer) this;
	}

	default Minecraft kjs$getMinecraft() {
		return Minecraft.getInstance();
	}

	@Override
	@Info(value = "Runs the specified console command client-side with the player's permission level.", params = {
		@Param(name = "command", value = "The console command. Slash at the beginning is optional."),
	})
	default void kjs$runCommand(String command) {
		kjs$self().connection.sendCommand(command);
	}

	@Override
	@Info(value = "Runs the specified console command client-side with the player's permission level. The command won't output any logs in chat nor console.", params = {
		@Param(name = "command", value = "The console command. Slash at the beginning is optional."),
	})
	default void kjs$runCommandSilent(String command) {
		kjs$self().connection.sendCommand(command);
	}

	@Override
	@ThisIs(classNames = "net.minecraft.client.player.LocalPlayer")
	@Info("Checks, whether the entity is a reference to yourself - that is - the client player you are controlling.")
	default boolean kjs$isSelf() {
		return true;
	}

	@Override
	default void kjs$sendData(String channel, @Nullable CompoundTag data) {
		if (!channel.isEmpty()) {
			ClientPacketDistributor.sendToServer(new SendDataFromClientPayload(channel, data));
		}
	}

	@Override
	default PlayerStatsJS kjs$getStats() {
		return new PlayerStatsJS(kjs$self(), kjs$self().getStats());
	}

	@Override
	@Info("Checks, whether the player is currently mining a block.")
	default boolean kjs$isMiningBlock() {
		return Minecraft.getInstance().gameMode.isDestroying();
	}

	@Override
	default void kjs$notify(NotificationToastData notification) {
		var mc = Minecraft.getInstance();
		mc.gui.toastManager().addToast(new NotificationToast(mc, notification));
	}

	@Override
	default void kjs$setActivePostShader(@Nullable Identifier id) {
		var sessionData = KubeSessionData.of(kjs$self().connection);

		if (sessionData != null) {
			sessionData.activePostShader = id;
			var mc = kjs$getMinecraft();
			mc.gameRenderer.checkEntityPostEffect(mc.options.getCameraType().isFirstPerson() ? mc.getCameraEntity() : null);
		}
	}
}
