package dev.latvian.mods.kubejs.core.component;

import dev.latvian.mods.kubejs.core.mixin.DataComponentPatchAccessorMixin;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

@RemapPrefixForJS("kjs$")
public interface DataComponentSetter {
	/// this method exists instead of non-null setters so people can use `set(x, null)` on the JS side
	///
	/// @implNote take care to hide any method named set with this signature, as it is likely not safe for null values!
	<T> void kjs$override(DataComponentType<T> type, @Nullable T value);

	@HideFromJS
	default <T> void kjs$setTyped(TypedDataComponent<T> component) {
		kjs$override(component.type(), component.value());
	}

	default void kjs$remove(DataComponentType<?> type) {
		kjs$override(Cast.to(type), null);
	}

	default void kjs$setUnit(DataComponentType<Unit> component) {
		kjs$override(component, Unit.INSTANCE);
	}

	default void kjs$set(DataComponentMap components) {
		components.forEach(this::kjs$setTyped);
	}

	default void kjs$patch(DataComponentPatch components) {
		var map = ((DataComponentPatchAccessorMixin) (Object) components).getMap();

		for (var entry : map.entrySet()) {
			DataComponentType<?> key = entry.getKey();
			Object value = entry.getValue();

			kjs$override(Cast.to(key), Cast.to(value));
		}
	}
}
