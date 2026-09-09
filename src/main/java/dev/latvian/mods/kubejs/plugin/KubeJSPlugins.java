package dev.latvian.mods.kubejs.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ModResourceBindings;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Discovers, loads, and stores all [KubeJSPlugin] instances.
/// Use [#forEachPlugin] to iterate over all loaded plugins.
public class KubeJSPlugins {
	private static final Gson GSON = new GsonBuilder().create();

	private static final List<KubeJSPlugin> LIST = new ArrayList<>();
	private static final List<String> GLOBAL_CLASS_FILTER = new ArrayList<>();
	private static final ModResourceBindings BINDINGS = new ModResourceBindings();

	/// Plugin definitions are collected while mods are scanned. Resolved and instantiated once every
	/// mod bas been scanned, so 'after' ordering can function with all loaded plugins
	private static final List<PendingPlugin> PENDING = new ArrayList<>();

	/// Scans all mod JARs for plugin, [ClassFilter] and bindings definitions, and loads them if present.
	///
	/// @see #loadMod(String, IModFile, boolean)
	public static void load(List<IModFile> modFiles, boolean loadClientPlugins) {
		try {
			for (var file : modFiles) {
				if (!file.getModInfos().isEmpty()) {
					loadMod(file.getModInfos().getFirst().getModId(), file, loadClientPlugins);
				}
			}

			resolvePendingPlugins();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load KubeJS plugin", ex);
		}
	}

	/// Given a mod file, checks if it has a file defining KubeJS plugins, class filter rules, or bindings,
	/// and tries to load them if they exist.
	///
	/// For the JSON syntax, see [#loadFromJson(java.lang.String, java.lang.String, boolean)]. The legacy
	/// text syntax is documented on [#loadLegacyFromFile(java.util.stream.Stream, java.lang.String, boolean)].
	private static void loadMod(String modId, IModFile mod, boolean loadClientPlugins) throws IOException {
		var contents = mod.getContents();

		var pluginData = contents.readFile("kube.plugin.json");
		if (pluginData != null) {
			loadFromJson(new String(pluginData, StandardCharsets.UTF_8), modId, loadClientPlugins);
		} else {
			var legacyPluginData = contents.readFile("kubejs.plugins.txt");
			if (legacyPluginData != null) {
				KubeJS.LOGGER.warn("KubeJS Plugin text files are deprecated. Move to kube.plugin.json instead");
				loadLegacyFromFile(new String(legacyPluginData, StandardCharsets.UTF_8).lines(), modId, loadClientPlugins);
			}
		}
	}

	// "Duplicate logs" okay fine idea
	private static void logFailedToParse(String source, String message) {
		KubeJS.LOGGER.error("Failed to parse kube.plugin.json from {}: {}", source, message);
	}

	private static void loadFromJson(String json, String source, boolean loadClientPlugins) {
		JsonElement element;

		try {
			element = GSON.fromJson(json, JsonElement.class);
		} catch (JsonSyntaxException ex) {
			if (DevProperties.get().logErroringPlugins) {
				logFailedToParse(source, ex.getMessage());
			}
			return;
		}

		var result = PLUGIN_DATA_CODEC.parse(JsonOps.INSTANCE, element);
		var dataOptional = result.result();

		if (dataOptional.isEmpty()) {
			if (DevProperties.get().logErroringPlugins) {
				var message = result.error().map(DataResult.Error::message).orElse("unknown error");
				logFailedToParse(source, message);
			}
			return;
		}

		var data = dataOptional.get();

		logFoundSource(source);

		for (var entry : data.plugins()) {
			if (entry.pluginClass().isEmpty()) {
				if (DevProperties.get().logSkippedPlugins) {
					KubeJS.LOGGER.warn("Plugin class in {} not found, skipping", source);
				}
				continue;
			}

			var pluginClass = entry.pluginClass().get();

			if (entry.clientOnly() && !loadClientPlugins) {
				logClientOnlySkip(pluginClass.getName());
				continue;
			}

			if (!entry.requiredMods().isEmpty()) {
				var missing = entry.requiredMods().stream().filter(m -> !ModList.get().isLoaded(m)).toList();

				if (!missing.isEmpty()) {
					if (DevProperties.get().logSkippedPlugins) {
						KubeJS.LOGGER.warn("Plugin {} does not have required mod(s) {} loaded, skipping", pluginClass.getName(), missing);
					}

					continue;
				}
			}

			PENDING.add(new PendingPlugin(
				source,
				entry.id(),
				pluginClass,
				entry.after()
			));
		}

		// Adding '+' and '-' into the GLOBAL_CLASS_FILTER is needed for legacy support, and for the createClassFilter method
		// Removing that would require a little rework on how it works in here

		data.classFilter().ifPresent(classFilter -> {
			for (var s : classFilter.allow()) {
				if (!s.isBlank()) {
					GLOBAL_CLASS_FILTER.add("+" + s.trim());
				}
			}

			for (var s : classFilter.deny()) {
				if (!s.isBlank()) {
					GLOBAL_CLASS_FILTER.add("-" + s.trim());
				}
			}
		});
	}

	private static void logFoundSource(String source) {
		KubeJS.LOGGER.info("Found plugin source {}", source);
	}

	private static void logClientOnlySkip(String name) {
		if (DevProperties.get().logSkippedPlugins) {
			KubeJS.LOGGER.warn("Plugin {} does not load on server side, skipping", name);
		}
	}

	/// Tries to load KubeJS plugins based on the contents of a `kubejs.plugins.txt` file.
	///
	/// A plugin definition consists of a FQCN referring to a class that implements [KubeJSPlugin],
	/// followed by an optional list of *mod ids* which are required for the plugin to be loaded.
	/// The string "client" may be used to ensure a plugin only loads on the client side.
	///
	/// Filters can be used to make sure that certain plugins only load if a mod is present.
	private static void loadLegacyFromFile(Stream<String> contents, String source, boolean loadClientPlugins) {
		logFoundSource(source);

		contents.map(s -> s.split("#", 2)[0].trim()) // allow comments (#)
			.filter(s -> !s.isBlank()) // filter empty lines
			.flatMap(s -> {
				String[] line = s.split(" ");

				for (int i = 1; i < line.length; i++) {
					if (line[i].equalsIgnoreCase("client")) {
						if (!loadClientPlugins) {
							logClientOnlySkip(line[0]);
							return Stream.empty();
						}
					} else if (!ModList.get().isLoaded(line[i])) {
						if (DevProperties.get().logSkippedPlugins) {
							KubeJS.LOGGER.warn("Plugin {} does not have required mod '{}' loaded, skipping", line[0], line[i]);
						}

						return Stream.empty();
					}
				}

				try {
					return Stream.of(Class.forName(line[0], false, KubeJSPlugins.class.getClassLoader()));
				} catch (Throwable t) {
					KubeJS.LOGGER.error("Failed to load plugin {} from source {}", s, source, t);
					return Stream.empty();
				}
			})
			.filter(KubeJSPlugin.class::isAssignableFrom)
			.forEach(c -> {
				try {
					LIST.add((KubeJSPlugin) c.getDeclaredConstructor().newInstance()); // create the actual plugin instance
				} catch (Throwable t) {
					KubeJS.LOGGER.error("Failed to init KubeJS plugin {} from source {}: {}", c.getName(), source, t);
				}
			});
	}

	/// Sorts [#PENDING] plugins by their `after` dependencies (referring to other plugins
	/// `id`), then instantiates each one in order and adds it to [#LIST]. Falls back to declaration order
	/// for any plugins involved in a dependency cycle.
	private static void resolvePendingPlugins() {
		if (PENDING.isEmpty()) {
			return;
		}

		var byId = new HashMap<String, PendingPlugin>();

		for (var p : PENDING) {
			if (p.id().isPresent() && !p.id().get().isBlank() && byId.putIfAbsent(p.id().get(), p) != null) {
				KubeJS.LOGGER.warn("Duplicate plugin id '{}' from source {}, 'after' references to it may be ambiguous", p.id(), p.source());
			}
		}

		var inDegree = new HashMap<PendingPlugin, Integer>();
		var dependents = new HashMap<PendingPlugin, List<PendingPlugin>>();

		for (var p : PENDING) {
			inDegree.put(p, 0);
			dependents.put(p, new ArrayList<>());
		}

		for (var p : PENDING) {
			for (var afterId : p.after()) {
				var dep = byId.get(afterId);

				if (dep == null || dep == p) {
					continue;
				}

				dependents.get(dep).add(p);
				inDegree.merge(p, 1, Integer::sum);
			}
		}

		Deque<PendingPlugin> ready = new ArrayDeque<>();

		for (var p : PENDING) {
			if (inDegree.get(p) == 0) {
				ready.add(p);
			}
		}

		var ordered = new ArrayList<PendingPlugin>();

		while (!ready.isEmpty()) {
			var p = ready.poll();
			ordered.add(p);

			for (var dependent : dependents.get(p)) {
				if (inDegree.merge(dependent, -1, Integer::sum) == 0) {
					ready.add(dependent);
				}
			}
		}

		if (ordered.size() != PENDING.size()) {
			if (DevProperties.get().logErroringPlugins) {
				KubeJS.LOGGER.error("Detected a cycle in KubeJS plugin 'after' ordering, falling back to declaration order for the remaining plugins");
			}
			var orderedSet = new HashSet<>(ordered);

			for (var p : PENDING) {
				if (!orderedSet.contains(p)) {
					ordered.add(p);
				}
			}
		}

		for (var p : ordered) {
			try {
				LIST.add(p.pluginClass().getDeclaredConstructor().newInstance());
			} catch (Throwable t) {
				if (DevProperties.get().logErroringPlugins) {
					KubeJS.LOGGER.error("Failed to load plugin {} from source {}: {}", p.pluginClass().getName(), p.source(), t.toString());
				}
			}
		}

		PENDING.clear();
	}

	public static ClassFilter createClassFilter(ScriptType type) {
		var filter = new ClassFilter(type);
		forEachPlugin(filter, KubeJSPlugin::registerClasses);

		for (var s : GLOBAL_CLASS_FILTER) {
			if (s.length() >= 2) {
				if (s.startsWith("+")) {
					filter.allow(s.substring(1).trim());
				} else if (s.startsWith("-")) {
					filter.deny(s.substring(1).trim());
				}
			}
		}

		return filter;
	}

	public static void forEachPlugin(Consumer<KubeJSPlugin> callback) {
		LIST.forEach(callback);
	}

	public static <T> void forEachPlugin(T instance, BiConsumer<KubeJSPlugin, T> callback) {
		for (var item : LIST) {
			callback.accept(item, instance);
		}
	}

	public static List<KubeJSPlugin> getAll() {
		return Collections.unmodifiableList(LIST);
	}

	public static void addSidedBindings(BindingRegistry event) {
		BINDINGS.addBindings(event);
	}

	/// A plugin queued for instantiation, after required-mod and client-only checks have already passed.
	/// `id` is optional in case a mod does not provide it, but really should
	private record PendingPlugin(
		String source,
		Optional<String> id,
		Class<? extends KubeJSPlugin> pluginClass,
		List<String> after
	) {}

	private static final Codec<ClassFilterData> CLASS_FILTER_CODEC = RecordCodecBuilder.create(
		inst -> inst.group(
			Codec.STRING.listOf().optionalFieldOf("allow", List.of()).forGetter(ClassFilterData::allow),
			Codec.STRING.listOf().optionalFieldOf("deny", List.of()).forGetter(ClassFilterData::deny)
		).apply(inst, ClassFilterData::new)
	);

	private static final Codec<Optional<Class<? extends KubeJSPlugin>>> PLUGIN_CLASS_CODEC = Codec.STRING.comapFlatMap(str -> {
		try {
			var clazz = Class.forName(str, false, KubeJSPlugins.class.getClassLoader());

			try {
				return DataResult.success(Optional.of(clazz.asSubclass(KubeJSPlugin.class)));
			} catch (ClassCastException e) {
				return DataResult.error(() -> "Class " + str + " does not implement KubeJSPlugin");
			}
		} catch (ClassNotFoundException e) {
			return DataResult.success(Optional.empty());
		}
	}, clazz -> clazz.map(Class::getName).orElse(""));

	private static final Codec<PluginEntry> PLUGIN_ENTRY_CODEC = RecordCodecBuilder.create(
		inst -> inst.group(
			Codec.STRING.optionalFieldOf("id").forGetter(PluginEntry::id),
			PLUGIN_CLASS_CODEC.fieldOf("class").forGetter(PluginEntry::pluginClass),
			Codec.BOOL.optionalFieldOf("client_only", false).forGetter(PluginEntry::clientOnly),
			Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(PluginEntry::requiredMods),
			Codec.STRING.listOf().optionalFieldOf("after", List.of()).forGetter(PluginEntry::after)
		).apply(inst, PluginEntry::new)
	);

	private static final Codec<PluginFileData> PLUGIN_DATA_CODEC = RecordCodecBuilder.create(
		inst -> inst.group(
			PLUGIN_ENTRY_CODEC.listOf().fieldOf("plugins").forGetter(PluginFileData::plugins),
			CLASS_FILTER_CODEC.optionalFieldOf("class_filter").forGetter(PluginFileData::classFilter)
		).apply(inst, PluginFileData::new)
	);

	private record PluginFileData (
		List<PluginEntry> plugins,
		Optional<ClassFilterData> classFilter
	) {}

	private record PluginEntry (
		Optional<String> id,
		// If a mod is required and not loaded, having it as an optional
		// prevents the entire plugin list from failing, therefor an Optional of the class is used
		Optional<Class<? extends KubeJSPlugin>> pluginClass,
		boolean clientOnly,
		List<String> requiredMods,
		List<String> after
	) {}

	private record ClassFilterData (
		List<String> allow,
		List<String> deny
	) {}
}
