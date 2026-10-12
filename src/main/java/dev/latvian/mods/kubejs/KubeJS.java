package dev.latvian.mods.kubejs;

import com.google.common.base.Stopwatch;
import dev.latvian.mods.kubejs.block.entity.KubeJSAttachmentTypes;
import dev.latvian.mods.kubejs.client.ClientScriptManager;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.fluid.KubeJSFluidIngredients;
import dev.latvian.mods.kubejs.gui.KubeJSMenus;
import dev.latvian.mods.kubejs.holder.KubeJSHolderSets;
import dev.latvian.mods.kubejs.ingredient.KubeJSIngredients;
import dev.latvian.mods.kubejs.item.creativetab.KubeJSCreativeTabs;
import dev.latvian.mods.kubejs.level.ruletest.KubeJSRuleTests;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.plugin.builtin.event.StartupEvents;
import dev.latvian.mods.kubejs.recipe.KubeJSRecipeSerializers;
import dev.latvian.mods.kubejs.registry.RegistryType;
import dev.latvian.mods.kubejs.script.KubeJSBackgroundThread;
import dev.latvian.mods.kubejs.script.PlatformWrapper;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.data.KubeFileResourcePack;
import dev.latvian.mods.kubejs.util.RecordDefaults;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.NullUnmarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

@Mod(KubeJS.MOD_ID)
@NullUnmarked
public class KubeJS {
	public static final String MOD_ID = "kubejs";
	public static final String MOD_NAME = "KubeJS";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	public static final int MC_VERSION_NUMBER = 2601;
	public static final String MC_VERSION_STRING = "26.1";
	public static String QUERY;
	public static String VERSION = "0";
	public static String DISPLAY_NAME = "KubeJS";
	public static final Identifier EMPTY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "empty");

	public static Identifier id(String path) {
		return EMPTY_ID.withPath(path);
	}

	public static IEventBus modEventBus;
	public static ModContainer thisMod;

	public static KubeJSCommon PROXY = new KubeJSCommon();

	private static ScriptManager startupScriptManager, clientScriptManager;

	public static ScriptManager getStartupScriptManager() {
		return startupScriptManager;
	}

	public static ScriptManager getClientScriptManager() {
		return clientScriptManager;
	}

	public KubeJS(IEventBus bus, Dist dist, ModContainer mod) throws Throwable {
		modEventBus = bus;
		thisMod = mod;
		VERSION = mod.getModInfo().getVersion().toString();
		DISPLAY_NAME = "KubeJS " + VERSION;
		QUERY = "source=kubejs&mc=" + MC_VERSION_NUMBER + "&loader=neoforge&v=" + URLEncoder.encode(mod.getModInfo().getVersion().toString(), StandardCharsets.UTF_8);

		if (Files.notExists(KubeJSPaths.README)) {
			try {
				Files.writeString(KubeJSPaths.README, """
					Find out more info on the website: https://kubejs.com/
					
					Directory information:
					
					assets - Acts as a resource pack, you can put any client resources in here, like textures, models, etc. Example: assets/kubejs/textures/item/test_item.png
					data - Acts as a datapack, you can put any server resources in here, like loot tables, functions, etc. Example: data/kubejs/loot_tables/blocks/test_block.json
					
					startup_scripts - Scripts that get loaded once during game startup - Used for adding items and other things that can only happen while the game is loading (Can be reloaded with /kubejs reload_startup_scripts, but it may not work!)
					server_scripts - Scripts that get loaded every time server resources reload - Used for modifying recipes, tags, loot tables, and handling server events (Can be reloaded with /reload)
					client_scripts - Scripts that get loaded every time client resources reload - Used for JEI events, tooltips and other client side things (Can be reloaded with F3+T)
					
					config - KubeJS config storage. This is also the only directory that scripts can access other than world directory
					exported - Data dumps like texture atlases end up here
					
					You can find type-specific logs in logs/kubejs/ directory
					""".trim()
				);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		RecordDefaults.init();

		boolean datagen = PlatformWrapper.isGeneratingData();

		if (!datagen) {
			new KubeJSBackgroundThread().start();
			ScriptType.STARTUP.console.startCapturingErrors();
			ScriptType.CLIENT.console.startCapturingErrors();
		}

		LOGGER.info("Loading vanilla registries...");
		RegistryType.Scanner.init();

		var pluginTimer = Stopwatch.createStarted();
		LOGGER.info("Looking for KubeJS plugins...");
		var allMods = new ArrayList<>(ModList.get().getMods().stream().map(IModInfo::getOwningFile).map(IModFileInfo::getFile).toList());
		var thisModFile = mod.getModInfo().getOwningFile().getFile();
		allMods.remove(thisModFile);
		allMods.addFirst(thisModFile);
		KubeJSPlugins.load(allMods, dist == Dist.CLIENT);
		LOGGER.info("Done in {}", pluginTimer.stop());

		startupScriptManager = new StartupScriptManager();

		KubeJSPlugins.forEachPlugin(KubeJSPlugin::init);

		if (!datagen) {
			startupScriptManager.reload();
		}

		if (dist.isClient()) {
			clientScriptManager = new ClientScriptManager();

			if (!datagen) {
				clientScriptManager.reload();
			}
		}

		KubeJSPlugins.forEachPlugin(KubeJSPlugin::initStartup);

		if (dist.isClient()) {
			KubeFileResourcePack.scanForInvalidFiles("kubejs/assets/", KubeJSPaths.ASSETS);
		}

		KubeFileResourcePack.scanForInvalidFiles("kubejs/data/", KubeJSPaths.DATA);

		if (dist.isClient() || !CommonProperties.get().serverOnly) {
			// See NeoForgeRegistriesSetup.VANILLA_SYNC_REGISTRIES
			NeoForgeMod.enableMilkFluid();
			// KubeJSComponents.REGISTRY.register(bus);
			KubeJSRecipeSerializers.REGISTRY.register(bus);
			KubeJSMenus.REGISTRY.register(bus);
			KubeJSIngredients.REGISTRY.register(bus);
			KubeJSFluidIngredients.REGISTRY.register(bus);
			KubeJSHolderSets.REGISTRY.register(bus);
		}

		KubeJSCreativeTabs.REGISTRY.register(bus);
		KubeJSRuleTests.REGISTRY.register(bus);
		KubeJSAttachmentTypes.REGISTRY.register(bus);

		StartupEvents.INIT.post(ScriptType.STARTUP, KubeStartupEvent.BASIC);
		// KubeJSRegistries.chunkGenerators().register(new Identifier(KubeJS.MOD_ID, "flat"), () -> KJSFlatLevelSource.CODEC);
	}
}