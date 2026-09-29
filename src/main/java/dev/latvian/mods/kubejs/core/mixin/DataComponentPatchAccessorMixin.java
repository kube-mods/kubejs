package dev.latvian.mods.kubejs.core.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DataComponentPatch.class)
public interface DataComponentPatchAccessorMixin {
	@Accessor("map")
	Reference2ObjectMap<DataComponentType<?>, Object> getMap();
}
