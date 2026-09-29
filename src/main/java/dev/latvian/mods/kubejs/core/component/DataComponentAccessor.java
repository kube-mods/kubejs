package dev.latvian.mods.kubejs.core.component;

import dev.latvian.mods.kubejs.color.KubeColor;
import dev.latvian.mods.kubejs.component.CustomModelDataType;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.latvian.mods.kubejs.component.DataComponentWrapper.tryWrapComponent;

/// A generic readable and writable accessor for data components.
/// Contains various helpers for getters and setters.
@RemapPrefixForJS("kjs$")
public interface DataComponentAccessor extends DataComponentGetter, DataComponentSetter {
	// Getters //
	default CompoundTag kjs$getCustomData() {
		return getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	@Nullable
	default Component kjs$getCustomName() {
		return get(DataComponents.CUSTOM_NAME);
	}

	default CustomModelData kjs$getCustomModelData() {
		return getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
	}

	// Setters //
	default void kjs$setCustomData(CompoundTag tag) {
		if (tag.isEmpty()) {
			kjs$remove(DataComponents.CUSTOM_DATA);
		} else {
			kjs$override(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	default void kjs$setRarity(Rarity rarity) {
		kjs$override(DataComponents.RARITY, rarity);
	}

	default void kjs$setCustomName(@Nullable Component name) {
		if (name != null) {
			kjs$override(DataComponents.CUSTOM_NAME, name);
		} else {
			kjs$remove(DataComponents.CUSTOM_NAME);
		}
	}

	default void kjs$setLore(List<Component> lines) {
		kjs$override(DataComponents.LORE, new ItemLore(List.copyOf(lines)));
	}

	default void kjs$setLore(List<Component> lines, List<Component> styledLines) {
		kjs$override(DataComponents.LORE, new ItemLore(List.copyOf(lines), List.copyOf(styledLines)));
	}

	default void kjs$setCustomModelData(CustomModelDataType type, List<?> values) {
		CustomModelData existing = getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
		kjs$override(DataComponents.CUSTOM_MODEL_DATA, type.apply(existing, values));
	}

	default void kjs$setTooltipHidden() {
		TooltipDisplay display = getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
		kjs$override(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, display.hiddenComponents()));
	}

	default void kjs$setAdditionalTooltipHidden() {
		TooltipDisplay display = getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);

		display = display.withHidden(DataComponents.BANNER_PATTERNS, true);
		display = display.withHidden(DataComponents.BEES, true);
		display = display.withHidden(DataComponents.BLOCK_ENTITY_DATA, true);
		display = display.withHidden(DataComponents.BLOCK_STATE, true);
		display = display.withHidden(DataComponents.WRITTEN_BOOK_CONTENT, true);

		kjs$override(DataComponents.TOOLTIP_DISPLAY, display);
	}

	default void kjs$setGlintOverride(boolean override) {
		kjs$override(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, override);
	}

	default void kjs$setDyedColor(KubeColor color) {
		kjs$override(DataComponents.DYED_COLOR, new DyedItemColor(color.kjs$getRGB()));
	}

	default void kjs$setDyedColorWithTooltip(KubeColor color) {
		kjs$override(DataComponents.DYED_COLOR, new DyedItemColor(color.kjs$getRGB()));
	}

	default void kjs$setPotionContents(PotionContents contents) {
		kjs$override(DataComponents.POTION_CONTENTS, contents);
	}

	default void kjs$setPotionId(Holder<Potion> potion) {
		kjs$override(DataComponents.POTION_CONTENTS, new PotionContents(potion));
	}

	default void kjs$setEntityData(CompoundTag tag) {
		if (tag.isEmpty()) {
			kjs$remove(DataComponents.ENTITY_DATA);
			return;
		}

		var id = tag.getString("id");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("ENTITY_DATA tag must contain non-empty \"id\"");
		}

		var type = EntityType.by(id.get()).orElseThrow(() -> new IllegalArgumentException("Unknown entity id: " + id));
		kjs$override(DataComponents.ENTITY_DATA, TypedEntityData.of(type, tag));
	}

	default void kjs$setProfile(com.mojang.authlib.GameProfile profile) {
		kjs$override(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
	}

	default void kjs$setProfile(@Nullable String name, @Nullable UUID uuid) {
		var cleanName = name != null && name.isBlank() ? null : name;
		var cleanUuid = uuid != null && uuid.getLeastSignificantBits() == 0L && uuid.getMostSignificantBits() == 0L ? null : uuid;

		if (cleanUuid != null) {
			kjs$override(DataComponents.PROFILE, ResolvableProfile.createUnresolved(cleanUuid));
		} else if (cleanName != null) {
			kjs$override(DataComponents.PROFILE, ResolvableProfile.createUnresolved(cleanName));
		} else {
			kjs$remove(DataComponents.PROFILE);
		}
	}

	default void kjs$setBaseColor(DyeColor color) {
		kjs$override(DataComponents.BASE_COLOR, color);
	}

	default void kjs$setBlockStateProperties(Map<String, String> properties) {
		kjs$override(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties));
	}

	default void kjs$setLockCode(ItemPredicate lock) {
		kjs$override(DataComponents.LOCK, new LockCode(lock));
	}

	default void kjs$setContainerLootTable(ResourceKey<LootTable> lootTable) {
		kjs$override(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootTable, 0L));
	}

	default void kjs$setContainerLootTable(ResourceKey<LootTable> lootTable, long seed) {
		kjs$override(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootTable, seed));
	}

	default void kjs$set(Context cx, DataComponentType<?> component, Object value) {
		var wrapped = tryWrapComponent(cx, component, value)
			.getOrThrow(msg ->
				new dev.latvian.mods.kubejs.error.KubeRuntimeException("Failed to wrap data component %s from '%s': %s".formatted(component, value, msg))
					.source(dev.latvian.mods.kubejs.script.SourceLine.of(cx))
			);

		if (wrapped.isPresent()) {
			kjs$override(component, Cast.to(wrapped.get()));
		} else {
			kjs$remove(component);
		}
	}

	default String kjs$getComponentString(Context cx) {
		return "[]";
	}
}
