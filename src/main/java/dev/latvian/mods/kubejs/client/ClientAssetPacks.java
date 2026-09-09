package dev.latvian.mods.kubejs.client;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.generator.KubeResourceGenerator;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.plugin.builtin.event.ClientEvents;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;
import dev.latvian.mods.kubejs.script.data.KubeFileResourcePack;
import dev.latvian.mods.kubejs.script.data.VirtualAssetPack;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.loading.FMLLoader;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientAssetPacks {
	public static final ClientAssetPacks INSTANCE = new ClientAssetPacks();

	public final VirtualAssetPack internalAssetPack;
	public final Map<GeneratedDataStage, VirtualAssetPack> virtualPacks;

	public ClientAssetPacks() {
		this.internalAssetPack = new VirtualAssetPack(GeneratedDataStage.INTERNAL, () -> RegistryAccessContainer.BUILTIN);
		this.virtualPacks = GeneratedDataStage.forScripts(stage -> new VirtualAssetPack(stage, () -> RegistryAccessContainer.BUILTIN));
	}

	public List<PackResources> inject(List<PackResources> original) {
		try {
			return inject0(original);
		} catch (Throwable ex) {
			ConsoleJS.CLIENT.error("Error while generating client assets", ex);
			return original;
		}
	}

	private List<PackResources> inject0(List<PackResources> original) {
		var packs = new ArrayList<>(original);

		var filePacks = new ArrayList<PackResources>();
		KubeFileResourcePack.scanAndLoad(KubeJSPaths.ASSETS, filePacks);
		filePacks.sort((p1, p2) -> p1.packId().compareToIgnoreCase(p2.packId()));
		filePacks.add(new KubeFileResourcePack(PackType.CLIENT_RESOURCES));

		int beforeModsIndex = KubeFileResourcePack.findBeforeModsIndex(packs);
		packs.add(beforeModsIndex, virtualPacks.get(GeneratedDataStage.BEFORE_MODS));

		int afterModsIndex = KubeFileResourcePack.findAfterModsIndex(packs);
		packs.add(afterModsIndex, internalAssetPack);
		packs.add(afterModsIndex + 1, virtualPacks.get(GeneratedDataStage.AFTER_MODS));
		packs.addAll(afterModsIndex + 2, filePacks);
		packs.add(virtualPacks.get(GeneratedDataStage.LAST));

		internalAssetPack.reset();

		for (var builder : RegistryObjectStorage.ALL_BUILDERS) {
			builder.generateAssets(internalAssetPack);
		}

		KubeJSPlugins.forEachPlugin(internalAssetPack, KubeJSPlugin::generateAssets);

		/*
		// Originally internalAssetPack.buildSounds();

		for (var pack : packs) {
			if (pack instanceof KubeResourceGenerator generator) {
				generator.flush();
			}
		}
		 */

		var langMap = new HashMap<LangKubeEvent.Key, String>();
		var langEvents = new HashMap<String, LangKubeEvent>();
		var enUsLangEvent = langEvents.computeIfAbsent("en_us", s -> new LangKubeEvent(s, langMap));

		for (var builder : RegistryObjectStorage.ALL_BUILDERS) {
			builder.generateLang(enUsLangEvent);
		}

		KubeJSPlugins.forEachPlugin(enUsLangEvent, KubeJSPlugin::generateLang);

		ClientEvents.GENERATE_ASSETS.post(ScriptType.CLIENT, GeneratedDataStage.AFTER_MODS, virtualPacks.get(GeneratedDataStage.AFTER_MODS));

		for (var lang : ClientEvents.LANG.findUniqueExtraIds(ScriptType.CLIENT)) {
			var l = String.valueOf(lang);

			if (LangKubeEvent.PATTERN.matcher(l).matches()) {
				ClientEvents.LANG.post(ScriptType.CLIENT, l, langEvents.computeIfAbsent(l, k -> new LangKubeEvent(k, langMap)));
			} else {
				ConsoleJS.CLIENT.error("Invalid language key: " + l);
			}
		}

		try {
			for (var dir : Files.list(KubeJSPaths.ASSETS).filter(Files::isDirectory).toList()) {
				var ns = dir.getFileName().toString();

				var langDir = dir.resolve("lang");

				if (Files.exists(langDir) && Files.isDirectory(langDir)) {
					for (var path : Files.list(langDir).filter(Files::isRegularFile).filter(Files::isReadable).toList()) {
						var fileName = path.getFileName().toString();

						if (fileName.endsWith(".json")) {
							try (var reader = Files.newBufferedReader(path)) {
								var json = JsonUtils.GSON.fromJson(reader, JsonObject.class);
								var lang = fileName.substring(0, fileName.length() - 5);

								for (var entry : json.entrySet()) {
									langMap.put(new LangKubeEvent.Key(ns, lang, entry.getKey()), entry.getValue().getAsString());
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		var finalMap = new HashMap<String, Map<String, JsonObject>>();

		for (var entry : langMap.entrySet()) {
			var ns = finalMap.computeIfAbsent(entry.getKey().namespace(), s -> new HashMap<>());
			var lang = ns.computeIfAbsent(entry.getKey().lang(), s -> new JsonObject());
			lang.addProperty(entry.getKey().key(), entry.getValue());
		}

		for (var e1 : finalMap.entrySet()) { // namespaces
			for (var e2 : e1.getValue().entrySet()) { // languages
				internalAssetPack.json(ResourceLocation.parse(e1.getKey() + ":lang/" + e2.getKey()), e2.getValue());
			}
		}

		for (var pack : virtualPacks.values()) {
			pack.reset();

			if (ClientEvents.GENERATE_ASSETS.hasListeners(pack.stage)) {
				ClientEvents.GENERATE_ASSETS.post(ScriptType.CLIENT, pack.stage, pack);
			}
		}

		for (var pack : packs) {
			if (pack instanceof KubeResourceGenerator generator) {
				generator.flush();
			}
		}

		if (!FMLLoader.isProduction()) {
			KubeJS.LOGGER.info("Loaded {} asset packs: {}", packs.size(), packs.stream().map(PackResources::packId).collect(Collectors.joining(", ")));
		}

		return packs;
	}
}
