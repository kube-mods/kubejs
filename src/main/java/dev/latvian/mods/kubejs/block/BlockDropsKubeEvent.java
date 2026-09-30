package dev.latvian.mods.kubejs.block;

import dev.latvian.mods.kubejs.entity.KubeEntityEvent;
import dev.latvian.mods.kubejs.item.ItemPredicate;
import dev.latvian.mods.kubejs.level.LevelBlock;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Info(value = """
	Modify dropped items and xp from block.
	""")
public class BlockDropsKubeEvent implements KubeEntityEvent {
	private final BlockDropsEvent event;

	public BlockDropsKubeEvent(BlockDropsEvent event) {
		this.event = event;
	}

	@Override
	public ServerLevel getLevel() {
		return event.getLevel();
	}

	@Override
	@Nullable
	public Entity getEntity() {
		return event.getBreaker();
	}

	@Info("The block that was broken.")
	public LevelBlock getBlock() {
		return event.getLevel().kjs$getBlock(event.getPos()).cache(event.getState()).cache(event.getBlockEntity());
	}

	@Info("The experience dropped by the block.")
	public int getXp() {
		return event.getDroppedExperience();
	}

	@Info("Sets the experience dropped by the block.")
	public void setXp(int xp) {
		event.setDroppedExperience(xp);
	}

	@Info("Dropped item entities.")
	public List<ItemEntity> getItemEntities() {
		return event.getDrops();
	}

	@Info("Dropped items. Immutable.")
	public List<ItemStack> getItems() {
		return event.getDrops().stream().map(ItemEntity::getItem).toList();
	}

	public boolean containsItem(ItemPredicate item) {
		for (var drop : event.getDrops()) {
			if (item.test(drop.getItem())) {
				return true;
			}
		}

		return false;
	}

	public ItemEntity addItem(ItemStack item) {
		double x = event.getPos().getX() + 0.5 + Mth.nextDouble(event.getLevel().getRandom(), -0.25, 0.25);
		double y = event.getPos().getY() + 0.5 + Mth.nextDouble(event.getLevel().getRandom(), -0.25, 0.25) - EntityTypes.ITEM.getHeight() / 2.0;
		double z = event.getPos().getZ() + 0.5 + Mth.nextDouble(event.getLevel().getRandom(), -0.25, 0.25);
		var entity = new ItemEntity(event.getLevel(), x, y, z, item);
		event.getDrops().add(entity);
		return entity;
	}

	public void removeItem(ItemPredicate item) {
		event.getDrops().removeIf(drop -> item.test(drop.getItem()));
	}

	@Nullable
	@Info("The tool used when breaking this block. May be null.")
	public ItemStack getTool() {
		return event.getTool().isEmpty() ? null : event.getTool();
	}
}