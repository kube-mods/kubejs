package dev.latvian.mods.kubejs.recipe.component;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.codec.KubeJSCodecs;
import dev.latvian.mods.kubejs.fluid.FluidWrapper;
import dev.latvian.mods.kubejs.holder.HolderWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.registry.RegistryType;
import dev.latvian.mods.kubejs.script.KubeJSContext;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

public record RegistryComponent<T>(
	ResourceKey<? extends Registry<T>> registry,
	@Nullable RegistryType<T> regType,
	Codec<Holder<T>> codec,
	TypeInfo typeInfo
) implements RecipeComponent<Holder<T>> {
	public static final ResourceKey<RecipeComponentType<?>> TYPE = RecipeComponentType.builtin("registry_element");

	public static final MapCodec<RegistryComponent<?>> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		KubeJSCodecs.REGISTRY_KEY_CODEC.fieldOf("registry").forGetter(RegistryComponent::registry)
	).apply(instance, RegistryComponent::createUnchecked));

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <T> RegistryComponent<T> createUnchecked(ResourceKey<?> resourceKey) {
		return new RegistryComponent(resourceKey);
	}

	public RegistryComponent(@Nullable RegistryType<T> regType, ResourceKey<? extends Registry<T>> registry) {
		this(registry, regType, RegistryFixedCodec.create(registry), regType == null || regType.type() == TypeInfo.STRING ? TypeInfo.STRING : TypeInfo.STRING.or(regType.type()));
	}

	public RegistryComponent(ResourceKey<? extends Registry<T>> key) {
		this(RegistryType.ofKey(key), key);
	}

	@Override
	public ResourceKey<RecipeComponentType<?>> type() {
		return TYPE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Holder<T> wrap(RecipeScriptContext cx, @Nullable Object from) {
		var lookup = cx.registries().lookupOrThrow(registry);

		if (registry.equals(Registries.ITEM)) {
			if (from instanceof ItemStack is) {
				return (Holder<T>) is.typeHolder();
			} else if (from instanceof Item item) {
				return (Holder<T>) item.builtInRegistryHolder();
			} else {
				return (Holder<T>) ItemWrapper.wrap(cx.cx(), from).typeHolder();
			}
		} else if (registry.equals(Registries.FLUID)) {
			if (from instanceof FluidStack fs) {
				return (Holder<T>) fs.typeHolder();
			} else if (from instanceof Fluid fluid) {
				return (Holder<T>) fluid.builtInRegistryHolder();
			} else {
				return (Holder<T>) FluidWrapper.wrap(cx.cx(), from).typeHolder();
			}
		} else if (regType != null) {
			return (Holder<T>) HolderWrapper.wrap((KubeJSContext) cx.cx(), from, regType.type());
		} else if (from instanceof ResourceKey<?> key) {
			if (key.isFor(registry)) {
				return lookup.get((ResourceKey<T>) key).orElseThrow(() ->
					new IllegalStateException("Missing element in %s: %s".formatted(registry, key))
				);
			} else {
				throw new IllegalStateException("Key %s is not valid for registry %s!".formatted(key, registry));
			}
		} else if (from instanceof CharSequence || from instanceof Identifier) {
			var rk = ResourceKey.create(registry, ID.mc(from.toString()));
			return lookup.get(rk).orElseThrow(() ->
				new IllegalStateException("Missing element in %s: %s".formatted(registry, rk))
			);
		} else {
			throw new IllegalStateException("Missing element in %s: %s".formatted(registry, from));
		}
	}

	@Override
	public boolean hasPriority(RecipeMatchContext cx, @Nullable Object from) {
		return (regType != null && regType.baseClass().isInstance(from))
			|| (ID.isValidKey(from))
			|| (from instanceof JsonPrimitive json && json.isString() && Identifier.tryParse(json.getAsString()) != null);
	}

	@Override
	public void buildUniqueId(UniqueIdBuilder builder, Holder<T> value) {
		var id = value.key();

		if (id != null) {
			builder.append(id.identifier());
		}
	}

	@Override
	public String toString() {
		return "registry_element<%s>".formatted(ID.reduce(registry.identifier()));
	}
}
