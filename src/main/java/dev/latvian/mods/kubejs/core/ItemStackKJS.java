package dev.latvian.mods.kubejs.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import dev.latvian.mods.kubejs.codec.KubeJSCodecs;
import dev.latvian.mods.kubejs.component.DataComponentWrapper;
import dev.latvian.mods.kubejs.core.component.ItemComponentFunctions;
import dev.latvian.mods.kubejs.core.mixin.DataComponentPatchAccessorMixin;
import dev.latvian.mods.kubejs.level.LevelBlock;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.Replaceable;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.kubejs.util.WithCodec;
import dev.latvian.mods.kubejs.web.RelativeURL;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import dev.latvian.mods.rhino.util.SpecialEquality;
import dev.latvian.mods.rhino.util.ToStringJS;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RemapPrefixForJS("kjs$")
public interface ItemStackKJS extends
	SpecialEquality,
	WithCodec,
	IngredientSupplierKJS,
	ToStringJS,
	Replaceable,
	ItemComponentFunctions,
	MutableDataComponentHolderKJS,
	ItemMatch,
	RegistryObjectKJS<Item> {
	default ItemStack kjs$self() {
		return (ItemStack) this;
	}

	@Override
	default boolean specialEquals(Context cx, @Nullable Object o, boolean shallow) {
		return switch (o) {
			case CharSequence cs -> kjs$getId().equals(ID.string(cs.toString()));
			case Identifier id -> kjs$getIdLocation().equals(id);
			case ItemStack s -> kjs$equalsIgnoringCount(s);
			case null, default -> KubeJSCodecs.filter(ItemWrapper.wrapResult(cx, o), this::kjs$equalsIgnoringCount);
		};
	}

	default boolean kjs$equalsIgnoringCount(ItemStack stack) {
		var self = kjs$self();

		if (self == stack) {
			return true;
		} else if (self.isEmpty()) {
			return stack.isEmpty();
		}

		return ItemStack.isSameItemSameComponents(self, stack);
	}

	@Override
	default ResourceKey<Registry<Item>> kjs$getRegistryId() {
		return Registries.ITEM;
	}

	@Override
	default Registry<Item> kjs$getRegistry() {
		return BuiltInRegistries.ITEM;
	}

	@Override
	default Identifier kjs$getIdLocation() {
		return kjs$self().getItem().kjs$getIdLocation();
	}

	@Override
	default Holder<Item> kjs$asHolder() {
		return kjs$self().getItem().kjs$asHolder();
	}

	@Override
	default ResourceKey<Item> kjs$getKey() {
		return kjs$self().getItem().kjs$getKey();
	}

	@Override
	default String kjs$getId() {
		return kjs$self().getItem().kjs$getId();
	}

	@Override
	default String kjs$getMod() {
		return kjs$self().getItem().kjs$getMod();
	}

	@Nullable
	default Block kjs$getBlock() {
		return kjs$self().getItem() instanceof BlockItem bi ? bi.getBlock() : null;
	}

	@ReturnsSelf(copy = true)
	default ItemStack kjs$withCount(int c) {
		if (c <= 0 || kjs$self().isEmpty()) {
			return ItemStack.EMPTY;
		}

		var is = kjs$self().copy();
		is.setCount(c);
		return is;
	}

	@Override
	default String kjs$getComponentString(Context cx) {
		return DataComponentWrapper.patchToString(new StringBuilder(), RegistryAccessContainer.of(cx).nbt(), kjs$self().getComponentsPatch()).toString();
	}

	@ReturnsSelf(copy = true)
	default ItemStack kjs$withCustomName(@Nullable Component name) {
		var is = kjs$self().copy();
		is.kjs$setCustomName(name);
		return is;
	}

	default ItemEnchantments kjs$getEnchantments() {
		return EnchantmentHelper.getEnchantmentsForCrafting(kjs$self());
	}

	default boolean kjs$hasEnchantment(Holder<Enchantment> enchantment, int level) {
		var e = kjs$getEnchantments();
		return e != null && e.getLevel(enchantment) >= level;
	}

	@ReturnsSelf
	default ItemStack kjs$enchant(Holder<Enchantment> enchantment, int level) {
		var is = kjs$self();
		is.enchant(enchantment, level);
		return is;
	}

	@ReturnsSelf(copy = true)
	default ItemStack kjs$enchant(ItemEnchantments enchantments) {
		var is = kjs$self().copy();

		EnchantmentHelper.updateEnchantments(is, mutable -> {
			for (var entry : enchantments.entrySet()) {
				mutable.upgrade(entry.getKey(), entry.getIntValue());
			}
		});

		return is;
	}

	default boolean kjs$areItemsEqual(ItemStack other) {
		return kjs$self().getItem() == other.getItem();
	}

	default boolean kjs$areComponentsEqual(ItemStack other) {
		return ItemStack.isSameItemSameComponents(kjs$self(), other);
	}

	default float kjs$getHarvestSpeed(@Nullable LevelBlock block) {
		return kjs$self().getDestroySpeed(block == null ? Blocks.AIR.defaultBlockState() : block.getBlockState());
	}

	default float kjs$getHarvestSpeed() {
		return kjs$getHarvestSpeed(null);
	}

	default Map<String, Object> kjs$getTypeData() {
		return kjs$self().getItem().kjs$getTypeData();
	}

	@Override
	default String toStringJS(Context cx) {
		return kjs$toItemString0(RegistryAccessContainer.of(cx).nbt());
	}

	default String kjs$toItemString(Context cx) {
		return kjs$toItemString0(RegistryAccessContainer.of(cx).nbt());
	}

	default String kjs$toItemString0(@Nullable DynamicOps<Tag> dynamicOps) {
		var is = kjs$self();
		var count = is.getCount();

		if (count <= 0) {
			return "minecraft:air";
		}

		var builder = new StringBuilder();
		builder.append('\'');

		if (count > 1) {
			builder.append(count);
			builder.append("x ");
		}

		builder.append(kjs$getId());

		if (!is.isComponentsPatchEmpty()) {
			DataComponentWrapper.patchToString(builder, dynamicOps, is.getComponentsPatch());
		}

		builder.append('\'');
		return builder.toString();
	}

	@Override
	default Ingredient kjs$asIngredient() {
		var p = kjs$self().getComponentsPatch();
		if (p.isEmpty()) {
			return Ingredient.of(HolderSet.direct(kjs$asHolder()));
		}
		var map = DataComponentMap.builder();
		var content = ((DataComponentPatchAccessorMixin) (Object) p).getMap();
		for (var entry : content.entrySet()) {
			if (entry.getValue() != null) {
				map.set(entry.getKey(), Cast.to(entry.getValue()));
			}
		}
		return IngredientWrapper.withData(HolderSet.direct(kjs$asHolder()), map.build());
	}

	@Override
	default Codec<ItemStack> getCodec(Context cx) {
		return ItemStack.CODEC;
	}

	@ReturnsSelf(copy = true)
	default ItemStack kjs$withLore(Component[] lines) {
		var is = kjs$self().copy();
		is.set(DataComponents.LORE, new ItemLore(List.of(lines)));
		return is;
	}

	@ReturnsSelf(copy = true)
	default ItemStack kjs$withLore(Component[] lines, Component[] styledLines) {
		var is = kjs$self().copy();
		is.set(DataComponents.LORE, new ItemLore(List.of(lines), List.of(styledLines)));
		return is;
	}

	@Override
	default Object replaceThisWith(RecipeScriptContext cx, Object with) {
		var t = kjs$self();
		var r = ItemWrapper.wrap(cx.cx(), with);

		if (!ItemStack.isSameItemSameComponents(t, r)) {
			r.setCount(t.getCount());
			return r;
		}

		return this;
	}

	@Override
	default boolean matches(RecipeMatchContext cx, ItemStack s, boolean exact) {
		return kjs$self().getItem() == s.getItem();
	}

	@Override
	default boolean matches(RecipeMatchContext cx, Ingredient in, boolean exact) {
		return in.test(kjs$self());
	}

	@Override
	default boolean matches(RecipeMatchContext cx, ItemLike itemLike, boolean exact) {
		return kjs$self().getItem() == itemLike.asItem();
	}

	default RelativeURL kjs$getWebIconURL(DynamicOps<Tag> ops, int size) {
		var url = "/img/" + size + "/item/" + ID.url(kjs$getIdLocation());
		var c = DataComponentWrapper.patchToString(new StringBuilder(), ops, DataComponentWrapper.visualPatch(kjs$self().getComponentsPatch())).toString();
		return new RelativeURL(url, c.equals("[]") ? Map.of() : Map.of("components", c.substring(1, c.length() - 1)));
	}
}
