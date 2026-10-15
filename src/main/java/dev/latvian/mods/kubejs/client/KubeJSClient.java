package dev.latvian.mods.kubejs.client;

import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.KubeJSCommon;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.item.ModifyItemTooltipsKubeEvent;
import dev.latvian.mods.kubejs.net.KubeServerData;
import dev.latvian.mods.kubejs.net.NetworkKubeEvent;
import dev.latvian.mods.kubejs.plugin.builtin.event.ItemEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.NetworkEvents;
import dev.latvian.mods.kubejs.script.ConsoleLine;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.data.ExportablePackResources;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;
import dev.latvian.mods.kubejs.script.data.VirtualAssetPack;
import dev.latvian.mods.kubejs.text.tooltip.ItemTooltipData;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class KubeJSClient extends KubeJSCommon {
	public static final Identifier WHITE_TEXTURE = Identifier.parse("textures/misc/white.png");
	public static final Identifier RECIPE_BUTTON_TEXTURE = Identifier.parse("textures/gui/recipe_button.png");

	public static final Map<GeneratedDataStage, VirtualAssetPack> CLIENT_PACKS = new EnumMap<>(GeneratedDataStage.class);
	public static List<ItemTooltipData> clientItemTooltips = List.of();

	static {
		for (var stage : GeneratedDataStage.values()) {
			CLIENT_PACKS.put(stage, new VirtualAssetPack(stage, () -> RegistryAccessContainer.BUILTIN));
		}
	}

	public static void reloadClientScripts() {
		KubeJS.getClientScriptManager().reload();
		var list = new ArrayList<ItemTooltipData>();
		ItemEvents.MODIFY_TOOLTIPS.post(ScriptType.CLIENT, new ModifyItemTooltipsKubeEvent(list::add));
		clientItemTooltips = List.copyOf(list);
	}

	public static void copyDefaultOptionsFile(File optionsFile) {
		if (!optionsFile.exists()) {
			var defOptions = KubeJSPaths.CONFIG.resolve("defaultoptions.txt");

			if (Files.exists(defOptions)) {
				try {
					KubeJS.LOGGER.info("Loaded default options from kubejs/config/defaultoptions.txt");
					final PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(optionsFile), StandardCharsets.UTF_8));
					printwriter.println("version:" + SharedConstants.getCurrentVersion().dataVersion().version());
					printwriter.print(Files.readString(defOptions));
					printwriter.close();
				} catch (IOException ex) {
					KubeJS.LOGGER.error("Failed to save default options to options.txt!");
				}
			}
		}
	}

	@Override
	public void handleDataFromServerPacket(String channel, @Nullable CompoundTag data) {
		if (NetworkEvents.DATA_RECEIVED.hasListeners(channel)) {
			NetworkEvents.DATA_RECEIVED.post(ScriptType.CLIENT, channel, new NetworkKubeEvent(Minecraft.getInstance().player, channel, data));
		}
	}

	@Override
	@Nullable
	public Player getClientPlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public void generateTypings(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("WIP!"), false);
	}

	@Override
	public void reloadConfig() {
		super.reloadConfig();
		ClientProperties.reload();
	}

	@Override
	public void reloadStartupScripts(boolean dedicated) {
		var mc = Minecraft.getInstance();

		if (mc.player != null) {
			CreativeModeTabs.CACHED_PARAMETERS = null;
			CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), mc.player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get(), mc.level.registryAccess());
		}
	}

	@Override
	public void export(List<ExportablePackResources> packs) {
		for (var pack : Minecraft.getInstance().getResourceManager().listPacks().toList()) {
			if (pack instanceof ExportablePackResources e && !packs.contains(e)) {
				packs.add(e);
			}
		}
	}

	@Override
	public void openErrors(ScriptType type) {
		runInMainThread(() -> Minecraft.getInstance().gui.setScreen(new KubeJSErrorScreen(null, type.console, true)));
	}

	@Override
	public void openErrors(ScriptType type, List<ConsoleLine> errors, List<ConsoleLine> warnings) {
		runInMainThread(() -> Minecraft.getInstance().gui.setScreen(new KubeJSErrorScreen(null, type, null, errors, warnings, true)));
	}

	@Override
	public void runInMainThread(Runnable runnable) {
		var mc = Minecraft.getInstance();

		if (mc != null) {
			mc.execute(runnable);
		} else {
			runnable.run();
		}
	}

	@Override
	public void updateServerData(KubeServerData data) {
		var sessionData = KubeSessionData.of(Minecraft.getInstance());

		if (sessionData != null) {
			sessionData.sync(data);
		}
	}

	@Override
	public String getWebServerWindowTitle() {
		var mc = Minecraft.getInstance();
		return mc.getGameProfile().name() + ", " + mc.kjs$getTitle();
	}

	private static final char[] POWER = {'K', 'M', 'B', 'T'};

	public static String formatNumber(int count) {
		if (Minecraft.getInstance().hasAltDown()) {
			return String.format("%,d", count);
		}

		int index = 0;

		if (count > 9999) {
			while (count / 1000 != 0) {
				count /= 1000;
				index++;
			}
		}

		if (index > 0) {
			return count + String.valueOf(POWER[index - 1]);
		} else {
			return String.valueOf(count);
		}
	}

	public static int drawStackSize(GuiGraphicsExtractor graphics, Font font, int size, int x, int y, int color, boolean dropShadow) {
		var str = formatNumber(size);
		int w = font.width(str);
		float scale = ClientProperties.get().shrinkStackSizeText ? (str.length() >= 4 ? 0.5F : str.length() == 3 ? 0.75F : 1F) : 1F;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 16F - (w - 1F) * scale, y + 16F - 7F * scale);
		graphics.pose().scale(scale, scale);

		graphics.text(font, str, 0, 0, color, dropShadow);

		graphics.pose().popMatrix();
		return Mth.ceil(w * scale);
	}
}