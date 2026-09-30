package dev.latvian.mods.kubejs.block.custom;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.BlockRightClickedKubeEvent;
import dev.latvian.mods.kubejs.block.KubeJSBlockProperties;
import dev.latvian.mods.kubejs.block.callback.BlockExplodedCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateMirrorCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateModifyCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateModifyPlacementCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateRotateCallback;
import dev.latvian.mods.kubejs.block.callback.BounceRestitutionCallback;
import dev.latvian.mods.kubejs.block.callback.CanBeReplacedCallback;
import dev.latvian.mods.kubejs.block.callback.EntityBlockCallback;
import dev.latvian.mods.kubejs.block.callback.EntityFallenOnBlockCallback;
import dev.latvian.mods.kubejs.block.callback.RandomTickCallback;
import dev.latvian.mods.kubejs.block.entity.InventoryAttachment;
import dev.latvian.mods.kubejs.block.entity.KubeBlockEntity;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.ScriptTypeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

public class BasicKubeBlock extends Block implements SimpleWaterloggedBlock {
	public static class Builder extends BlockBuilder {
		public Builder(Identifier i) {
			super(i);
		}

		@Override
		public Block createObject() {
			return blockEntityInfo != null ? new WithEntity(this) : new BasicKubeBlock(this);
		}
	}

	@SuppressWarnings("DataFlowIssue") // safe
	public static class WithEntity extends BasicKubeBlock implements EntityBlock {
		public WithEntity(BlockBuilder p) {
			super(p);
		}

		@Nullable
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return blockBuilder.blockEntityInfo.createBlockEntity(pos, state);
		}

		@Nullable
		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
			return blockBuilder.blockEntityInfo.getTicker(level);
		}
	}

	public final BlockBuilder blockBuilder;
	public final VoxelShape shape;

	public BasicKubeBlock(BlockBuilder p) {
		super(p.createProperties());
		blockBuilder = p;
		shape = BlockBuilder.createShape(p.customShape);

		var blockState = stateDefinition.any();
		if (blockBuilder.defaultStateModification != null) {
			var callbackJS = new BlockStateModifyCallback(blockState);

			if (safeCallback(ScriptType.STARTUP, blockBuilder.defaultStateModification, callbackJS, "Error while creating default blockState for block " + p.id)) {
				registerDefaultState(callbackJS.getState());
			}
		} else if (blockBuilder.canBeWaterlogged()) {
			registerDefaultState(blockState.setValue(BlockStateProperties.WATERLOGGED, false));
		}
	}

	@Override
	public BlockBuilder kjs$getBlockBuilder() {
		return blockBuilder;
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		if (properties instanceof KubeJSBlockProperties kp) {
			for (var property : kp.blockBuilder.blockStateProperties) {
				builder.add(property);
			}
			kp.blockBuilder.blockStateProperties = Collections.unmodifiableSet(kp.blockBuilder.blockStateProperties);
		}
	}

	@Override
	@Deprecated
	public FluidState getFluidState(BlockState state) {
		return state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if (blockBuilder.placementStateModification != null) {
			var callbackJS = new BlockStateModifyPlacementCallback(context, this);
			if (safeCallback(context.getLevel(), blockBuilder.placementStateModification, callbackJS, "Error while modifying BlockState placement of " + blockBuilder.id)) {
				return callbackJS.getState();
			}
		}

		if (!blockBuilder.canBeWaterlogged()) {
			return defaultBlockState();
		}

		return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	public boolean canBeReplaced(BlockState blockState, BlockPlaceContext context) {
		if (blockBuilder.canBeReplacedFunction != null) {
			var callbackJS = new CanBeReplacedCallback(context, blockState);
			return blockBuilder.canBeReplacedFunction.test(callbackJS);
		}
		return super.canBeReplaced(blockState, context);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return state;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return blockBuilder.transparent || !(state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false));
	}

	@Override
	@Deprecated
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (blockBuilder.randomTickCallback != null) {
			var callback = new RandomTickCallback(level.kjs$getBlock(pos), random);
			safeCallback(level, blockBuilder.randomTickCallback, callback, "Error while random ticking custom block ");
		}
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return blockBuilder.randomTickCallback != null;
	}

	@Override
	@Deprecated
	public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return blockBuilder.transparent ? Shapes.empty() : super.getVisualShape(state, level, pos, ctx);
	}

	@Override
	@Deprecated
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return blockBuilder.transparent ? 1F : super.getShadeBrightness(state, level, pos);
	}

	@Override
	@Deprecated
	public boolean skipRendering(BlockState state, BlockState state2, Direction direction) {
		return blockBuilder.transparent ? (state2.is(this) || super.skipRendering(state, state2, direction)) : super.skipRendering(state, state2, direction);
	}

	private static <T> boolean safeCallback(ScriptTypeHolder holder, Consumer<T> consumer, T value, String errorMessage) {
		try {
			consumer.accept(value);
		} catch (Throwable e) {
			holder.kjs$getScriptType().console.error(errorMessage, e);
			return false;
		}

		return true;
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type) {
		if (blockBuilder.canBeWaterlogged()) {
			return SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, type);
		}

		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
		if (blockBuilder.canBeWaterlogged()) {
			return SimpleWaterloggedBlock.super.placeLiquid(levelAccessor, blockPos, blockState, fluidState);
		}

		return false;
	}

	@Override
	public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
		if (blockBuilder.canBeWaterlogged()) {
			return SimpleWaterloggedBlock.super.pickupBlock(user, level, pos, state);
		}

		return ItemStack.EMPTY;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		if (blockBuilder.canBeWaterlogged()) {
			return SimpleWaterloggedBlock.super.getPickupSound();
		}

		return Optional.empty();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (blockBuilder.insideCallback != null) {
			var callbackJS = new EntityBlockCallback(level, entity, pos, state);
			safeCallback(level, blockBuilder.insideCallback, callbackJS, "Error while an entity was inside a custom block ");
		} else {
			super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
		}
	}


	@Override
	public void stepOn(Level level, BlockPos blockPos, BlockState blockState, Entity entity) {
		if (blockBuilder.stepOnCallback != null) {
			var callbackJS = new EntityBlockCallback(level, entity, blockPos, blockState);
			safeCallback(level, blockBuilder.stepOnCallback, callbackJS, "Error while an entity stepped on custom block ");
		} else {
			super.stepOn(level, blockPos, blockState, entity);
		}
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		if (blockBuilder.fallOnCallback != null) {
			var callbackJS = new EntityFallenOnBlockCallback(level, entity, pos, state, fallDistance);
			safeCallback(level, blockBuilder.fallOnCallback, callbackJS, "Error while an entity fell on custom block ");
		} else {
			super.fallOn(level, state, pos, entity, fallDistance);
		}
	}


	@Override
	public float getBounceRestitution(Level level, BlockPos pos, BlockState blockState, Entity entity) {
		float base = super.getBounceRestitution(level, pos, blockState, entity);

		if (blockBuilder.bounceRestitutionCallback != null) {
			var callbackJS = new BounceRestitutionCallback(level, pos, blockState, entity, base);
			if (safeCallback(level, blockBuilder.bounceRestitutionCallback, callbackJS, "Error while calculating bounce restitution of custom block " + blockBuilder.id)) {
				return callbackJS.getRestitution();
			}
		}

		return base;
	}

	@Override
	public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
		if (blockBuilder.explodedCallback != null) {
			var callbackJS = new BlockExplodedCallback(level, pos, explosion);
			safeCallback(level, blockBuilder.explodedCallback, callbackJS, "Error while exploding custom block ");
		} else {
			super.wasExploded(level, pos, explosion);
		}
	}

	@Override
	public BlockState rotate(BlockState blockState, Rotation rotation) {
		if (blockBuilder.rotateStateModification != null) {
			var callbackJS = new BlockStateRotateCallback(blockState, rotation);
			if (safeCallback(ScriptType.STARTUP, blockBuilder.rotateStateModification, callbackJS, "Error while rotating BlockState of ")) {
				return callbackJS.getState();
			}
		}

		return super.rotate(blockState, rotation);
	}

	@Override
	public BlockState mirror(BlockState blockState, Mirror mirror) {
		if (blockBuilder.mirrorStateModification != null) {
			var callbackJS = new BlockStateMirrorCallback(blockState, mirror);
			if (safeCallback(ScriptType.STARTUP, blockBuilder.mirrorStateModification, callbackJS, "Error while mirroring BlockState of ")) {
				return callbackJS.getState();
			}
		}

		return super.mirror(blockState, mirror);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (blockBuilder.rightClick != null) {
			if (!level.isClientSide()) {
				blockBuilder.rightClick.accept(new BlockRightClickedKubeEvent(stack, player, hand, pos, hit.getDirection(), hit));
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (level.getBlockEntity(pos) instanceof KubeBlockEntity entity) {
			InventoryAttachment.onRemove(level, entity);
			level.updateNeighbourForOutputSignal(pos, this.asBlock());
		}
	}


	@Override
	public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
		if (livingEntity != null && !level.isClientSide() && level.getBlockEntity(blockPos) instanceof KubeBlockEntity e) {
			e.placerId = livingEntity.getUUID();
		}
	}
}