package dev.latvian.mods.kubejs.block.drop;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import org.jspecify.annotations.Nullable;

public record BlockDrops(ItemStack[] items, @Nullable ContextIntProvider rolls, @Nullable Item defaultItem) {
	public static final BlockDrops EMPTY = new BlockDrops(new ItemStack[0], null, null);

	public static BlockDrops createDefault(Item item) {
		return new BlockDrops(new ItemStack[0], new ConstantValue(1), item);
	}

	public static BlockDrops createStack(ItemStack item) {
		return new BlockDrops(new ItemStack[]{item}, new ConstantValue(1), null);
	}
}
