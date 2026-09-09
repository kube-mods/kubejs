package dev.latvian.mods.kubejs.item;

import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ItemStackSet implements Iterable<ItemStack> {
	private final LinkedHashMap<ItemStackKey, ItemStack> map;

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
		var key = ItemStackKey.of(stack);

		if (key != ItemStackKey.EMPTY) {
			map.putIfAbsent(key, stack);
		}
	}

	public void addItem(Item item) {
		if (item != Items.AIR) {
			map.putIfAbsent(item.kjs$getTypeItemStackKey(), new ItemStack(item));
		}
	}

	public void remove(ItemStack stack) {
		var key = ItemStackKey.of(stack);

		if (key != ItemStackKey.EMPTY) {
			map.remove(key);
		}
	}

	public boolean contains(ItemStack stack) {
		var key = ItemStackKey.of(stack);
		return key != ItemStackKey.EMPTY && map.containsKey(key);
	}

	public void addAll(ItemStackSet other) {
		map.putAll(other.map);
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

	public static ItemStackSet merge(ItemStackSet first, ItemStackSet second) {
		first.addAll(second);
		return second;
	}
}
