package dev.latvian.mods.kubejs.fluid;

import dev.latvian.mods.kubejs.block.BlockRenderType;
import dev.latvian.mods.kubejs.color.KubeColor;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

@ReturnsSelf
public class FluidTypeBuilder extends BuilderBase<FluidType> {
	public static class KubeFluidType extends FluidType {
		public final FluidTypeBuilder builder;

		public KubeFluidType(FluidTypeBuilder builder) {
			super(builder.properties);
			this.builder = builder;
		}
	}

	public transient FluidType.Properties properties;
	public transient ResourceLocation stillTexture;
	public transient ResourceLocation flowingTexture;
	public transient ResourceLocation actualStillTexture;
	public transient ResourceLocation actualFlowingTexture;
	public transient ResourceLocation screenOverlayTexture;
	public transient ResourceLocation blockOverlayTexture;
	public transient KubeColor tint;
	public transient BlockRenderType renderType;

	public FluidTypeBuilder(ResourceLocation id) {
		super(id);
		this.properties = FluidType.Properties.create();
		this.stillTexture = newID("block/", "_still");
		this.flowingTexture = newID("block/", "_flow");
		this.actualStillTexture = newID("block/generated/", "_still");
		this.actualFlowingTexture = newID("block/generated/", "_flow");
		this.tint = null;
		this.renderType = BlockRenderType.SOLID;

		sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL);
		sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
		sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH);
	}

	@Override
	public FluidType createObject() {
		return new KubeFluidType(this);
	}

	public FluidTypeBuilder stillTexture(ResourceLocation stillTexture) {
		this.stillTexture = stillTexture;
		return this;
	}

	public FluidTypeBuilder flowingTexture(ResourceLocation flowingTexture) {
		this.flowingTexture = flowingTexture;
		return this;
	}

	public FluidTypeBuilder screenOverlayTexture(ResourceLocation screenOverlayTexture) {
		this.screenOverlayTexture = screenOverlayTexture.withPath(ID.PNG_TEXTURE);
		return this;
	}

	public FluidTypeBuilder blockOverlayTexture(ResourceLocation blockOverlayTexture) {
		this.blockOverlayTexture = blockOverlayTexture;
		return this;
	}

	public FluidTypeBuilder tint(KubeColor tint) {
		this.tint = tint;
		return this;
	}

	public FluidTypeBuilder descriptionId(String descriptionId) {
		properties.descriptionId(descriptionId);
		return this;
	}

	public FluidTypeBuilder motionScale(double motionScale) {
		properties.motionScale(motionScale);
		return this;
	}

	public FluidTypeBuilder canPushEntity(boolean canPushEntity) {
		properties.canPushEntity(canPushEntity);
		return this;
	}

	public FluidTypeBuilder canSwim(boolean canSwim) {
		properties.canSwim(canSwim);
		return this;
	}

	public FluidTypeBuilder canDrown(boolean canDrown) {
		properties.canDrown(canDrown);
		return this;
	}

	public FluidTypeBuilder fallDistanceModifier(float fallDistanceModifier) {
		properties.fallDistanceModifier(fallDistanceModifier);
		return this;
	}

	public FluidTypeBuilder canExtinguish(boolean canExtinguish) {
		properties.canExtinguish(canExtinguish);
		return this;
	}

	public FluidTypeBuilder canConvertToSource(boolean canConvertToSource) {
		properties.canConvertToSource(canConvertToSource);
		return this;
	}

	public FluidTypeBuilder supportsBoating(boolean supportsBoating) {
		properties.supportsBoating(supportsBoating);
		return this;
	}

	public FluidTypeBuilder pathType(@Nullable PathType pathType) {
		properties.pathType(pathType);
		return this;
	}

	public FluidTypeBuilder adjacentPathType(@Nullable PathType adjacentPathType) {
		properties.adjacentPathType(adjacentPathType);
		return this;
	}

	public FluidTypeBuilder sound(SoundAction action, SoundEvent sound) {
		properties.sound(action, sound);
		return this;
	}

	public FluidTypeBuilder canHydrate(boolean canHydrate) {
		properties.canHydrate(canHydrate);
		return this;
	}

	public FluidTypeBuilder lightLevel(int lightLevel) {
		properties.lightLevel(lightLevel);
		return this;
	}

	public FluidTypeBuilder density(int density) {
		properties.density(density);
		return this;
	}

	public FluidTypeBuilder temperature(int temperature) {
		properties.temperature(temperature);
		return this;
	}

	public FluidTypeBuilder viscosity(int viscosity) {
		properties.viscosity(viscosity);
		return this;
	}

	public FluidTypeBuilder rarity(Rarity rarity) {
		properties.rarity(rarity);
		return this;
	}

	public FluidTypeBuilder addDripstoneDripping(float chance, ParticleOptions dripParticle, Block cauldron, @Nullable SoundEvent fillSound) {
		properties.addDripstoneDripping(chance, dripParticle, cauldron, fillSound);
		return this;
	}

	public FluidTypeBuilder renderType(BlockRenderType renderType) {
		this.renderType = renderType;
		return this;
	}
}