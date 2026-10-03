package dev.latvian.mods.kubejs.util;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;

/// Wraps a [RegistryAccessContainer] so that it can be used as a NeoForge [ICondition.IContext].
///
/// [RegistryAccessContainer] itself can't implement [ICondition.IContext] alongside [RegistryAccess],
/// because both interfaces declare a `registries()` method with unrelated return types
/// ([HolderGetter.Provider] for [ICondition.IContext], `Stream<RegistryAccess.RegistryEntry<?>>`
/// for [RegistryAccess]).
public final class RegistryContextContainer implements ICondition.IContext {
	private final RegistryAccessContainer registries;

	public RegistryContextContainer(RegistryAccessContainer registries) {
		this.registries = registries;
	}

	public RegistryAccessContainer getRegistries() {
		return registries;
	}

	@Override
	public HolderGetter.Provider registries() {
		return registries;
	}

	@Override
	public <T> boolean isTagLoaded(TagKey<T> key) {
		return registries.isTagLoaded(key);
	}
}
