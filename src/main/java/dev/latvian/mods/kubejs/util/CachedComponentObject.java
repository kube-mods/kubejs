package dev.latvian.mods.kubejs.util;

import dev.latvian.mods.kubejs.component.DataComponentWrapper;
import dev.latvian.mods.kubejs.core.RegistryObjectKJS;
import dev.latvian.mods.kubejs.core.mixin.DataComponentPatchAccessorMixin;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Objects;
import java.util.UUID;

public record CachedComponentObject<T extends RegistryObjectKJS<T>, S>(UUID cacheKey, T value, S stack, DataComponentPatch components, Mutable<String> iconPath) {
	public static <T extends RegistryObjectKJS<T>> void writeCacheKey(FriendlyByteBuf buf, T value, DataComponentPatch components) {
		buf.writeUtf(value.kjs$getId());
		buf.writeVarInt(components.size());

		var map = ((DataComponentPatchAccessorMixin) (Object) components).getMap();

		for (var entry : map.reference2ObjectEntrySet()) {
			var key = Objects.requireNonNull(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey()));
			var mVal = entry.getValue();

			buf.writeUtf(key.getNamespace());
			buf.writeUtf(key.getPath());
			buf.writeBoolean(mVal != null);

			if (mVal != null) {
				buf.writeVarInt(mVal.hashCode());
			}
		}
	}

	public static <T extends RegistryObjectKJS<T>, S> CachedComponentObject<T, S> of(T value, S stack, DataComponentPatch components) {
		var buf = new FriendlyByteBuf(Unpooled.buffer());
		writeCacheKey(buf, value, components);
		return new CachedComponentObject<>(UUID.nameUUIDFromBytes(buf.array()), value, stack, components, new MutableObject<>());
	}

	public static CachedComponentObject<Item, ItemStack> ofItemStack(ItemStack stack, boolean visual) {
		return of(stack.getItem(), stack, visual ? DataComponentWrapper.visualPatch(stack.getComponentsPatch()) : stack.getComponentsPatch());
	}

	public static CachedComponentObject<Fluid, FluidStack> ofFluidStack(FluidStack stack, boolean visual) {
		return of(stack.getFluid(), stack, visual ? DataComponentWrapper.visualPatch(stack.getComponentsPatch()) : stack.getComponentsPatch());
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof CachedComponentObject<?, ?> c && value == c.value && components.equals(c.components);
	}
}
