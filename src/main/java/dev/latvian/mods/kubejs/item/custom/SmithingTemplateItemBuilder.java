package dev.latvian.mods.kubejs.item.custom;

import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.item.SmithingTemplateItem.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ReturnsSelf
public class SmithingTemplateItemBuilder extends ItemBuilder {

	private static final List<Identifier> ARMOR_ICONS = SmithingTemplateItem.createTrimmableArmorIconList();
	private static final List<Identifier> INGOTS_AND_CRYSTALS_ICONS = SmithingTemplateItem.createTrimmableMaterialIconList();
	private static final List<Identifier> EQUIPMENT_ICONS = SmithingTemplateItem.createNetheriteUpgradeIconList();
	private static final List<Identifier> TOOL_ICONS = List.of(EMPTY_SLOT_HOE, EMPTY_SLOT_AXE, EMPTY_SLOT_SWORD, EMPTY_SLOT_SHOVEL, EMPTY_SLOT_PICKAXE);
	private static final List<Identifier> CRYSTAL_ICONS = List.of(EMPTY_SLOT_REDSTONE_DUST, EMPTY_SLOT_QUARTZ, EMPTY_SLOT_EMERALD, EMPTY_SLOT_DIAMOND, EMPTY_SLOT_LAPIS_LAZULI, EMPTY_SLOT_AMETHYST_SHARD);

	private final Map<String, String> translations = new HashMap<>();
	public Component appliesToText = Component.literal("set with .appliesTo(string) on your smithing_template type item").withStyle(ChatFormatting.BLUE);
	public Component ingredientsText = Component.literal("set with .ingredients(string) on your smithing_template type item").withStyle(ChatFormatting.BLUE);
	public Component appliesToSlotDescriptionText = Component.literal("set with .appliesToSlotDescription(string) on your smithing_template type item");
	public Component ingredientSlotDescriptionText = Component.literal("set with .ingredientsSlotDescription(string) on your smithing_template type item");
	public final List<Identifier> appliesToEmptyIcons = new ArrayList<>();
	public final List<Identifier> ingredientsSlotEmptyIcons = new ArrayList<>();

	public SmithingTemplateItemBuilder(Identifier i) {
		super(i);
	}

	@Info("""
		Sets the description text that shows in the item tooltip to describe what it can be applied to.
		Using 'Armor' or 'Diamond Equipment' will use the vanilla language keys so it is translated into other languages automatically.
		THIS IS PURELY VISUAL
		
		If you wish to apply non standard formatting (like change the colour) set the `ingredientsText` field.
		""")
	public SmithingTemplateItemBuilder appliesTo(String text) {
		appliesToText = switch (text) {
			// reuse the existing translation keys if they match
			case "Armor" -> SmithingTemplateItem.ARMOR_TRIM_APPLIES_TO;
			case "Diamond Equipment" -> SmithingTemplateItem.NETHERITE_UPGRADE_APPLIES_TO;
			default -> defaultTranslateableTooltipComponent(text, "applies_to", true);
		};
		return this;
	}

	@Info("""
		Sets the description text that shows in the item tooltip to describe what ingredients can be added.
		Using 'Ingots & Crystals' or 'Netherite Ingot' will use the vanilla language keys so it is translated into other languages automatically.
		THIS IS PURELY VISUAL
		
		If you wish to apply non standard formatting (like change the colour) set the `ingredientsText` field.
		""")
	public SmithingTemplateItemBuilder ingredients(String text) {
		ingredientsText = switch (text) {
			// reuse the existing translation keys if they match
			case "Ingots and Crystals", "Ingots & Crystals" -> SmithingTemplateItem.ARMOR_TRIM_INGREDIENTS;
			case "Netherite Ingot" -> SmithingTemplateItem.NETHERITE_UPGRADE_INGREDIENTS;
			default -> defaultTranslateableTooltipComponent(text, "ingredients", true);
		};
		return this;
	}

	@Info("""
		Sets the description text that shows when you hover over the base item slot when this item is put in smithing table as a template.
		Using 'Add a piece of armor' or 'Add diamond armor, weapon, or tool' will use the vanilla language keys so it is translated into other languages automatically.
		
		If you wish to apply non standard formatting (like change the colour) set the `appliesToSlotDescriptionText` field.
		""")
	public SmithingTemplateItemBuilder appliesToSlotDescription(String text) {
		appliesToSlotDescriptionText = switch (text) {
			// reuse the existing translation keys if they match
			case "Add a piece of armor" -> SmithingTemplateItem.ARMOR_TRIM_BASE_SLOT_DESCRIPTION;
			case "Add diamond armor, weapon, or tool" -> SmithingTemplateItem.NETHERITE_UPGRADE_BASE_SLOT_DESCRIPTION;
			default -> defaultTranslateableTooltipComponent(text, "base_slot_description", false);
		};
		return this;
	}

	@Info("""
		Sets the description text that shows when you hover over the ingredient slot when this item is put in smithing table as a template.
		Using 'Add ingot or crystal' or 'Add Netherite Ingot' will use the vanilla language keys so it is translated into other languages automatically.
		
		If you wish to apply non standard formatting (like change the colour) set the `ingredientSlotDescriptionText` field.
		""")
	public SmithingTemplateItemBuilder ingredientsSlotDescription(String text) {
		ingredientSlotDescriptionText = switch (text) {
			// reuse the existing translation keys if they match
			case "Add ingot or crystal" -> SmithingTemplateItem.ARMOR_TRIM_BASE_SLOT_DESCRIPTION;
			case "Add Netherite Ingot" -> SmithingTemplateItem.NETHERITE_UPGRADE_BASE_SLOT_DESCRIPTION;
			default -> defaultTranslateableTooltipComponent(text, "ingredient_slot_description", false);
		};
		return this;
	}

	@Info("Adds the specified texture location to the list of base slot icons that the smithing table cycles through when this smithing template is put in.")
	public SmithingTemplateItemBuilder addAppliesToSlotIcon(Identifier location) {
		appliesToEmptyIcons.add(location);
		return this;
	}

	@Info("Adds the specified texture location to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder addIngredientsSlotIcon(Identifier location) {
		ingredientsSlotEmptyIcons.add(location);
		return this;
	}

	@Info("Adds all armor icons to the list of base slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder armorIcons() {
		appliesToEmptyIcons.addAll(ARMOR_ICONS);
		return this;
	}

	@Info("Adds all armor and basic tool icons to the list of base slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder equipmentIcons() {
		appliesToEmptyIcons.addAll(EQUIPMENT_ICONS);
		return this;
	}

	@Info("Adds all basic tool icons to the list of base slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder toolIcons() {
		appliesToEmptyIcons.addAll(TOOL_ICONS);
		return this;
	}

	@Info("Adds an ingot, dust, diamond, emerald, quartz, lapis lazuli and amethyst shard icons to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder ingotAndCrystalIcons() {
		ingredientsSlotEmptyIcons.addAll(INGOTS_AND_CRYSTALS_ICONS);
		return this;
	}

	@Info("Adds a dust, diamond, emerald, quartz, lapis lazuli and amethyst shard icons to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder crystalIcons() {
		ingredientsSlotEmptyIcons.addAll(CRYSTAL_ICONS);
		return this;
	}

	@Info("Adds an ingot to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder ingotIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_INGOT);
	}

	@Info("Adds a dust to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder dustIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_REDSTONE_DUST);
	}

	@Info("Adds an amethyst shard to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder shardIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_AMETHYST_SHARD);
	}

	@Info("Adds a diamond to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder diamondIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_DIAMOND);
	}

	@Info("Adds an emerald to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder emeraldIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_EMERALD);
	}

	@Info("Adds a quartz to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder quartzIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_QUARTZ);
	}

	@Info("Adds a lapis lazuli to the list of ingredient slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder lapisIcon() {
		return addIngredientsSlotIcon(EMPTY_SLOT_LAPIS_LAZULI);
	}

	@Info("Adds a sword to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder swordIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_SWORD);
	}

	@Info("Adds a shovel to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder shovelIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_SHOVEL);
	}

	@Info("Adds a axe to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder axeIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_AXE);
	}

	@Info("Adds a pickaxe to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder pickaxeIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_PICKAXE);
	}

	@Info("Adds a hoe to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder hoeIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_HOE);
	}

	@Info("Adds a helmet to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder helmetIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_HELMET);
	}

	@Info("Adds a chestplate to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder chestplateIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_CHESTPLATE);
	}

	@Info("Adds leggings to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder leggingsIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_LEGGINGS);
	}

	@Info("Adds boots to the list of base item slot icons that the smithing table cycles through when this smithing template is put in")
	public SmithingTemplateItemBuilder bootsIcon() {
		return addAppliesToSlotIcon(EMPTY_SLOT_BOOTS);
	}

	private Component defaultTranslateableTooltipComponent(String text, String type, boolean tooltipDescription) {
		String translationKey = makeTooltipDescriptionId(type);
		translations.put(translationKey, text);
		MutableComponent component = Component.translatable(translationKey);
		if (tooltipDescription) {
			component.withStyle(DESCRIPTION_FORMAT);
		}
		return component;
	}

	private String makeTooltipDescriptionId(String type) {
		return getTranslationKeyGroup() + '.' + id.getNamespace() + ".smithing_template." + id.getPath() + '.' + type;
	}

	@Override // override so we can add @Info
	@Info("""
		Sets the name for this smithing template.
		Note that the normal display name for all smithing templates is the same and cannot be changed, this instead sets the name in the tooltip (see vanilla smithing templates for what this looks like).
		
		This will be overridden by a lang file if it exists.
		""")
	public SmithingTemplateItemBuilder displayName(Component name) {
		super.displayName(name.copy().withStyle(TITLE_FORMAT));
		return this;
	}

	@Override
	public void generateLang(LangKubeEvent lang) {
		// call super as we still use the display name for the 'upgrade description'
		// we don't use a custom lang key for that as vanillas format depends on it being an upgrade or trim, and we don't know which it is
		super.generateLang(lang);
		lang.addAll(id.getNamespace(), translations);
	}

	@Override
	public SmithingTemplateItem createObject() {
		return new SmithingTemplateItem(
			appliesToText,
			ingredientsText,
			appliesToSlotDescriptionText,
			ingredientSlotDescriptionText,
			appliesToEmptyIcons,
			ingredientsSlotEmptyIcons,
			createItemProperties()
		);
	}
}
