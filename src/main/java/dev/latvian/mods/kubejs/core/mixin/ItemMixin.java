package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.core.ItemKJS;
import dev.latvian.mods.kubejs.item.ItemBehavior;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = Item.class, priority = 1001)
@RemapPrefixForJS("kjs$")
public abstract class ItemMixin implements ItemKJS {
	@Shadow
	public abstract DataComponentMap components();

	@Shadow
	@Final
	private Holder.Reference<Item> builtInRegistryHolder;

	@Unique
	private @Nullable ItemBehavior kjs$behavior;

	@Unique
	private @Nullable Map<String, Object> kjs$typeData;

	@Unique
	private @Nullable ResourceKey<Item> kjs$registryKey;

	@Unique
	private @Nullable String kjs$id;

	@Override
	@Nullable
	public ItemBehavior kjs$getItemBehavior() {
		return kjs$behavior;
	}

	@Override
	public void kjs$setItemBehavior(ItemBehavior b) {
		kjs$behavior = b;
	}

	@Override
	public Holder.Reference<Item> kjs$asHolder() {
		return builtInRegistryHolder;
	}

	@Override
	public ResourceKey<Item> kjs$getKey() {
		return kjs$asHolder().key();
	}

	@Override
	public String kjs$getId() {
		if (kjs$id == null) {
			kjs$id = ItemKJS.super.kjs$getId();
		}

		return kjs$id;
	}

	@Override
	public Map<String, Object> kjs$getTypeData() {
		if (kjs$typeData == null) {
			kjs$typeData = new HashMap<>();
		}

		return kjs$typeData;
	}

	@Override
	@Accessor("craftingRemainingItem")
	@Mutable
	public abstract void kjs$setCraftingRemainder(ItemStackTemplate i);

	@Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
	private void isFoil(ItemStack itemStack, CallbackInfoReturnable<Boolean> ci) {
		if (kjs$behavior != null && kjs$behavior.glow) {
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "appendHoverText", at = @At("RETURN"))
	private void appendHoverText(
		ItemStack itemStack,
		Item.TooltipContext context,
		TooltipDisplay display,
		Consumer<Component> builder,
		TooltipFlag tooltipFlag,
		CallbackInfo ci
	) {
		if (kjs$behavior != null && !kjs$behavior.tooltip.isEmpty()) {
			for (var c : kjs$behavior.tooltip) {
				builder.accept(c);
			}
		}
	}

	@Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
	private void isBarVisible(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
		if (kjs$behavior != null && kjs$behavior.barWidth != null && kjs$behavior.barWidth.applyAsInt(stack) <= Item.MAX_BAR_WIDTH) {
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
	private void getBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> ci) {
		if (kjs$behavior != null && kjs$behavior.barWidth != null) {
			ci.setReturnValue(kjs$behavior.barWidth.applyAsInt(stack));
		}
	}

	@Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
	private void getBarColor(ItemStack stack, CallbackInfoReturnable<Integer> ci) {
		if (kjs$behavior != null && kjs$behavior.barColor != null) {
			ci.setReturnValue(kjs$behavior.barColor.apply(stack).kjs$getRGB());
		}
	}

	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void getUseDuration(ItemStack itemStack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
		if (kjs$behavior != null && kjs$behavior.useDuration != null) {
			cir.setReturnValue(kjs$behavior.useDuration.applyAsInt(itemStack, entity));
		}
	}

	@Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
	private void getUseAnimation(ItemStack itemStack, CallbackInfoReturnable<ItemUseAnimation> ci) {
		if (kjs$behavior != null && kjs$behavior.anim != null) {
			ci.setReturnValue(kjs$behavior.anim);
		}
	}

	@Inject(method = "getName", at = @At("HEAD"), cancellable = true)
	private void getName(ItemStack itemStack, CallbackInfoReturnable<Component> ci) {
		if (kjs$behavior != null && kjs$behavior.nameGetter != null) {
			ci.setReturnValue(kjs$behavior.nameGetter.apply(itemStack));
		}

		if (kjs$behavior != null && kjs$behavior.displayName != null && kjs$behavior.formattedDisplayName) {
			ci.setReturnValue(kjs$behavior.displayName);
		}
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (kjs$behavior != null && kjs$behavior.use != null) {
			if (kjs$behavior.use.use(level, player, hand)) {
				cir.setReturnValue(ItemUtils.startUsingInstantly(level, player, hand));
			} else {
				cir.setReturnValue(InteractionResult.FAIL);
			}
		}
	}

	@Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
	private void finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> ci) {
		if (kjs$behavior != null && kjs$behavior.finishUsing != null) {
			ci.setReturnValue(kjs$behavior.finishUsing.finishUsingItem(itemStack, level, livingEntity));
		}
	}

	@Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
	private void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
		if (kjs$behavior != null && kjs$behavior.releaseUsing != null) {
			kjs$behavior.releaseUsing.releaseUsing(itemStack, level, entity, remainingTime);
		}
	}

	@Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
	private void hurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker, CallbackInfo ci) {
		if (kjs$behavior != null && kjs$behavior.hurtEnemy != null) {
			kjs$behavior.hurtEnemy.test(new ItemBehavior.HurtEnemyContext(itemStack, mob, attacker));
		}
	}

	@Override
	public Ingredient kjs$asIngredient() {
		return Ingredient.of(kjs$self());
	}

	@Override
	@Accessor("descriptionId")
	@Mutable
	public abstract void kjs$setNameKey(String key);

	@Override
	@Accessor("canCombineRepair")
	@Mutable
	public abstract void kjs$setCanRepair(boolean repairable);
}
