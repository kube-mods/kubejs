package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.latvian.mods.kubejs.block.BlockStartedFallingKubeEvent;
import dev.latvian.mods.kubejs.block.BlockStoppedFallingKubeEvent;
import dev.latvian.mods.kubejs.plugin.builtin.event.BlockEvents;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
	@Shadow
	private BlockState blockState;

	@Inject(method = "fall", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", shift = At.Shift.BEFORE))
	private static void kjs$fallStart(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir, @Local(name = "entity") FallingBlockEntity entity) {
		if (!level.isClientSide() && BlockEvents.STARTED_FALLING.hasListeners(state.kjs$getKey())) {
			if (BlockEvents.STARTED_FALLING.post(ScriptType.SERVER, state.kjs$getKey(), new BlockStartedFallingKubeEvent(level, pos, state, entity)).interruptFalse()) {
				cir.setReturnValue(entity);
			}
		}
	}

	@Unique
	private double kjs$lastFallSpeed;

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;applyGravity()V", shift = At.Shift.BEFORE))
	private void kjs$captureFallSpeed(CallbackInfo ci) {
		kjs$lastFallSpeed = Math.abs(((FallingBlockEntity) (Object) this).getDeltaMovement().y());
	}

	@Inject(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ChunkMap;sendToTrackingPlayers(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/Packet;)V",
			shift = At.Shift.BEFORE
		)
	)
	private void kjs$fallEnd(CallbackInfo ci, @Local(name = "pos") BlockPos pos, @Local(name = "currentState") BlockState replacedState) {
		var entity = (FallingBlockEntity) (Object) this;
		if (!entity.level().isClientSide() && BlockEvents.STOPPED_FALLING.hasListeners(blockState.kjs$getKey())) {
			BlockEvents.STOPPED_FALLING.post(ScriptType.SERVER, blockState.kjs$getKey(), new BlockStoppedFallingKubeEvent(entity.level(), pos, blockState, entity, kjs$lastFallSpeed, replacedState));
		}
	}

	@Unique
	@RemapForJS("setBlockState")
	public void kjs$setBlockState(BlockState state) {
		this.blockState = state;
	}
}
