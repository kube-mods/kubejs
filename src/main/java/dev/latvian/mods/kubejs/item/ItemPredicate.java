package dev.latvian.mods.kubejs.item;

import dev.latvian.mods.kubejs.core.IngredientSupplierKJS;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

// TODO: Rework to fit ingredient changes
@RemapPrefixForJS("kjs$")
public interface ItemPredicate extends Predicate<ItemStack>, IngredientSupplierKJS {
	TypeInfo TYPE_INFO = TypeInfo.of(ItemPredicate.class);
	ItemPredicate NONE = _ -> false;
	ItemPredicate ALL = _ -> true;

	static ItemPredicate wrap(Context cx, @Nullable Object from) {
		if (from == null) {
			return NONE;
		} else if (from instanceof BaseFunction func) {
			return (ItemPredicate) cx.createInterfaceAdapter(TYPE_INFO, func);
		} else {
			if (from instanceof CharSequence s) {
				if (s.equals("*")) {
					return ALL;
				} else if (s.isEmpty() || s.equals("-")) {
					return NONE;
				}
			}

			var in = IngredientWrapper.wrap(cx, from);

			return in.kjs$isWildcard() ? ALL : in;
		}
	}

	// TODO: remove or rework
	@Deprecated(forRemoval = true)
	default ItemStack[] kjs$getStackArray() {
		return ItemWrapper.getList().stream().map(ItemStackTemplate::create).filter(this).toArray(ItemStack[]::new);
	}

	// TODO: remove or rework
	@Deprecated(forRemoval = true)
	default ItemStackSet kjs$getStacks() {
		return new ItemStackSet(kjs$getStackArray());
	}

	// TODO: remove or rework
	// specifically this can VERY likely just be removed since we can use SlotDisplay instead?
	@Deprecated(forRemoval = true)
	default ItemStackSet kjs$getDisplayStacks() {
		var set = new ItemStackSet();

		for (var template : ItemWrapper.getList()) {
			var stack = template.create();
			if (test(stack)) {
				set.add(stack);
			}
		}

		return set;
	}

	default boolean kjs$isWildcard() {
		return this == ALL;
	}

	default Set<Item> kjs$getItemTypes() {
		var items = kjs$getStackArray();

		if (items.length == 1 && !items[0].isEmpty()) {
			return Set.of(items[0].getItem());
		}

		var set = new LinkedHashSet<Item>(items.length);

		for (var stack : items) {
			if (!stack.isEmpty()) {
				set.add(stack.getItem());
			}
		}

		return set;
	}

	default Set<String> kjs$getItemIds() {
		var items = kjs$getStackArray();

		if (items.length == 1 && !items[0].isEmpty()) {
			return Set.of(items[0].kjs$getId());
		}

		var ids = new LinkedHashSet<String>(items.length);

		for (var item : items) {
			if (!item.isEmpty()) {
				ids.add(item.kjs$getId());
			}
		}

		return ids;
	}

	/// Marks whether an ingredient is safe to be used to match recipe filters during the recipe event.
	/// (The answer is usually no for non-Vanilla ingredients, but can be overridden manually by addons or downstream mods with integration.)
	default boolean kjs$canBeUsedForMatching() {
		return true;
	}

	@Override
	default Ingredient kjs$asIngredient() {
		return Ingredient.of(Arrays.stream(kjs$getStackArray()).map(ItemStack::getItem));
	}
}
