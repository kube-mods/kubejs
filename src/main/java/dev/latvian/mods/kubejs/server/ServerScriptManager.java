package dev.latvian.mods.kubejs.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.core.ScriptManagerHolderKJS;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.net.SyncServerDataPayload;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaStorage;
import dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.registry.ServerRegistryKubeEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;
import dev.latvian.mods.kubejs.script.data.KubeFileResourcePack;
import dev.latvian.mods.kubejs.script.data.VirtualDataPack;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerScriptManager extends ScriptManager {
	@ApiStatus.Internal
	public static ServerScriptManager createForDataGen() {
		var manager = new ServerScriptManager(true);
		manager.reload(); // Is this needed?
		return manager;
	}

	@ApiStatus.Internal
	public static MultiPackResourceManager bindServerResources(List<PackResources> original, boolean firstLoad, BiFunction<PackType, List<PackResources>, MultiPackResourceManager> ctor) {
		var packs = new ArrayList<>(original);

		var filePacks = new ArrayList<PackResources>();
		KubeFileResourcePack.scanAndLoad(KubeJSPaths.DATA, filePacks);
		filePacks.sort((p1, p2) -> p1.packId().compareToIgnoreCase(p2.packId()));
		filePacks.add(new KubeFileResourcePack(PackType.SERVER_DATA));

		int beforeModsIndex = KubeFileResourcePack.findBeforeModsIndex(packs);
		int afterModsIndex = KubeFileResourcePack.findAfterModsIndex(packs);

		var manager = new ServerScriptManager(firstLoad);

		packs.add(beforeModsIndex, manager.virtualPacks.get(GeneratedDataStage.BEFORE_MODS));
		packs.add(afterModsIndex, manager.internalDataPack);
		packs.add(afterModsIndex + 1, manager.registriesDataPack);
		packs.add(afterModsIndex + 2, manager.virtualPacks.get(GeneratedDataStage.AFTER_MODS));
		packs.addAll(afterModsIndex + 3, filePacks);
		packs.add(manager.virtualPacks.get(GeneratedDataStage.LAST));
		manager.reload();

		if (!FMLLoader.getCurrent().isProduction()) {
			KubeJS.LOGGER.info("Loaded {} data packs: {}", packs.size(), packs.stream().map(PackResources::packId).collect(Collectors.joining(", ")));
		}

		var resources = ctor.apply(PackType.SERVER_DATA, packs);
		((ScriptManagerHolderKJS) resources).kjs$setScriptManager(manager);

		return resources;
	}

	public final RecipeSchemaStorage recipeSchemaStorage;
	public @Nullable SyncServerDataPayload serverData;
	public final VirtualDataPack internalDataPack;
	public final VirtualDataPack registriesDataPack;
	public final Map<GeneratedDataStage, VirtualDataPack> virtualPacks;
	public final Map<Identifier, Set<Identifier>> serverRegistryTags;
	public boolean firstLoad;

	private ServerScriptManager(boolean firstLoad) {
		super(ScriptType.SERVER);
		this.recipeSchemaStorage = new RecipeSchemaStorage(this);
		this.serverData = null;

		this.internalDataPack = new VirtualDataPack(GeneratedDataStage.INTERNAL, this::getRegistries);
		this.registriesDataPack = new VirtualDataPack(GeneratedDataStage.REGISTRIES, this::getRegistries);
		this.virtualPacks = GeneratedDataStage.forScripts(stage -> new VirtualDataPack(stage, this::getRegistries));
		serverRegistryTags = new HashMap<>();

		this.firstLoad = firstLoad;

		try {
			if (Files.notExists(KubeJSPaths.DATA)) {
				Files.createDirectories(KubeJSPaths.DATA);
			}
		} catch (Throwable ex) {
			throw new RuntimeException("KubeJS failed to register it's script loader!", ex);
		}
	}

	@Override
	public void loadFromDirectory() {
		ConsoleJS.SERVER.startCapturingErrors();
		super.loadFromDirectory();

		if (FMLLoader.getCurrent().getDist().isDedicatedServer()) {
			loadPackFromDirectory(KubeJSPaths.LOCAL_SERVER_SCRIPTS, "local server", true);
		}
	}

	private record AdditionalServerRegistryHandler(SourceLine sourceLine, List<BuilderBase<?>> builders) implements AdditionalObjectRegistry {
		@Override
		public <T> void add(ResourceKey<Registry<T>> registry, BuilderBase<? extends T> builder) {
			builder.sourceLine = sourceLine;
			builder.registryKey = (ResourceKey) registry;
			builders.add(builder);
		}
	}

	@Override
	public void loadAdditional() {
		for (var builder : RegistryObjectStorage.ALL_BUILDERS) {
			builder.generateData(internalDataPack);
		}

		KubeJSPlugins.forEachPlugin(internalDataPack, KubeJSPlugin::generateData);
		internalDataPack.flush();

		if (firstLoad) {
			firstLoad = false;

			if (ServerEvents.REGISTRY.hasListeners()) {
				var builders = new ArrayList<BuilderBase<?>>();

				var current = RegistryAccessContainer.current;
				RegistryAccessContainer.current = new RegistryAccessContainer(new RegistryAccess.Frozen() {

					final Map<ResourceKey<? extends Registry<?>>, Optional<Registry<?>>> registries = new HashMap<>();

					@Override
					public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
						return Cast.to(registries.computeIfAbsent(registryKey, key -> {
							var c = current.lookup(key);
							if (c.isPresent()) {
								return Cast.to(c);
							}
							return Optional.of(new MappedRegistry(key, Lifecycle.experimental()));
						}));
					}

					@Override
					public Stream<RegistryEntry<?>> registries() {
						return Stream.empty();
					}
				});

				var ops = RegistryAccessContainer.current.json();

				var codecs = new Reference2ObjectOpenHashMap<ResourceKey<?>, Codec<?>>();

				for (var reg : DataPackRegistriesHooks.getReloadableRegistries()) {
					var key = (ResourceKey) reg.key();
					codecs.put(key, reg.elementCodec());

					if (ServerEvents.REGISTRY.hasListeners(key)) {
						ServerEvents.REGISTRY.post(ScriptType.SERVER, key, new ServerRegistryKubeEvent<>(key, ops, reg.elementCodec(), builders));
					}
				}

				for (var b : List.copyOf(builders)) {
					b.createAdditionalObjects(new AdditionalServerRegistryHandler(b.sourceLine, builders));
				}

				for (var b : builders) {
					b.generateData(registriesDataPack);

					if (!b.defaultTags.isEmpty()) {
						serverRegistryTags.put(b.id, b.defaultTags);
					}
				}

				for (var b : builders) {
					if (b.registryKey == null) {
						ConsoleJS.SERVER.error("", new KubeRuntimeException("Failed to register object '" + b.id + "' - unknown registry").source(b.sourceLine));
						continue;
					}

					try {
						var codec = codecs.get(b.registryKey);

						if (codec == null) {
							throw new KubeRuntimeException("Don't know how to encode '" + b.id + "' of '" + b.registryKey.identifier() + "'!").source(b.sourceLine);
						}

						var obj = b.createTransformedObject();
						var json = codec.encodeStart(ops, Cast.to(obj)).getOrThrow();
						var k = b.registryKey.identifier();

						if (k.getNamespace().equals("minecraft")) {
							registriesDataPack.json(Identifier.fromNamespaceAndPath(b.id.getNamespace(), k.getPath() + "/" + b.id.getPath()), json);
						} else {
							registriesDataPack.json(Identifier.fromNamespaceAndPath(b.id.getNamespace(), k.getNamespace() + "/" + k.getPath() + "/" + b.id.getPath()), json);
						}
					} catch (Exception ex) {
						ConsoleJS.SERVER.error("", new KubeRuntimeException("Failed to register object '" + b.id + "' of registry '" + b.registryKey.identifier() + "'!", ex).source(b.sourceLine));
					}
				}

				registriesDataPack.flush();

				RegistryAccessContainer.current = current;
			}
		}
	}

	@Override
	public void reload() {
		internalDataPack.reset();

		for (var pack : virtualPacks.values()) {
			pack.reset();
		}

		serverData = null;

		super.reload();

		internalDataPack.flush();

		for (var pack : virtualPacks.values()) {
			if (ServerEvents.GENERATE_DATA.hasListeners(pack.stage)) {
				ServerEvents.GENERATE_DATA.post(ScriptType.SERVER, pack.stage, pack);
			}

			pack.flush();
		}
	}

	@Override
	protected void fullReload() {
		var server = ServerLifecycleHooks.getCurrentServer();

		if (server != null) {
			server.execute(() -> server.kjs$runCommand("reload"));
		}
	}
}
