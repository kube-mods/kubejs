package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.util.RegistryContextContainer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TagEmptyCondition.class)
public abstract class TagEmptyConditionMixin {
	@Shadow
	@Final
	private TagKey<Item> tag;

	@Inject(method = "test", at = @At("HEAD"), cancellable = true, remap = false)
	private void kjs$test(ICondition.IContext ctx, CallbackInfoReturnable<Boolean> cir) {
		if (ctx instanceof RegistryContextContainer c && c.getRegistries().cachedItemTags != null) {
			cir.setReturnValue(c.getRegistries().cachedItemTags.isEmpty(tag));
		}
	}
}