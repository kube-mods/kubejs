package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.gui.KubeJSGUI;
import dev.latvian.mods.kubejs.gui.KubeJSMenu;
import dev.latvian.mods.kubejs.gui.chest.ChestMenuData;
import dev.latvian.mods.kubejs.gui.chest.CustomChestMenu;
import dev.latvian.mods.kubejs.level.LevelBlock;
import dev.latvian.mods.kubejs.net.KubeJSNet;
import dev.latvian.mods.kubejs.net.NotificationPayload;
import dev.latvian.mods.kubejs.net.SendDataFromServerPayload;
import dev.latvian.mods.kubejs.net.SetActivePostShaderPayload;
import dev.latvian.mods.kubejs.player.PlayerStatsJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.typings.ThisIs;
import dev.latvian.mods.kubejs.util.NotificationToastData;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

@RemapPrefixForJS("kjs$")
public interface ServerPlayerKJS extends PlayerKJS {
	@Override
	@HideFromJS
	default ServerPlayer kjs$self() {
		return (ServerPlayer) this;
	}

	@ThisIs(ServerPlayer.class)
	@Info("Checks if the entity is a server-side player.")
	default boolean kjs$isServerPlayer() {
		return true;
	}

	@Override
	default void kjs$sendData(String channel, @Nullable CompoundTag data) {
		if (!channel.isEmpty()) {
			KubeJSNet.safeSendToPlayer(kjs$self(), new SendDataFromServerPayload(channel, data));
		}
	}

	@Override
	default PlayerStatsJS kjs$getStats() {
		return new PlayerStatsJS(kjs$self(), kjs$self().getStats());
	}

	@Override
	@Info("Checks, whether the player is currently mining a block.")
	default boolean kjs$isMiningBlock() {
		return kjs$self().gameMode.isDestroyingBlock;
	}

	@Override
	default void kjs$setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
		PlayerKJS.super.kjs$setPositionAndRotation(x, y, z, yaw, pitch);
		kjs$self().connection.teleport(x, y, z, yaw, pitch);
	}

	@Info(value = """
		Switches the player's gamemode between Creative and Survival.
		To change the player's gamemode to a mode other than Creative or Survival, use `setGameMode`.
		""", params = {
		@Param(name = "mode", value = """
			`true` to change the player's gamemode to Creative.
			`false` to change the player's gamemode to Survival.
			""")
	})
	default void kjs$setCreativeMode(boolean mode) {
		kjs$self().setGameMode(mode ? GameType.CREATIVE : GameType.SURVIVAL);
	}

	@Info("Checks, whether the player is a server operator.")
	default boolean kjs$isOp() {
		return kjs$self().server.getPlayerList().isOp(kjs$self().nameAndId());
	}

	@Info(value = "Kicks the player from the server with the provided reason.", params = {
		@Param(name = "reason", value = "A text component, containing the kick reason. It may be a string, which will be implicitly wrapped into a text component.")
	})
	default void kjs$kick(Component reason) {
		kjs$self().connection.disconnect(reason);
	}

	@Info("Kicks the player from the server with a generic reason.")
	default void kjs$kick() {
		kjs$kick(Component.translatable("multiplayer.disconnect.kicked"));
	}

	@Info(value = "Bans the player from the server.", params = {
		@Param(name = "banner", value = "A string, that specifies who/what banned the player."),
		@Param(name = "reason", value = "A string, that contains the ban reason."),
		@Param(name = "banDuration", value = "Duration of a ban. Negative durations will result in a 10-year ban.")
	})
	default void kjs$ban(String banner, String reason, Duration banDuration) {
		final long TEN_YEARS_SECONDS = 315569520;
		var start = Instant.now();
		var end = start.plus(banDuration);

		var userlistbansentry = new UserBanListEntry(kjs$self().nameAndId(), Date.from(start), banner, Date.from(start.isBefore(end) ? end : start.plus(Duration.ofSeconds(TEN_YEARS_SECONDS))), reason);
		kjs$self().server.getPlayerList().getBans().add(userlistbansentry);
		kjs$kick(Component.translatable("multiplayer.disconnect.banned"));
	}

	default boolean kjs$isAdvancementDone(Identifier id) {
		var a = kjs$self().server.kjs$getAdvancement(id);
		return a != null && kjs$self().getAdvancements().getOrStartProgress(a.holder()).isDone();
	}

	default void kjs$unlockAdvancement(Identifier id) {
		var a = kjs$self().server.kjs$getAdvancement(id);

		if (a != null) {
			var advancementprogress = kjs$self().getAdvancements().getOrStartProgress(a.holder());

			for (var s : advancementprogress.getRemainingCriteria()) {
				kjs$self().getAdvancements().award(a.holder(), s);
			}
		}
	}

	default void kjs$revokeAdvancement(Identifier id) {
		var a = kjs$self().server.kjs$getAdvancement(id);

		if (a != null) {
			var advancementprogress = kjs$self().getAdvancements().getOrStartProgress(a.holder());

			if (advancementprogress.hasProgress()) {
				for (var s : advancementprogress.getCompletedCriteria()) {
					kjs$self().getAdvancements().revoke(a.holder(), s);
				}
			}
		}
	}

	@Override
	default void kjs$setSelectedSlot(int index) {
		var p = kjs$getSelectedSlot();
		PlayerKJS.super.kjs$setSelectedSlot(index);
		var n = kjs$getSelectedSlot();

		if (p != n && kjs$self().connection != null) {
			kjs$self().connection.send(new ClientboundSetHeldSlotPacket(n));
		}
	}


	@Override
	default void kjs$setMouseItem(ItemStack item) {
		PlayerKJS.super.kjs$setMouseItem(item);

		if (kjs$self().connection != null) {
			kjs$self().inventoryMenu.broadcastChanges();
		}
	}

	default boolean kjs$hasPermission(int i) {
		var permission = new Permission.HasCommandLevel(PermissionLevel.byId(i));
		return kjs$self().permissions().hasPermission(permission);
	}

	@Nullable
	default LevelBlock kjs$getSpawnLocation() {
		var config = kjs$self().getRespawnConfig();
		if (config == null) {
			return null;
		}
		var pos = config.respawnData().pos();
		return pos == null ? null : kjs$getLevel().kjs$getBlock(pos);
	}

	default void kjs$setSpawnLocation(LevelBlock c) {
		var level = c.getLevel();
		var pos = c.getPos();
		var yaw = 0F;
		var pitch = 0F;
		var forced = false;

		var respawn = new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), pos, yaw, pitch), forced);
		kjs$self().setRespawnPosition(respawn, false);
	}

	@Override
	default void kjs$notify(NotificationToastData builder) {
		KubeJSNet.safeSendToPlayer(kjs$self(), new NotificationPayload(builder));
	}

	default void kjs$openChestGUI(Consumer<KubeJSGUI> gui) {
		var data = new KubeJSGUI();
		gui.accept(data);

		kjs$self().openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return data.title;
			}

			@Override
			public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
				return new KubeJSMenu(i, inventory, data);
			}
		}, data::write);
	}

	default void kjs$openInventoryGUI(InventoryKJS inventory, Component title) {
		kjs$openInventoryGUI(inventory, title, inventory.kjs$getWidth());
	}

	default void kjs$openInventoryGUI(InventoryKJS inventory, Component title, int columns) {
		kjs$openInventoryGUI(inventory, title, columns, inventory.kjs$getSlots() / columns);
	}

	default void kjs$openInventoryGUI(InventoryKJS inventory, Component title, int columns, int rows) {
		if (inventory.kjs$getSlots() < columns * rows) {
			throw new RuntimeException("Given container size is unable to contain inventory of size %sx%s!".formatted(columns, rows));
		}
		kjs$openChestGUI(gui -> {
			gui.title = title;
			gui.setInventory(inventory);

			gui.inventoryWidth = columns;
			gui.inventoryHeight = rows;
			gui.height = 114 + gui.inventoryHeight * 18;
			gui.width = 14 + gui.inventoryWidth * 18;
			gui.inventoryLabelY = gui.height - 94;
		});
	}

	@HideFromJS
	default boolean kjs$restoreCapturedInventory() {
		var map = kjs$self().server.kjs$restoreInventories().remove(kjs$self().getUUID());

		if (map == null) {
			return false;
		}

		var playerItems = kjs$self().getInventory().getNonEquipmentItems();

		for (int i = 0; i < playerItems.size(); i++) {
			playerItems.set(i, map.getOrDefault(i, ItemStack.EMPTY));
		}

		return true;
	}

	default Container kjs$captureInventory(boolean autoRestore) {
		var playerItems = kjs$self().getInventory().getNonEquipmentItems();

		var captured = new SimpleContainer(playerItems.size());
		var map = new HashMap<Integer, ItemStack>();

		for (int i = 0; i < playerItems.size(); i++) {
			var c = playerItems.set(i, ItemStack.EMPTY);

			if (!c.isEmpty()) {
				if (autoRestore) {
					map.put(i, c);
				}

				captured.setItem(i, c.copy());
			}
		}

		if (autoRestore && !map.isEmpty()) {
			kjs$self().server.kjs$restoreInventories().put(kjs$self().getUUID(), map);
		}

		return captured;
	}

	default void kjs$openChestGUI(Component title, int rows, Consumer<ChestMenuData> gui) {
		var data = new ChestMenuData(kjs$self(), title, Mth.clamp(rows, 1, 6));
		gui.accept(data);

		if (kjs$self().containerMenu instanceof CustomChestMenu open) {
			data.capturedInventory = open.data.capturedInventory;
		} else {
			data.capturedInventory = kjs$captureInventory(true);
		}

		if (kjs$self().containerMenu instanceof CustomChestMenu open && open.data.rows == data.rows && open.data.title.equals(title)) {
			open.data = data;
			data.sync();
		} else {
			data.sync();

			kjs$self().openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return title;
				}

				@Override
				public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
					return new CustomChestMenu(i, data);
				}
			});
		}
	}

	@Info("Heals the player to full, and fully restores hunger and saturation.")
	default void kjs$heal() {
		kjs$self().heal(kjs$self().getMaxHealth());
		kjs$self().getFoodData().eat(20, 1F);
	}

	@Override
	default void kjs$setActivePostShader(@Nullable Identifier id) {
		KubeJSNet.safeSendToPlayer(kjs$self(), new SetActivePostShaderPayload(Optional.ofNullable(id)));
	}
}
