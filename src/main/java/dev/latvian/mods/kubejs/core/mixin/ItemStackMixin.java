package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.core.ItemStackKJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ItemStack.class)
@RemapPrefixForJS("kjs$")
public abstract class ItemStackMixin implements ItemStackKJS {

	@Shadow
	@Final
	private PatchedDataComponentMap components;

	@Shadow
	@HideFromJS
	public abstract void enchant(Holder<Enchantment> enchantment, int level);

	@Shadow
	@HideFromJS
	public abstract ItemEnchantments getEnchantments();

	@Shadow
	@HideFromJS
	public abstract Holder<Item> typeHolder();

	@Override
	public void kjs$resetComponents(Context cx) {
		components.restorePatch(DataComponentPatch.EMPTY);
	}

	@Inject(method = "consume", at = @At("HEAD"))
	private void kjs$onConsume(int amount, @Nullable LivingEntity owner, CallbackInfo ci) {
		if (amount <= 0 || owner == null) {
			return;
		}
		ItemStack stack = (ItemStack) (Object) this;
		if (!stack.has(DataComponents.FOOD) || !stack.has(DataComponents.CONSUMABLE)) {
			return;
		}
		if (owner instanceof Player player) {
			player.kjs$foodEaten(stack, Objects.requireNonNull(stack.get(DataComponents.FOOD)));
		}
	}
}
