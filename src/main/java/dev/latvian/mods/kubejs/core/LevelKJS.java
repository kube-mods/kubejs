package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.level.CachedLevelBlock;
import dev.latvian.mods.kubejs.level.ExplosionProperties;
import dev.latvian.mods.kubejs.level.LevelBlock;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.ScriptTypeHolder;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.rhino.util.RemapForJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@RemapPrefixForJS("kjs$")
public interface LevelKJS extends WithAttachedData<Level>, ScriptTypeHolder, EntityGetterKJS {
	@Override
	default Level kjs$self() {
		return (Level) this;
	}

	@Override
	@RemapForJS("getSide")
	default ScriptType kjs$getScriptType() {
		return kjs$self().isClientSide() ? ScriptType.CLIENT : ScriptType.SERVER;
	}

	@Override
	default Component kjs$getName() {
		return Component.literal(kjs$getDimension().toString());
	}

	@Override
	default void kjs$tell(Component message) {
		for (var entity : kjs$self().players()) {
			entity.kjs$tell(message);
		}
	}

	@Override
	default void kjs$setStatusMessage(Component message) {
		for (var entity : kjs$self().players()) {
			entity.kjs$setStatusMessage(message);
		}
	}

	@Override
	@Info(value = "Each player in the level (world) runs the specified console command with their permission level.", params = {
		@Param(name = "command", value = "The console command. Slash at the beginning is optional."),
	})
	default void kjs$runCommand(String command) {
		for (var entity : kjs$self().players()) {
			entity.kjs$runCommand(command);
		}
	}

	@Override
	@Info(value = "Each player in the level (world) runs the specified console command with their permission level. The command won't output any logs in chat nor console", params = {
		@Param(name = "command", value = "The console command. Slash at the beginning is optional."),
	})
	default void kjs$runCommandSilent(String command) {
		for (var entity : kjs$self().players()) {
			entity.kjs$runCommandSilent(command);
		}
	}

	@Override
	default void kjs$setActivePostShader(@Nullable Identifier id) {
		for (var entity : kjs$self().players()) {
			entity.kjs$setActivePostShader(id);
		}
	}

	default Identifier kjs$getDimension() {
		return kjs$self().dimension().identifier();
	}

	default boolean kjs$isOverworld() {
		return kjs$self().dimension() == Level.OVERWORLD;
	}

	default void kjs$setTime(long time) {
		if (kjs$self().getLevelData() instanceof ServerLevelData d) {
			d.setGameTime(time);
		}
	}

	default LevelBlock kjs$getBlock(int x, int y, int z) {
		return kjs$getBlock(new BlockPos(x, y, z));
	}

	default LevelBlock kjs$getBlock(BlockPos pos) {
		return new CachedLevelBlock(kjs$self(), pos);
	}

	default LevelBlock kjs$getBlock(BlockEntity entity) {
		return new CachedLevelBlock(entity.getLevel(), entity.getBlockPos()).cache(entity).cache(entity.getBlockState());
	}

	default void kjs$explode(double x, double y, double z, ExplosionProperties properties) {
		properties.explode(kjs$self(), x, y, z);
	}

	@Nullable
	default Entity kjs$createEntity(EntityType<?> type) {
		return type.create(kjs$self(), EntitySpawnReason.COMMAND);
	}

	@Nullable
	default Entity kjs$createEntity(EntityType<?> type, EntitySpawnReason reason) {
		return type.create(kjs$self(), reason);
	}

	default void spawnEntity(EntityType<?> type, Consumer<Entity> callback) {
		var entity = type.create(kjs$self(), EntitySpawnReason.COMMAND);

		if (entity != null) {
			callback.accept(entity);
			kjs$self().addFreshEntity(entity);
		}
	}

	default void kjs$spawnFireworks(double x, double y, double z, Fireworks fireworks, int lifetime) {
		var stack = new ItemStack(Items.FIREWORK_ROCKET);
		stack.set(DataComponents.FIREWORKS, fireworks);

		var rocket = new FireworkRocketEntity(kjs$self(), x, y, z, stack);

		if (lifetime != -1) {
			((FireworkRocketEntityKJS) rocket).setLifetimeKJS(lifetime);
		}

		rocket.setInvisible(true);
		kjs$self().addFreshEntity(rocket);
	}

	default void kjs$spawnParticles(ParticleOptions options, boolean overrideLimiter, double x, double y, double z, double vx, double vy, double vz, int count, double speed) {
		var level = kjs$self();

		if (count == 0) {
			double d0 = speed * vx;
			double d2 = speed * vy;
			double d4 = speed * vz;

			try {
				level.addParticle(options, overrideLimiter, false, x, y, z, d0, d2, d4);
			} catch (Throwable ignored) {
			}
		} else {
			var random = level.getRandom();

			for (int i = 0; i < count; ++i) {
				double ox = random.nextGaussian() * vx;
				double oy = random.nextGaussian() * vy;
				double oz = random.nextGaussian() * vz;
				double d6 = random.nextGaussian() * speed;
				double d7 = random.nextGaussian() * speed;
				double d8 = random.nextGaussian() * speed;

				try {
					level.addParticle(options, overrideLimiter, false, x + ox, y + oy, z + oz, d6, d7, d8);
				} catch (Throwable ignored) {
					return;
				}
			}
		}
	}

	default void kjs$spawnLightning(double x, double y, double z, boolean visualOnly, @Nullable ServerPlayer cause) {
		var e = EntityTypes.LIGHTNING_BOLT.create(kjs$self(), EntitySpawnReason.COMMAND);
		e.snapTo(x, y, z);
		e.setCause(cause);
		e.setVisualOnly(visualOnly);
		kjs$self().addFreshEntity(e);
	}

	default void kjs$spawnLightning(double x, double y, double z, boolean visualOnly) {
		kjs$spawnLightning(x, y, z, visualOnly, null);
	}
}
