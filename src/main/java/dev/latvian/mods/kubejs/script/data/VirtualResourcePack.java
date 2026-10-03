package dev.latvian.mods.kubejs.script.data;

import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.generator.KubeResourceGenerator;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.TextIcons;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackMetadataResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class VirtualResourcePack extends AbstractPackMetadataResources implements KubeResourceGenerator, ExportablePackResources {
	public final ScriptType scriptType;
	public final PackType packType;
	public final GeneratedDataStage stage;
	public final Supplier<RegistryAccessContainer> registries;
	public final String info;
	public final Component component;
	private final Map<Identifier, GeneratedData> locationToData;
	private final Map<String, GeneratedData> pathToData;
	private final Set<String> namespaces;

	public VirtualResourcePack(ScriptType scriptType, PackType packType, GeneratedDataStage stage, Supplier<RegistryAccessContainer> registries) {
		super(KubeFileResourcePack.PACK_LOCATION_INFO);
		this.scriptType = scriptType;
		this.packType = packType;
		this.stage = stage;
		this.registries = registries;
		this.info = stage.displayName + ", " + packType.getDirectory();
		this.component = Component.empty().append(TextIcons.NAME).append(" (" + info + ", )");

		this.locationToData = new HashMap<>();
		this.pathToData = new HashMap<>();
		this.namespaces = new HashSet<>();
	}

	public void reset() {
		locationToData.clear();
		pathToData.clear();
		namespaces.clear();
	}

	@Override
	public RegistryAccessContainer getRegistries() {
		return registries.get();
	}

	@Override
	public void add(GeneratedData data) {
		locationToData.put(data.id(), data);
		pathToData.put(packType.getDirectory() + "/" + data.id().getNamespace() + "/" + data.id().getPath(), data);
		namespaces.add(data.id().getNamespace());

		if (DevProperties.get().virtualPackOutput) {
			scriptType.console.info("Registered virtual file [" + info + "] '" + data.id() + "': " + data);
		}
	}

	@Override
	@Nullable
	public GeneratedData getGenerated(Identifier id) {
		return locationToData.get(id);
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... path) {
		return switch (path.length == 1 ? path[0] : "") {
			case PACK_META -> GeneratedData.PACK_META;
			case "pack.png" -> GeneratedData.PACK_ICON;
			default -> null;
		};
	}

	@Override
	@Nullable
	public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
		if (type != packType) {
			return null;
		}

		var s = locationToData.get(location);

		if (s != null) {
			if (DevProperties.get().virtualPackOutput) {
				scriptType.console.info("Served virtual file [" + info + "] '" + location + "': " + s);
			}

			return s;
		}

		return null;
	}

	@Override
	public void listResources(PackType packType, String namespace, String path, ResourceOutput visitor) {
		if (!path.endsWith("/")) {
			path = path + "/";
		}

		for (Identifier r : locationToData.keySet()) {
			if (r.getNamespace().equals(namespace) && r.getPath().startsWith(path)) {
				visitor.accept(r, getResource(packType, r));
			}
		}
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return Set.copyOf(namespaces);
	}


	@Override
	public <T> @Nullable T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException {
		return super.getMetadataSection(metadataSerializer);
	}

	@Override
	public String toString() {
		return packId();
	}

	@Override
	public String packId() {
		return "KubeJS Virtual Resource Pack [" + info + "]";
	}

	@Override
	public String exportPath() {
		return packType.getDirectory() + '/' + stage.name;
	}

	@Override
	public void export(Path root) throws IOException {
		for (var file : pathToData.entrySet()) {
			var path = root.resolve(file.getKey());
			var parent = path.getParent();

			if (Files.notExists(parent)) {
				Files.createDirectories(parent);
			}

			Files.write(path, file.getValue().data().get());
		}
	}

	@Override
	public void close() {
		if (!FMLLoader.getCurrent().isProduction()) {
			KubeJS.LOGGER.info("Closed {}", packId());
		}
	}
}
