package dev.latvian.mods.kubejs;

import dev.latvian.mods.kubejs.block.entity.BlockEntityBuilder;
import dev.latvian.mods.kubejs.block.entity.CustomCapabilityAttachment;
import dev.latvian.mods.kubejs.block.entity.KubeBlockEntity;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.item.creativetab.CreativeTabCallbackForge;
import dev.latvian.mods.kubejs.item.creativetab.CreativeTabKubeEvent;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.plugin.builtin.event.StartupEvents;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ConsoleLine;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.ScriptsLoadedEvent;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;

@EventBusSubscriber(modid = KubeJS.MOD_ID)
public class KubeJSModEventHandler {
	@SubscribeEvent
	public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
		UtilsJS.postItemModificationEvents(event);
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
		var tabId = event.getTabKey().identifier();

		if (StartupEvents.MODIFY_CREATIVE_TAB.hasListeners(tabId)) {
			StartupEvents.MODIFY_CREATIVE_TAB.post(ScriptType.STARTUP, tabId, new CreativeTabKubeEvent(event.getTab(), event.hasPermissions(), new CreativeTabCallbackForge(event)));
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void loadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(KubeJSModEventHandler::loadComplete0);
	}

	private static void loadComplete0() {
		KubeJSPlugins.forEachPlugin(KubeJSPlugin::afterInit);
		NeoForge.EVENT_BUS.post(new ScriptsLoadedEvent());
		StartupEvents.POST_INIT.post(ScriptType.STARTUP, KubeStartupEvent.BASIC);
		UtilsJS.postBlockModificationEvents();

		if (!ConsoleJS.STARTUP.errors.isEmpty()) {
			var list = new ArrayList<String>();
			list.add("Startup script errors:");

			var lines = ConsoleJS.STARTUP.errors.toArray(ConsoleLine.EMPTY_ARRAY);

			for (int i = 0; i < lines.length; i++) {
				list.add((i + 1) + ") " + lines[i]);
			}

			KubeJS.LOGGER.error(String.join("\n", list));

			ConsoleJS.STARTUP.flush(true);

			if (FMLLoader.getCurrent().getDist().isDedicatedServer() || !CommonProperties.get().startupErrorGUI) {
				throw new RuntimeException("There were KubeJS startup script syntax errors! See logs/kubejs/startup.log for more info");
			}
		}

		ConsoleJS.STARTUP.stopCapturingErrors();
		ConsoleJS.CLIENT.stopCapturingErrors();

		Util.nonCriticalIoPool().execute(() -> {
			try {
				var response = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.connectTimeout(Duration.ofSeconds(5L))
					.build()
					.send(HttpRequest.newBuilder().uri(URI.create("https://v.kubejs.com/update-check?" + KubeJS.QUERY)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() == 200) {
					var body = response.body().trim();

					if (!body.isEmpty()) {
						ConsoleJS.STARTUP.info("Update available: " + body);
					}
				}
			} catch (Exception ignored) {
			}
		});
	}

	private static boolean directionMatches(EnumSet<Direction> directions, @Nullable Object from) {
		return directions.isEmpty() || (from instanceof Direction d && directions.contains(d));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		for (var info : RegistryObjectStorage.BLOCK_ENTITY.objects.values().stream()
			.filter(BlockEntityBuilder.class::isInstance)
			.map(b -> ((BlockEntityBuilder) b).info).toList()) {
			var beType = (BlockEntityType<KubeBlockEntity>) info.entityType;
			for (var config : info.energyConfigs) {
				registerDirectional(event, Capabilities.Energy.BLOCK, beType, config.directions(),
					(entity, from) -> entity.energyWrappers.get(config.id()));
			}

			for (var config : info.fluidConfigs) {
				registerDirectional(event, Capabilities.Fluid.BLOCK, beType, config.directions(),
					(entity, from) -> entity.fluidWrappers.get(config.id()));
			}

			for (var config : info.inventoryConfigs) {
				registerDirectional(event, Capabilities.Item.BLOCK, beType, config.directions(),
					(entity, from) -> entity.inventoryWrappers.get(config.id()));
			}

			for (var config : info.customCapConfigs) {
				registerCustomCapability(event, config, beType);
			}
		}
	}

	private static <CAP> void registerDirectional(
		RegisterCapabilitiesEvent event,
		BlockCapability<CAP, Direction> capability,
		BlockEntityType<KubeBlockEntity> beType,
		EnumSet<Direction> directions,
		ICapabilityProvider<KubeBlockEntity, Direction, CAP> provider
	) {
		event.registerBlockEntity(capability, beType, (entity, from) -> {
			if (directionMatches(directions, from)) {
				return provider.getCapability(entity, from);
			}
			return null;
		});
	}

	private static void registerCustomCapability(
		RegisterCapabilitiesEvent event,
		CustomCapabilityAttachment.Config config,
		BlockEntityType<KubeBlockEntity> beType
	) {
		registerDirectional(event, (BlockCapability) config.capability(), beType, config.directions(),
			(entity, from) -> {
				var cap = entity.customCapabilities.get(config.id());
				return cap != null ? cap.getCapability(config.capability()) : null;
			});
	}
}
