package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.core.EntityKJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(Entity.class)
@RemapPrefixForJS("kjs$")
public abstract class EntityMixin implements EntityKJS {
	@Shadow
	private Level level;

	@Shadow
	public abstract void playerTouch(Player player);

	@Unique
	private @Nullable CompoundTag kjs$persistentData;

	@Override
	public CompoundTag kjs$getPersistentData() {
		if (kjs$persistentData == null) {
			kjs$persistentData = new CompoundTag();
		}

		return kjs$persistentData;
	}

	@Shadow(remap = false)
	@RemapForJS("getForgePersistentData")
	public abstract CompoundTag getPersistentData();

	@Inject(method = "saveWithoutId", at = @At("RETURN"))
	private void saveKJS(ValueOutput output, CallbackInfo ci) {
		if (kjs$persistentData != null && !kjs$persistentData.isEmpty()) {
			output.store("KubeJSPersistentData", CompoundTag.CODEC, kjs$persistentData);
		}
	}

	@Inject(method = "load", at = @At("RETURN"))
	private void loadKJS(ValueInput input, CallbackInfo ci) {
		kjs$persistentData = input
			.read("KubeJSPersistentData", CompoundTag.CODEC)
			.orElse(null);
	}

	@Override
	@Nullable
	@HideFromJS
	public CompoundTag kjs$getRawPersistentData() {
		return kjs$persistentData;
	}

	@Override
	@HideFromJS
	public void kjs$setRawPersistentData(@Nullable CompoundTag tag) {
		kjs$persistentData = tag;
	}

	@Shadow
	@RemapForJS("tickCount")
	public int tickCount;

	@Shadow
	@RemapForJS("getUuid")
	public abstract UUID getUUID();

	@Shadow
	@RemapForJS("getStringUuid")
	public abstract String getStringUUID();

	@Shadow
	@RemapForJS("isGlowing")
	public abstract boolean isCurrentlyGlowing();

	@Shadow
	@RemapForJS("setGlowing")
	public abstract void setGlowingTag(boolean glowing);

	@Shadow
	@RemapForJS("getYaw")
	public abstract float getYRot();

	@Shadow
	@RemapForJS("setYaw")
	public abstract void setYRot(float yaw);

	@Shadow
	@RemapForJS("getPitch")
	public abstract float getXRot();

	@Shadow
	@RemapForJS("setPitch")
	public abstract void setXRot(float pitch);

	@Shadow
	@RemapForJS("setBodyYaw")
	@Info("Sets the entity's body yaw.")
	public abstract void setYBodyRot(float yBodyRot);

	@Shadow
	@RemapForJS("getBodyYaw")
	@Info("Gets the entity's body yaw (if the entity is a `LivingEntity`), or the entity's visual rotation (if the entity is an item entity or an item frame).")
	public abstract float getVisualRotationYInDegrees();

	@Shadow
	@RemapForJS("setMotion")
	public abstract void setDeltaMovement(double x, double y, double z);

	@Shadow
	public abstract void setRot(float yaw, float pitch);

	@Shadow
	@RemapForJS("addMotion")
	public abstract void push(double x, double y, double z);

	/// Replaced in JS by [EntityKJS#kjs$getPassengers()].
	@Shadow
	@HideFromJS
	public abstract List<Entity> getPassengers();

	@Shadow
	@RemapForJS("isOnSameTeam")
	public abstract boolean isAlliedTo(Entity e);

	/// KubeJS adds [EntityKJS#kjs$getFacing()] which also can specify whether the entity is looking down or up.
	@Shadow
	@RemapForJS("getHorizontalFacing")
	public abstract Direction getDirection();

	@Shadow
	@RemapForJS("extinguish")
	public abstract void extinguishFire();

	/// Replaced in JS by [EntityKJS#kjs$damage]
	@Shadow
	@HideFromJS
	public abstract void hurt(DamageSource source, float damage);

	// The remaps:
	// distanceToSqr(double x, double y, double z) - remains as is
	// distanceToSqr(Vec3 vector) - remains as is
	// distanceToSqr(Entity entity) - remapped to distanceToEntitySqr
	// distanceToBlockSqr(BlockPos block) is added by EntityKJS

	/// [EntityKJS#kjs$getType()] returns a string in JS.
	@Shadow
	@RemapForJS("getEntityType")
	public abstract EntityType<?> getType();

	@Shadow
	@Info("Measures the **square** of a distance of entity to the point at specified 3D position vector.")
	public abstract double distanceToSqr(Vec3 vec);

	@Shadow
	@Info("Measures the distance of entity to the point at specified `x`, `y` and `z`.")
	public abstract double distanceToSqr(double x, double y, double z);

	@Shadow
	@RemapForJS("distanceToEntitySqr")
	@Info("Measures the **square** of a distance of entity to another entity.")
	public abstract double distanceToSqr(Entity arg);

	@Shadow
	@RemapForJS("distanceToEntity")
	@Info("Measures the distance of entity to another entity.")
	public abstract float distanceTo(Entity arg);

	/// Replaced in JS by [EntityKJS#kjs$getLevel()].
	@Shadow
	@HideFromJS
	public abstract Level level();

	@Shadow
	@HideFromJS
	public abstract @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack itemStack);

	@Shadow
	@HideFromJS
	public abstract @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack itemStack, Vec3 offset);

	@Shadow
	@HideFromJS
	public abstract ItemEntity spawnAtLocation(ServerLevel level, ItemLike resource);

	@Shadow
	@HideFromJS
	public abstract ItemEntity spawnAtLocation(ServerLevel level, ItemStack itemStack, float offset);

	/// Disambiguates [Entity#snapTo(Vec3, float, float)] in JS.
	@Shadow
	@RemapForJS("snapToBlockPos")
	public abstract void snapTo(BlockPos spawnPos, float yRot, float xRot);
}
