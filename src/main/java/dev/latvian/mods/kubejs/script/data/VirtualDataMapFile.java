package dev.latvian.mods.kubejs.script.data;

import com.mojang.datafixers.util.Either;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.registries.datamaps.DataMapEntry;
import net.neoforged.neoforge.registries.datamaps.DataMapFile;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/// Mutable builder for a single NeoForge [DataMapFile], for use in [VirtualDataPack]s.
/// Files are built using [#toFile()] when the virtual data pack is built.
///
/// @param <RT> the type of the registry this data map is attached to
/// @param <DT> the type of the attached data
/// @see KubeDataGenerator#dataMap
///
public class VirtualDataMapFile<RT, DT> implements BiConsumer<Identifier, DT> {
	public final KubeDataGenerator pack;
	public final RegistryAccessContainer registryAccess;
	public final Registry<RT> registry;

	private boolean replace = false;

	private final Map<TagKey<RT>, DataMapEntry<DT>> tagAdditions = new LinkedHashMap<>();
	private final Set<TagKey<RT>> tagRemovals = new LinkedHashSet<>();

	private final Map<Holder<RT>, DataMapEntry<DT>> additions = new LinkedHashMap<>();
	private final Set<Holder<RT>> removals = new LinkedHashSet<>();

	public VirtualDataMapFile(DataMapType<RT, DT> type, VirtualDataPack pack) {
		this.pack = pack;
		this.registryAccess = pack.getRegistries();
		this.registry = registryAccess.registryAccess().lookupOrThrow(type.registryKey());
	}

	/// Causes this data map file to replace all other existing entries rather than merge with them.
	public void replaceAll() {
		replace = true;
	}

	public void add(HolderSet<RT> holders, DT value) {
		add(holders, value, false);
	}

	public void add(HolderSet<RT> holders, DT value, boolean replace) {
		var tag = holders.unwrapKey();
		if (tag.isPresent()) {
			addTag(tag.get(), value, replace);
		} else {
			for (var holder : holders) {
				add(holder, value, replace);
			}
		}
	}

	public void remove(HolderSet<RT> holders) {
		holders.unwrap()
			.ifLeft(this::removeTag)
			.ifRight(values -> values.forEach(this::remove));
	}

	public void add(Holder<RT> holder, DT value) {
		add(holder, value, false);
	}

	public void add(Holder<RT> holder, DT value, boolean replace) {
		additions.put(holder, new DataMapEntry<>(value, replace));
	}

	public void remove(Holder<RT> holder) {
		removals.add(holder);
	}

	public void add(RT holder, DT value) {
		add(holder, value, false);
	}

	public void add(RT holder, DT value, boolean replace) {
		additions.put(registry.wrapAsHolder(holder), new DataMapEntry<>(value, replace));
	}

	public void remove(RT holder) {
		removals.add(registry.wrapAsHolder(holder));
	}

	public void addTag(TagKey<RT> tag, DT value) {
		addTag(tag, value, false);
	}

	public void addTag(TagKey<RT> tag, DT value, boolean replace) {
		tagAdditions.put(tag, new DataMapEntry<>(value, replace));
	}

	public void removeTag(TagKey<RT> tag) {
		tagRemovals.add(tag);
	}

	public void clear() {
		tagAdditions.clear();
		tagRemovals.clear();
		additions.clear();
		removals.clear();
	}

	private void buildValues(Map<Either<TagKey<RT>, ResourceKey<RT>>, Optional<WithConditions<DataMapEntry<DT>>>> map) {
		for (var tagEntry : tagAdditions.entrySet()) {
			TagKey<RT> key = tagEntry.getKey();
			DataMapEntry<DT> entry = tagEntry.getValue();
			map.put(Either.left(key), Optional.of(new WithConditions<>(List.of(), entry)));
		}

		for (var holderEntry : additions.entrySet()) {
			Holder<RT> holder = holderEntry.getKey();
			DataMapEntry<DT> entry = holderEntry.getValue();
			map.put(Either.right(holder.key()), Optional.of(new WithConditions<>(List.of(), entry)));
		}
	}

	private void buildRemovals(ArrayList<DataMapEntry.Removal<DT, RT>> list) {
		for (var key : tagRemovals) {
			list.add(new DataMapEntry.Removal<>(Either.left(key), Optional.empty()));
		}

		for (var removal : removals) {
			list.add(new DataMapEntry.Removal<>(Either.right(removal.key()), Optional.empty()));
		}
	}

	/// builds the real data
	DataMapFile<DT, RT> toFile() {
		return new DataMapFile<>(
			replace,
			Util.make(new HashMap<>(), this::buildValues),
			Util.make(new ArrayList<>(), this::buildRemovals)
		);
	}

	@Override
	@HideFromJS
	public void accept(Identifier id, DT data) {
		add(registry.getOrThrow(ResourceKey.create(registry.key(), id)), data);
	}
}
