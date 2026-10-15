package dev.latvian.mods.kubejs.item;

import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.component.DataComponentWrapper;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.registry.ModelledBuilderBase;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.TickDuration;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ReturnsSelf
public class ItemBuilder extends ModelledBuilderBase<Item> implements ItemBehaviorFunctions {

	public transient @Nullable Map<Object, Object> components;
	public transient int maxStackSize;
	public transient int maxDamage;
	public transient int burnTime;
	private @Nullable Identifier containerItem;
	public transient @Nullable Function<ItemStack, Collection<ItemStack>> subtypes;
	public transient @Nullable Rarity rarity;
	public transient boolean fireResistant;
	public transient @Nullable FoodBuilder foodBuilder;
	public transient @Nullable ResourceKey<JukeboxSong> jukeboxSong;
	public transient final ItemBehavior behavior = new ItemBehavior();

	public transient @Nullable Compostable compostable;
	public transient @Nullable CookingFuel cookingFuel;

	public transient @Nullable Tool tool;
	public transient @Nullable ItemAttributeModifiers itemAttributeModifiers;
	public transient boolean canRepair;

	public ItemBuilder(Identifier id) {
		super(id);
		this.baseTexture = id.withPath(ID.ITEM).toString();

		this.maxStackSize = -1;
		this.maxDamage = 0;
		this.burnTime = 0;
		this.containerItem = null;
		this.subtypes = null;
		this.rarity = null;
		this.foodBuilder = null;
		this.fireResistant = false;

		this.tool = null;
		this.itemAttributeModifiers = null;
		this.canRepair = true;
	}

	@Override
	public Item createObject() {
		return new Item(createItemProperties());
	}

	@Override
	public Item transformObject(Item obj) {
		displayName(displayName, formattedDisplayName);
		obj.kjs$setItemBehavior(behavior);
		return obj;
	}

	@Override
	public void generateAssets(KubeAssetGenerator generator) {
		generateItemModels(generator);
	}

	@HideFromJS
	public void generateItemModels(KubeAssetGenerator generator) {
		generator.itemModel(id, model -> {
			if (modelGenerator != null) {
				modelGenerator.accept(model);
				return;
			}

			model.parent(parentModel != null ? parentModel : KubeAssetGenerator.GENERATED_ITEM_MODEL);

			if (textures.isEmpty()) {
				model.texture("layer0", baseTexture);
			} else {
				model.textures(textures);
			}
		}, KubeAssetGenerator.createItemTintSources(getMaxTintIndex()));
	}

	@HideFromJS
	public boolean hasCustomModel() {
		return modelGenerator != null ||
			parentModel != null ||
			!textures.isEmpty() ||
			!baseTexture.equals(id.withPath(ID.ITEM).toString());
	}

	@HideFromJS
	public int getMaxTintIndex() {
		int maxTintIndex = behavior.tint == null ? -1 : behavior.tint.getMaxTintIndex();

		if (this instanceof BlockItemBuilder blockItemBuilder && blockItemBuilder.blockBuilder.tint != null) {
			maxTintIndex = Math.max(maxTintIndex, blockItemBuilder.blockBuilder.tint.getMaxTintIndex());
		}

		return maxTintIndex;
	}

	public <T> ItemBuilder component(DataComponentType<T> type, T value) {
		if (components == null) {
			components = new HashMap<>();
		}

		components.put(type, value);
		return this;
	}

	@Info("Sets the item's max stack size. Default is 64.")
	public ItemBuilder maxStackSize(int v) {
		maxStackSize = v;
		return this;
	}

	@Info("Makes the item not stackable, equivalent to setting the item's max stack size to 1.")
	public ItemBuilder unstackable() {
		return maxStackSize(1);
	}

	@Info("Sets the item's max damage. Default is 0 (No durability).")
	public ItemBuilder maxDamage(int v) {
		maxDamage = v;
		return this;
	}

	@Info("Sets the item's burn time. Default is 0 (Not a fuel).")
	public ItemBuilder burnTime(TickDuration v) {
		burnTime = v.intTicks();
		return cookingFuel(v);
	}

	@Info("Sets how many compost layers the item adds when composted.")
	public ItemBuilder compostable(int layers) {
		compostable = new Compostable(new ResolvableInt.Constant(layers));
		return this;
	}

	@Info("Sets how long the item burns for when used as furnace or campfire fuel. Cooking speed defaults to the vanilla value.")
	public ItemBuilder cookingFuel(TickDuration burnTime) {
		cookingFuel = new CookingFuel(new ResolvableInt.Constant(burnTime.intTicks()), ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER));
		return this;
	}

	@Info("Sets how long the item burns for when used as fuel, and how fast cooking goes while one is burning.")
	public ItemBuilder cookingFuel(TickDuration burnTime, float speedMultiplier) {
		cookingFuel = new CookingFuel(new ResolvableInt.Constant(burnTime.intTicks()), new ResolvableFloat.Constant(speedMultiplier));
		return this;
	}

	@Info("Sets the item's container item, e.g. a bucket for a milk bucket.")
	public ItemBuilder containerItem(Identifier id) {
		containerItem = id;
		return this;
	}

	@Info("""
		Adds subtypes to the item. The function should return a collection of item stacks, each with a different subtype.

		Each subtype will appear as a separate item in JEI and the creative inventory.
		""")
	public ItemBuilder subtypes(Function<ItemStack, Collection<ItemStack>> fn) {
		subtypes = fn;
		return this;
	}

	@Info("Sets the item's rarity.")
	public ItemBuilder rarity(Rarity v) {
		rarity = v;
		return this;
	}

	@Deprecated
	public ItemBuilder group(@Nullable String g) {
		ConsoleJS.STARTUP.error("Item builder .group() is no longer supported, use StartupEvents.modifyCreativeTab!");
		return this;
	}

	@Info("Colorizes item's texture of the given index. Index is used when you have multiple layers, e.g. a crushed ore (of rock + ore).")
	public ItemBuilder color(int index, ItemTintFunction color) {
		if (!(behavior.tint instanceof ItemTintFunction.Mapped)) {
			behavior.tint = new ItemTintFunction.Mapped();
		}

		((ItemTintFunction.Mapped) behavior.tint).map.put(index, color);
		return this;
	}

	@Info("Colorizes item's texture of the given index. Useful for coloring items, like GT ores ore dusts.")
	public ItemBuilder color(ItemTintFunction callback) {
		behavior.tint = callback;
		return this;
	}

	@Info("""
		Set the food properties of the item.
		""")
	public ItemBuilder food(Consumer<FoodBuilder> b) {
		if (foodBuilder == null) {
			foodBuilder = new FoodBuilder();
		}

		b.accept(foodBuilder);
		foodEaten(foodBuilder.eaten);
		return this;
	}

	@Info("""
		Set the food nutrition and saturation of the item.
		""")
	public ItemBuilder food(int nutrition, float saturation) {
		return food(b -> b.nutrition(nutrition).saturation(saturation));
	}

	@Info("Makes the item fire resistant like netherite tools (or not).")
	public ItemBuilder fireResistant(boolean isFireResistant) {
		fireResistant = isFireResistant;
		return this;
	}

	@Info("Makes the item fire resistant like netherite tools.")
	public ItemBuilder fireResistant() {
		return fireResistant(true);
	}

	@Override
	public ItemBehavior kjs$getOrCreateBehavior() {
		return behavior;
	}

	public ItemBuilder jukeboxPlayable(ResourceKey<JukeboxSong> song) {
		this.jukeboxSong = song;
		return this;
	}

	public ItemBuilder disableRepair() {
		this.canRepair = false;
		return this;
	}

	public Item.Properties createItemProperties() {
		var properties = new KubeJSItemProperties(this);
		properties.setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), this.id));

		if (components != null && !components.isEmpty()) {
			for (var entry : components.entrySet()) {
				var type = DataComponentWrapper.wrapType(entry.getKey());

				if (type != null) {
					properties.component((DataComponentType) type, entry.getValue());
				} else {
					ConsoleJS.STARTUP.error("Component '" + entry.getKey() + "' not found for item " + id);
				}
			}
		}

		if (maxDamage > 0) {
			properties.durability(maxDamage);
		} else if (maxStackSize != -1) {
			properties.stacksTo(maxStackSize);
		}

		if (rarity != null) {
			properties.rarity(rarity);
		}

		var item = containerItem == null ? Items.AIR : ItemWrapper.getItem(containerItem);

		if (item != Items.AIR) {
			properties.craftRemainder(item);
		}

		// TODO: rework into consumable!
		if (foodBuilder != null) {
			foodBuilder.applyTo(properties);
		}

		if (fireResistant) {
			properties.fireResistant();
		}

		if (tool != null) {
			properties.component(DataComponents.TOOL, tool);
		}

		if (itemAttributeModifiers != null) {
			properties.attributes(itemAttributeModifiers);
		}

		if (jukeboxSong != null) {
			properties.jukeboxPlayable(jukeboxSong);
		}

		if (!canRepair) {
			properties.setNoCombineRepair();
		}

		if (compostable != null) {
			properties.component(DataComponents.COMPOSTABLE, compostable);
		}

		if (cookingFuel != null) {
			properties.component(DataComponents.COOKING_FUEL, cookingFuel);
		}

		return properties;
	}
}
