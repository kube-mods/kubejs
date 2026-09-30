package dev.latvian.mods.kubejs.block.callback;

import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BounceRestitutionCallback extends EntityBlockCallback {
	private float restitution;

	public BounceRestitutionCallback(Level level, BlockPos pos, BlockState state, Entity entity, float defaultRestitution) {
		super(level, entity, pos, state);
		this.restitution = defaultRestitution;
	}

	@Info("""
       The restitution that will be used. Starts as the block's default.
       0 = no bounce, 1 = rebounds with the same speed it landed with.
       """)
	public float getRestitution() {
		return restitution;
	}

	@Info("""
       Sets the restitution. Negative values are clamped to 0.
       Values above 1 make the entity rebound faster than it landed.
       """)
	public void setRestitution(float restitution) {
		this.restitution = Math.max(0F, restitution);
	}
}