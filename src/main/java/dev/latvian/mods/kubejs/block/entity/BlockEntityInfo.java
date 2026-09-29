package dev.latvian.mods.kubejs.block.entity;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.core.InventoryKJS;
import dev.latvian.mods.kubejs.core.ServerPlayerKJS;
import dev.latvian.mods.kubejs.item.ItemPredicate;
import dev.latvian.mods.rhino.util.HideFromJS;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class BlockEntityInfo implements BlockEntityAttachmentHandler {
	public transient final BlockBuilder blockBuilder;
	@SuppressWarnings("NotNullFieldNotInitialized") // lateinit field
	public transient BlockEntityType<?> entityType;
	public transient CompoundTag initialData;
	public transient boolean serverTicking;
	public transient boolean clientTicking;
	public transient boolean attachmentsTicking;
	public transient int tickFrequency;
	public transient int tickOffset;
	public transient boolean sync;
	public transient List<EnergyStorageAttachment.Config> energyConfigs;
	public transient List<FluidTankAttachment.Config> fluidConfigs;
	public transient List<InventoryAttachment.Config> inventoryConfigs;
	public transient List<CustomCapabilityAttachment.Config> customCapConfigs;
	public transient Int2ObjectMap<BlockEntityEventCallback> eventHandlers;

	public BlockEntityInfo(BlockBuilder blockBuilder) {
		this.blockBuilder = blockBuilder;
		this.initialData = new CompoundTag();
		this.serverTicking = false;
		this.clientTicking = false;
		this.attachmentsTicking = false;
		this.tickFrequency = 1;
		this.tickOffset = 0;
		this.sync = false;
		this.eventHandlers = new Int2ObjectArrayMap<>(0);
		this.energyConfigs = new ArrayList<>(0);
		this.fluidConfigs = new ArrayList<>(0);
		this.inventoryConfigs = new ArrayList<>(0);
		this.customCapConfigs = new ArrayList<>(0);
	}

	public boolean hasAnyAttachments() {
		return !energyConfigs.isEmpty() || !fluidConfigs.isEmpty() || !inventoryConfigs.isEmpty() || !customCapConfigs.isEmpty();
	}

	public void initialData(CompoundTag data) {
		initialData = data;
	}

	public void serverTicking() {
		serverTicking = true;
	}

	public void clientTicking() {
		clientTicking = true;
	}

	public void ticking() {
		serverTicking();
		clientTicking();
	}

	public void tickFrequency(int frequency) {
		tickFrequency = Math.max(1, frequency);
	}

	public void tickOffset(int offset) {
		tickOffset = Math.max(0, offset);
	}

	public void enableSync() {
		sync = true;
	}

	@Override
	public void energyStorage(String id, Set<Direction> directions, int capacity, int maxReceive, int maxExtract, int autoOutput) {
		var config = EnergyStorageAttachment.createConfig(id, directions, capacity, maxReceive, maxExtract, autoOutput);
		energyConfigs.add(config);

		if (!attachmentsTicking && config.isTicking()) {
			attachmentsTicking = true;
		}
	}

	@Override
	public void fluidTank(String id, Set<Direction> directions, int capacity, @Nullable FluidIngredient inputFilter) {
		fluidConfigs.add(FluidTankAttachment.createConfig(id, directions, capacity, inputFilter));
	}

	@Override
	public void inventory(String id, Set<Direction> directions, int width, int height, @Nullable ItemPredicate inputFilter) {
		inventoryConfigs.add(InventoryAttachment.createConfig(id, directions, width, height, inputFilter));
	}

	@Override
	public void attachCustomCapability(String id, Set<Direction> directions, BlockCapability<?, ?> capability, Supplier<?> dataFactory) {
		customCapConfigs.add(CustomCapabilityAttachment.createConfig(id, directions, capability, dataFactory));
	}

	public void eventHandler(int eventId, BlockEntityEventCallback callback) {
		eventHandlers.put(eventId, callback);
	}

	public void rightClickOpensInventory(String id) {
		blockBuilder.rightClick = e -> {
			if (e.getPlayer() instanceof ServerPlayerKJS p && e.getBlock().getEntity() instanceof KubeBlockEntity entity && entity.attachments.get(id) instanceof InventoryKJS inv) {
				p.kjs$openInventoryGUI(inv, blockBuilder.get().getName());
			}
		};
	}

	public void rightClickFillsTank(String id) {
		blockBuilder.rightClick = e -> {
			if (e.getPlayer() instanceof ServerPlayerKJS && e.getBlock().getEntity() instanceof KubeBlockEntity entity && entity.attachments.get(id) instanceof ResourceHandler<?> tank) {
				@SuppressWarnings("unchecked")
				var fluidTank = (ResourceHandler<FluidResource>) tank;
				FluidUtil.interactWithFluidHandler(e.getPlayer(), e.getHand(), e.getBlock().getPos(), fluidTank, null);
			}
		};
	}

	@HideFromJS
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new KubeBlockEntity(pos, state, this);
	}

	@HideFromJS
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level) {
		if (level.isClientSide()) {
			return clientTicking ? (BlockEntityTicker) KubeBlockEntity.TICKER : null;
		} else {
			return serverTicking || attachmentsTicking ? (BlockEntityTicker) KubeBlockEntity.TICKER : null;
		}
	}

	@Override
	public String toString() {
		return "BlockEntityInfo[" + blockBuilder.id + "]";
	}
}
