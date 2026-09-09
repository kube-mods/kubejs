package dev.latvian.mods.kubejs.item;

import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ItemStackSet implements Iterable<ItemStack> {
	private final LinkedHashMap<ItemResource, ItemStack> map;

	public ItemStackSet(int initialSize) {
		map = new LinkedHashMap<>(initialSize);
	}

	public ItemStackSet() {
		this(2);
	}

	public ItemStackSet(ItemStack... items) {
		this(items.length);

		for (var stack : items) {
			add(stack);
		}
	}

	public void add(ItemStack stack) {
		var key = ItemResource.of(stack);

		if (key != ItemResource.EMPTY) {
			map.putIfAbsent(key, stack);
		}
	}

	public void addItem(Item item) {
		if (item != Items.AIR) {
			map.putIfAbsent(ItemResource.of(item), item.getDefaultInstance());
		}
	}

	public void remove(ItemStack stack) {
		var key = ItemResource.of(stack);

		if (key != ItemResource.EMPTY) {
			map.remove(key);
		}
	}

	public boolean contains(ItemStack stack) {
		var key = ItemResource.of(stack);
		return key != ItemResource.EMPTY && map.containsKey(key);
	}

	public List<ItemStack> toList() {
		return map.isEmpty() ? List.of() : new ArrayList<>(map.values());
	}

	public ItemStack[] toArray() {
		return map.isEmpty() ? ItemWrapper.EMPTY_ARRAY : map.values().toArray(ItemWrapper.EMPTY_ARRAY);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	@Override
	public Iterator<ItemStack> iterator() {
		return map.values().iterator();
	}

	@Override
	public void forEach(Consumer<? super ItemStack> action) {
		map.values().forEach(action);
	}

	public ItemStack getFirst() {
		return map.isEmpty() ? ItemStack.EMPTY : map.values().iterator().next();
	}

	public Stream<ItemStack> stream() {
		return map.isEmpty() ? Stream.of() : map.values().stream();
	}
}
