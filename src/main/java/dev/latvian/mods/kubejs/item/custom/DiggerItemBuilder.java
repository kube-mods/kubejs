package dev.latvian.mods.kubejs.item.custom;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.Tags;

import java.util.function.BiFunction;

public class DiggerItemBuilder extends HandheldItemBuilder {
	public final BiFunction<ToolMaterial, Item.Properties, Item> function;

	public DiggerItemBuilder(Identifier i, float d, float s, BiFunction<ToolMaterial, Item.Properties, Item> f) {
		super(i, d, s);
		function = f;
	}

	@Override
	public Item createObject() {
		var props = createItemProperties();
		var material = toolTier.build();
		itemAttributeModifiers = createToolAttributes(material, attackDamageBaseline, speedBaseline);
		return function.apply(material, props);
	}

	public static class Pickaxe extends DiggerItemBuilder {
		public static final Identifier[] PICKAXE_TAGS = {
			ItemTags.PICKAXES.location(),
			ItemTags.CLUSTER_MAX_HARVESTABLES.location(),
			Tags.Items.MINING_TOOL_TOOLS.location(),
		};

		public static final Identifier PICKAXE_MODEL = Identifier.withDefaultNamespace("item/iron_pickaxe");

		public Pickaxe(Identifier i) {
			super(i, 1F, -2.8F, (material, props) -> new Item(props.pickaxe(material, 1F, -2.8F)));
			parentModel = PICKAXE_MODEL;
			tag(PICKAXE_TAGS);
		}
	}

	public static class Shovel extends DiggerItemBuilder {
		public static final Identifier[] SHOVEL_TAGS = {
			ItemTags.SHOVELS.location(),
		};

		public static final Identifier SHOVEL_MODEL = Identifier.withDefaultNamespace("item/iron_shovel");

		public Shovel(Identifier i) {
			super(i, 1.5F, -3F, (material, props) -> new Item(props.shovel(material, 1.5F, -3F)));
			parentModel = SHOVEL_MODEL;
			tag(SHOVEL_TAGS);
		}
	}

	public static class Axe extends DiggerItemBuilder {
		public static final Identifier[] AXE_TAGS = {
			ItemTags.AXES.location(),
		};

		public static final Identifier AXE_MODEL = Identifier.withDefaultNamespace("item/iron_axe");

		public Axe(Identifier i) {
			super(i, 6F, -3.1F, (material, props) -> new Item(props.axe(material, 6F, -3.1F)));
			parentModel = AXE_MODEL;
			tag(AXE_TAGS);
		}
	}

	public static class Hoe extends DiggerItemBuilder {
		public static final Identifier[] HOE_TAGS = {
			ItemTags.HOES.location(),
		};

		public static final Identifier HOE_MODEL = Identifier.withDefaultNamespace("item/iron_hoe");

		public Hoe(Identifier i) {
			super(i, 0F, -3F, (material, props) -> new Item(props.hoe(material, 0F, -3F)));
			parentModel = HOE_MODEL;
			tag(HOE_TAGS);
		}
	}
}
