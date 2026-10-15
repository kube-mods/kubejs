package dev.latvian.mods.kubejs.util;

import dev.latvian.mods.rhino.Context;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.stream.Stream;

public class Tags {
	public static TagKey<Item> item(Identifier id) {
		return generic(id, Registries.ITEM);
	}

	public static TagKey<Block> block(Identifier id) {
		return generic(id, Registries.BLOCK);
	}

	public static TagKey<Fluid> fluid(Identifier id) {
		return generic(id, Registries.FLUID);
	}

	public static TagKey<EntityType<?>> entityType(Identifier id) {
		return generic(id, Registries.ENTITY_TYPE);
	}

	public static TagKey<Biome> biome(Identifier id) {
		return generic(id, Registries.BIOME);
	}

	public static Stream<TagKey<Item>> byItemStack(ItemStack stack) {
		return byItem(stack.getItem());
	}

	public static Stream<TagKey<Item>> byItem(Item item) {
		return forHolder(item.builtInRegistryHolder());
	}

	public static Stream<TagKey<Block>> byBlockState(BlockState state) {
		return byBlock(state.getBlock());
	}

	public static Stream<TagKey<Block>> byBlock(Block block) {
		return forHolder(block.builtInRegistryHolder());
	}

	public static Stream<TagKey<Fluid>> byFluid(Fluid fluid) {
		return forHolder(fluid.builtInRegistryHolder());
	}

	public static Stream<TagKey<EntityType<?>>> byEntity(Entity entity) {
		return byEntityType(entity.getType());
	}

	public static Stream<TagKey<EntityType<?>>> byEntityType(EntityType<?> entityType) {
		return forHolder(entityType.builtInRegistryHolder());
	}

	public static <T> Stream<TagKey<T>> forType(Context cx, T object, Registry<T> registry) {
		return registry.getResourceKey(object)
			.flatMap(k -> registry.get(k.identifier()))
			.stream()
			.flatMap(Holder::tags);
	}


	private static <T> TagKey<T> generic(Identifier id, ResourceKey<Registry<T>> registry) {
		return TagKey.create(registry, id);
	}

	private static <T> Stream<TagKey<T>> forHolder(Holder.Reference<T> registryHolder) {
		return registryHolder.tags();
	}
}
