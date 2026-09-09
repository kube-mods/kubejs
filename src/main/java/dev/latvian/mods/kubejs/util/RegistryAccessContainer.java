package dev.latvian.mods.kubejs.util;

import com.google.gson.JsonArray;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.RegistryWrapper;
import dev.latvian.mods.kubejs.recipe.CachedItemTagLookup;
import dev.latvian.mods.kubejs.recipe.CachedTagLookup;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.KubeJSContext;
import dev.latvian.mods.kubejs.server.DataExport;
import dev.latvian.mods.rhino.Context;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class RegistryAccessContainer extends RegistryOpsContainer implements RegistryAccess, ICondition.IContext {
	public static final RegistryAccessContainer BUILTIN = new RegistryAccessContainer(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

	// Still necessary because STARTUP and CLIENT scripts need to know about registries
	@ApiStatus.Internal
	public static RegistryAccessContainer current = BUILTIN;

	public static RegistryAccessContainer of(Context cx) {
		return cx instanceof KubeJSContext kcx ? kcx.getRegistries() : current;
	}

	private final RegistryAccess.Frozen access;
	private @Nullable DamageSources damageSources;
	private final Map<String, ItemStack> itemStackParseCache;
	public final Map<ResourceKey<?>, CachedTagLookup.Entry<?>> cachedRegistryTags;
	public @Nullable CachedItemTagLookup cachedItemTags;
	public @Nullable CachedTagLookup<Block> cachedBlockTags;
	public @Nullable CachedTagLookup<Fluid> cachedFluidTags;
	private final Map<Identifier, RegistryWrapper> cachedRegistryWrappers = new HashMap<>();

	public RegistryAccessContainer(RegistryAccess.Frozen access) {
		super(
			access.createSerializationContext(NbtOps.INSTANCE),
			access.createSerializationContext(JsonOps.INSTANCE),
			access.createSerializationContext(JavaOps.INSTANCE)
		);

		this.access = access;
		this.damageSources = null;
		this.itemStackParseCache = new HashMap<>();
		this.cachedRegistryTags = new Reference2ObjectOpenHashMap<>();
	}

	public Registry<Item> item() {
		return lookupOrThrow(Registries.ITEM);
	}

	public Registry<Block> block() {
		return lookupOrThrow(Registries.BLOCK);
	}

	public DamageSources damageSources() {
		if (damageSources == null) {
			damageSources = new DamageSources(access);
		}

		return damageSources;
	}

	public Map<String, ItemStack> itemStackParseCache() {
		return itemStackParseCache;
	}

	// Currently this is the best way I can think of to have tags available at the time of recipe loading
	@SuppressWarnings({"rawtypes", "unchecked"})
	public synchronized <T> void cacheTags(Registry<T> registry, Map<Identifier, List<TagLoader.EntryWithSource>> map) {
		ResourceKey key1 = registry.key();

		try {
			if (key1 == Registries.ITEM) {
				cachedItemTags = Cast.to(new CachedItemTagLookup(Cast.to(registry), map));
				cachedRegistryTags.put(key1, new CachedTagLookup.Entry(key1, registry, cachedItemTags));
			} else if (key1 == Registries.BLOCK) {
				cachedBlockTags = Cast.to(new CachedTagLookup<>(registry, map));
				cachedRegistryTags.put(key1, new CachedTagLookup.Entry(key1, registry, cachedBlockTags));
			} else if (key1 == Registries.FLUID) {
				cachedFluidTags = Cast.to(new CachedTagLookup<>(registry, map));
				cachedRegistryTags.put(key1, new CachedTagLookup.Entry(key1, registry, cachedFluidTags));
			} else {
				cachedRegistryTags.put(key1, new CachedTagLookup.Entry(key1, registry, new CachedTagLookup<>(registry, map)));
			}
		} catch (Exception ex) {
			ConsoleJS.SERVER.error("Error caching tags for " + key1, ex);
		}

		if (DataExport.export != null) {
			var loc = "tags/" + key1.identifier() + "/";

			for (var entry : map.entrySet()) {
				var list = new ArrayList<String>();

				for (var e : entry.getValue()) {
					list.add(e.entry().toString());
				}

				list.sort(String.CASE_INSENSITIVE_ORDER);
				var arr = new JsonArray();

				for (var e : list) {
					arr.add(e);
				}

				DataExport.export.addJson(loc + entry.getKey() + ".json", arr);
			}
		}
	}

	private <T> RegistryWrapper<T> createRegistryWrapper(Identifier id) {
		var key = ResourceKey.<T>createRegistryKey(id);
		return new RegistryWrapper<>(access.lookup(key).orElseThrow(() -> new KubeRuntimeException("Unknown registry: " + id)), ResourceKey.create(key, ID.UNKNOWN));
	}

	public RegistryWrapper<?> wrapRegistry(Identifier id) {
		return cachedRegistryWrappers.computeIfAbsent(id, this::createRegistryWrapper);
	}

	@Override
	public <T> boolean isTagLoaded(TagKey<T> key) {
		var cached = cachedRegistryTags.get(key.registry());
		return cached != null && cached.lookup().tagMap().containsKey(key.location());
	}

	// TODO: move to the ContextAwareReloadListener lookup somehow??
	@Override
	public RegistryAccess.Frozen registryAccess() {
		return access;
	}

	@Override
	public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
		return access.lookup(registryKey);
	}

	@Override
	public Stream<RegistryEntry<?>> registries() {
		return access.registries();
	}
}
