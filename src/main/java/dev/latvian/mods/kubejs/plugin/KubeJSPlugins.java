package dev.latvian.mods.kubejs.plugin;

import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ModResourceBindings;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Discovers, loads, and stores all [KubeJSPlugin] instances.
/// Use [#forEachPlugin] to iterate over all loaded plugins.
public class KubeJSPlugins {
	private static final List<KubeJSPlugin> LIST = new ArrayList<>();
	private static final List<String> GLOBAL_CLASS_FILTER = new ArrayList<>();
	private static final ModResourceBindings BINDINGS = new ModResourceBindings();

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
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load KubeJS plugin", ex);
		}
	}

	/// Given a mod file, checks if it has a class annotated with [KubeJSPluginEntrypoint], a file defining
	/// KubeJS plugins, class filter rules, or bindings, and tries to load them if they exist.
	///
	/// For plugin file syntax, see [#loadFromFile(java.util.stream.Stream, java.lang.String, boolean)],
	/// and for bindings, see [ModResourceBindings]. Class filter syntax simply consists have lines
	/// starting with either a '+' (allow) or '-' (deny) followed by a class or package name.
	private static void loadMod(String modId, IModFile mod, boolean loadClientPlugins) throws IOException {
		mod.getScanResult().getAnnotatedBy(KubeJSPluginEntrypoint.class, ElementType.TYPE).forEach(data -> {
			try {
				loadFromAnnotation(data, loadClientPlugins);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to load KubeJS plugin", ex);
			}
		});

		var contents = mod.getContents();

		var pluginData = contents.readFile("kubejs.plugins.txt");
		if (pluginData != null) {
			loadFromFile(new String(pluginData, StandardCharsets.UTF_8).lines(), modId, loadClientPlugins);
		}

		var filterData = contents.readFile("kubejs.classfilter.txt");
		if (filterData != null) {
			GLOBAL_CLASS_FILTER.addAll(new String(filterData, StandardCharsets.UTF_8).lines().toList());
		}

		BINDINGS.readBindings(modId, mod);
	}

	/// Tries to load KubeJS plugin based on the [KubeJSPluginEntrypoint] annotation.
	///
	/// The annotation must be placed on a class that implements [KubeJSPlugin].
	/// The `clientOnly` field determines whether the plugin should only be loaded on the client side.
	/// The `requiredMods` field specifies a list of mod ids that must be present for the plugin to be loaded.
	private static void loadFromAnnotation(ModFileScanData.AnnotationData data, boolean loadClientPlugins) {
		KubeJS.LOGGER.info("Found annotated plugin entrypoint {}", data.memberName());

		var clientOnly = (boolean) data.annotationData().getOrDefault("clientOnly", false);
		if (clientOnly && !loadClientPlugins) {
			if (DevProperties.get().logSkippedPlugins) {
				KubeJS.LOGGER.warn("Plugin " + data.memberName() + " does not load on server side, skipping");
			}
			return;
		}

		//noinspection unchecked
		var requiredMods = (List<String>) data.annotationData().getOrDefault("requiredMods", Collections.emptyList());
		for (var modId : requiredMods) {
			if (!ModList.get().isLoaded(modId)) {
				if (DevProperties.get().logSkippedPlugins) {
					KubeJS.LOGGER.warn("Plugin " + data.memberName() + " does not have required mod '" + modId + "' loaded, skipping");
				}
				return;
			}
		}

		Class<?> pluginClass;
		try {
			pluginClass = Class.forName(data.memberName());
		} catch (Throwable t) {
			KubeJS.LOGGER.error("Failed to load plugin {}: {}", data.memberName(), t);
			return;
		}

		if (!KubeJSPlugin.class.isAssignableFrom(pluginClass)) {
			KubeJS.LOGGER.error("Plugin {} does not implement KubeJSPlugin interface", data.memberName());
			return;
		}

		try {
			LIST.add((KubeJSPlugin) pluginClass.getDeclaredConstructor().newInstance());
		} catch (Throwable t) {
			KubeJS.LOGGER.error("Failed to init KubeJS plugin {}: {}", data.memberName(), t);
		}
	}

	/// Tries to load KubeJS plugins based on the contents of a `kubejs.plugins.txt` file.
	///
	/// A plugin definition consists of a FQCN referring to a class that implements [KubeJSPlugin],
	/// followed by an optional list of *mod ids* which are required for the plugin to be loaded.
	/// The string "client" may be used to ensure a plugin only loads on the client side.
	///
	/// Filters can be used to make sure that certain plugins only load if a mod is present.
	private static void loadFromFile(Stream<String> contents, String source, boolean loadClientPlugins) {
		KubeJS.LOGGER.info("Found plugin source {}", source);

		contents.map(s -> s.split("#", 2)[0].trim()) // allow comments (#)
			.filter(s -> !s.isBlank()) // filter empty lines
			.flatMap(s -> {
				String[] line = s.split(" ");

				for (int i = 1; i < line.length; i++) {
					if (line[i].equalsIgnoreCase("client")) {
						if (!loadClientPlugins) {
							if (DevProperties.get().logSkippedPlugins) {
								KubeJS.LOGGER.warn("Plugin " + line[0] + " does not load on server side, skipping");
							}

							return Stream.empty();
						}
					} else if (!ModList.get().isLoaded(line[i])) {
						if (DevProperties.get().logSkippedPlugins) {
							KubeJS.LOGGER.warn("Plugin " + line[0] + " does not have required mod '" + line[i] + "' loaded, skipping");
						}

						return Stream.empty();
					}
				}

				try {
					return Stream.of(Class.forName(line[0])); // try to load plugin class
				} catch (Throwable t) {
					KubeJS.LOGGER.error("Failed to load plugin {} from source {}: {}", s, source, t);
					t.printStackTrace();
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
}
