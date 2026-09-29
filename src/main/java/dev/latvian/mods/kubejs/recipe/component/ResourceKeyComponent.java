package dev.latvian.mods.kubejs.recipe.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.codec.KubeJSCodecs;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.registry.RegistryType;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.OpsContainer;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public record ResourceKeyComponent<T>(@Nullable ResourceKey<RecipeComponentType<?>> typeOverride, ResourceKey<? extends Registry<T>> registryKey, Codec<ResourceKey<T>> codec, TypeInfo typeInfo) implements RecipeComponent<ResourceKey<T>> {
	private static <T> ResourceKeyComponent<T> create(@Nullable ResourceKey<RecipeComponentType<?>> typeOverride, ResourceKey<? extends Registry<T>> registryKey) {
		var reg = RegistryType.ofKey(registryKey);
		return new ResourceKeyComponent<>(typeOverride, registryKey, ResourceKey.codec(registryKey), reg == null ? TypeInfo.of(ResourceKey.class) : TypeInfo.of(ResourceKey.class).withParams(reg.type()));
	}

	public static final ResourceKeyComponent<Level> DIMENSION = create(RecipeComponentType.builtin("dimension_resource_key"), Registries.DIMENSION);
	public static final ResourceKeyComponent<LootTable> LOOT_TABLE = create(RecipeComponentType.builtin("loot_table_resource_key"), Registries.LOOT_TABLE);

	private static <T> ResourceKeyComponent<T> of(ResourceKey<?> key) {
		if (key == Registries.DIMENSION) {
			return Cast.to(DIMENSION);
		} else if (key == Registries.LOOT_TABLE) {
			return Cast.to(LOOT_TABLE);
		} else {
			return create(null, Cast.to(key));
		}
	}

	public static final ResourceKey<RecipeComponentType<?>> TYPE = RecipeComponentType.builtin("resource_key");
	public static final MapCodec<ResourceKeyComponent<?>> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		KubeJSCodecs.REGISTRY_KEY_CODEC.fieldOf("registry").forGetter(ResourceKeyComponent::registryKey)
	).apply(instance, ResourceKeyComponent::of));

	@Override
	public ResourceKey<RecipeComponentType<?>> type() {
		return typeOverride == null ? TYPE : typeOverride;
	}

	@Override
	public ResourceKey<T> wrap(RecipeScriptContext cx, @Nullable Object from) {
		return ResourceKey.create(registryKey, ID.mc(from));
	}

	@Override
	public String toString() {
		if (typeOverride != null) {
			return typeOverride.toString();
		} else {
			return "resource_key<" + ID.reduce(registryKey.identifier()) + ">";
		}
	}

	@Override
	public String toString(OpsContainer ops, ResourceKey<T> value) {
		return value.identifier().toString();
	}
}
