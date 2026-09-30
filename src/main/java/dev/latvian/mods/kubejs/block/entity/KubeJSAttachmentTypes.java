package dev.latvian.mods.kubejs.block.entity;

import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class KubeJSAttachmentTypes {
	public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, KubeJS.MOD_ID);

	public static final Supplier<AttachmentType<Map<String, Integer>>> ENERGY = REGISTRY.register("energy",
		() -> AttachmentType.<Map<String, Integer>>builder(() -> new HashMap<>())
			.serialize(new IAttachmentSerializer<>() {
				@Override
				public Map<String, Integer> read(IAttachmentHolder holder, ValueInput input) {
					var map = new HashMap<String, Integer>();
					for (var key : input.keySet()) {
						input.read(key, Codec.INT).ifPresent(val -> map.put(key, val));
					}
					return map;
				}

				@Override
				public boolean write(Map<String, Integer> map, ValueOutput output) {
					boolean anyWritten = false;
					for (var entry : map.entrySet()) {
						if (entry.getValue() > 0) {
							output.store(entry.getKey(), Codec.INT, entry.getValue());
							anyWritten = true;
						}
					}
					return anyWritten;
				}
			})
			.build());

	public static final Supplier<AttachmentType<Map<String, FluidStack>>> FLUID = REGISTRY.register("fluid",
		() -> AttachmentType.<Map<String, FluidStack>>builder(() -> new HashMap<>())
			.serialize(new IAttachmentSerializer<>() {
				@Override
				public Map<String, FluidStack> read(IAttachmentHolder holder, ValueInput input) {
					var map = new HashMap<String, FluidStack>();
					for (var key : input.keySet()) {
						input.read(key, FluidStack.OPTIONAL_CODEC).ifPresent(val -> map.put(key, val));
					}
					return map;
				}

				@Override
				public boolean write(Map<String, FluidStack> map, ValueOutput output) {
					boolean anyWritten = false;
					for (var entry : map.entrySet()) {
						if (!entry.getValue().isEmpty()) {
							output.store(entry.getKey(), FluidStack.OPTIONAL_CODEC, entry.getValue());
							anyWritten = true;
						}
					}
					return anyWritten;
				}
			})
			.build());

	public static final Supplier<AttachmentType<Map<String, NonNullList<ItemStack>>>> INVENTORY = REGISTRY.register("inventory",
		() -> AttachmentType.<Map<String, NonNullList<ItemStack>>>builder(() -> new HashMap<>())
			.serialize(new IAttachmentSerializer<>() {
				private static final Codec<NonNullList<ItemStack>> ITEM_LIST_CODEC = ItemStack.OPTIONAL_CODEC.listOf()
					.xmap(
						list -> {
							var nonnull = NonNullList.withSize(list.size(), ItemStack.EMPTY);
							for (int i = 0; i < list.size(); i++) {
								nonnull.set(i, list.get(i));
							}
							return nonnull;
						},
						list -> list.stream().toList()
					);

				@Override
				public Map<String, NonNullList<ItemStack>> read(IAttachmentHolder holder, ValueInput input) {
					var map = new HashMap<String, NonNullList<ItemStack>>();
					for (var key : input.keySet()) {
						input.read(key, ITEM_LIST_CODEC).ifPresent(val -> map.put(key, val));
					}
					return map;
				}

				@Override
				public boolean write(Map<String, NonNullList<ItemStack>> map, ValueOutput output) {
					boolean anyWritten = false;
					for (var entry : map.entrySet()) {
						boolean hasItems = false;
						for (var stack : entry.getValue()) {
							if (!stack.isEmpty()) {
								hasItems = true;
								break;
							}
						}
						if (hasItems) {
							output.store(entry.getKey(), ITEM_LIST_CODEC, entry.getValue());
							anyWritten = true;
						}
					}
					return anyWritten;
				}
			})
			.build());
}
