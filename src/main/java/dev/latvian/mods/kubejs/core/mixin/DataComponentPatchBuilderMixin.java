package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.core.component.DataComponentAccessor;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.util.HideFromJS;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(DataComponentPatch.Builder.class)
public abstract class DataComponentPatchBuilderMixin implements DataComponentAccessor {
	@Shadow
	public abstract DataComponentPatch build();

	@Shadow
	@Final
	private Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

	@Shadow
	@HideFromJS // replaced by kjs$set, which accepts a nullable value
	public abstract <T> DataComponentPatch.Builder set(DataComponentType<T> type, T value);

	@Override
	public <T> @Nullable T get(DataComponentType<? extends T> type) {
		return map.get(type).map(Cast::<T>to).orElse(null);
	}

	@Override
	public <T> void kjs$override(DataComponentType<T> type, @Nullable T value) {
		if (value == null) {
			kjs$remove(type);
		} else {
			set(type, value);
		}
	}

	@Override
	public void kjs$remove(DataComponentType<?> type) {
		map.remove(type);
	}
}
