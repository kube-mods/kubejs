package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.block.callback.RandomTickCallback;
import dev.latvian.mods.kubejs.core.BlockBehaviourKJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(BlockBehaviour.class)
@RemapPrefixForJS("kjs$")
public abstract class BlockBehaviourMixin implements BlockBehaviourKJS {
	@Unique
	@Nullable
	private Consumer<RandomTickCallback> kjs$randomTickCallback;

	@Override
	public void kjs$setRandomTickCallback(Consumer<RandomTickCallback> callback) {
		kjs$setIsRandomlyTicking(true);
		this.kjs$randomTickCallback = callback;
	}

	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (kjs$randomTickCallback != null) {
			kjs$randomTickCallback.accept(new RandomTickCallback(level.kjs$getBlock(pos).cache(state), random));
			ci.cancel();
		}
	}

	@Override
	@Accessor("hasCollision")
	@Mutable
	public abstract void kjs$setHasCollision(boolean v);

	@Override
	@Accessor("explosionResistance")
	@Mutable
	public abstract void kjs$setExplosionResistance(float v);

	@Override
	@Accessor("isRandomlyTicking")
	@Mutable
	public abstract void kjs$setIsRandomlyTicking(boolean v);

	@Override
	@Accessor("soundType")
	@Mutable
	public abstract void kjs$setSoundType(SoundType v);

	@Override
	@Accessor("friction")
	@Mutable
	public abstract void kjs$setFriction(float v);

	@Override
	@Accessor("speedFactor")
	@Mutable
	public abstract void kjs$setSpeedFactor(float v);

	@Override
	@Accessor("jumpFactor")
	@Mutable
	public abstract void kjs$setJumpFactor(float v);

	@Override
	@Accessor("descriptionId")
	@Mutable
	public abstract void kjs$setNameKey(String key);
}
