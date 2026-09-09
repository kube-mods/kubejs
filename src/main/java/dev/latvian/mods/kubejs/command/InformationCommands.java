package dev.latvian.mods.kubejs.command;

import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.ingredient.NamespaceIngredient;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;

public class InformationCommands {
	private static Component copy(String s, ChatFormatting col, String info) {
		return copy(Component.literal(s).withStyle(col), Component.literal(info));
	}

	private static Component copy(String s, ChatFormatting col, Component info) {
		return copy(Component.literal(s).withStyle(col), info);
	}

	private static Component copy(Component c, Component info) {
		String textToCopy = c.getString();
		
		if (textToCopy.startsWith("'") && textToCopy.endsWith("'") && CommonProperties.get().useDoubleQuotes) {
			textToCopy = "\"" + textToCopy.substring(1, textToCopy.length() - 1) + "\"";
		}
		
		return Component.literal("- ")
			.withStyle(ChatFormatting.GRAY)
			.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, textToCopy)))
			.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, info.copy().append(" (Click to copy)"))))
			.append(c);
	}

	public static int hand(ServerPlayer player, InteractionHand hand) {
		player.sendSystemMessage(Component.literal("Item in hand:"));
		var stack = player.getItemInHand(hand);
		var holder = stack.getItemHolder();
		var itemRegistry = player.server.registryAccess().registry(Registries.ITEM).orElseThrow();
		var blockRegistry = player.server.registryAccess().registry(Registries.BLOCK).orElseThrow();
		var fluidRegistry = player.server.registryAccess().registry(Registries.FLUID).orElseThrow();
		var tabRegistry = player.server.registryAccess().registry(Registries.CREATIVE_MODE_TAB).orElseThrow();

		// item info
		// id
		player.sendSystemMessage(copy(stack.kjs$toItemString0(player.server.registryAccess().createSerializationContext(NbtOps.INSTANCE)), ChatFormatting.GREEN, "Item ID"));
		// item tags
		var itemTags = holder.tags().toList();
		for (var tag : itemTags) {
			var id = "'#%s'".formatted(tag.location());
			var size = itemRegistry.getTag(tag).map(HolderSet::size).orElse(0);
			player.sendSystemMessage(copy(id, ChatFormatting.YELLOW, "Item Tag [" + size + " items]"));
		}
		// mod
		player.sendSystemMessage(copy("'@" + stack.kjs$getMod() + "'", ChatFormatting.AQUA, "Mod [" + new NamespaceIngredient(stack.kjs$getMod()).toVanilla().kjs$getStacks().size() + " items]"));

		// creative tab
		for (var tab : tabRegistry) {
			if (tab.contains(stack)) {
				var id = tabRegistry.getKey(tab);
				var count = tab.getDisplayItems().size();
				var searchCount = tab.getSearchTabDisplayItems().size();

				player.sendSystemMessage(copy("'%" + id + "'", ChatFormatting.LIGHT_PURPLE, tab.getDisplayName().copy().append(" [%d/%d items in tab / search tab]".formatted(count, searchCount))));
			}
		}

		// block info
		if (stack.getItem() instanceof BlockItem blockItem) {
			player.sendSystemMessage(Component.literal("Held block:"));
			var block = blockItem.getBlock();
			var blockHolder = block.builtInRegistryHolder();
			// id
			player.sendSystemMessage(copy("'" + block.kjs$getId() + "'", ChatFormatting.GREEN, "Block ID"));
			// block tags
			var blockTags = blockHolder.tags().toList();
			for (var tag : blockTags) {
				var id = "'#%s'".formatted(tag.location());
				var size = blockRegistry.getTag(tag).map(HolderSet::size).orElse(0);
				player.sendSystemMessage(copy(id, ChatFormatting.YELLOW, "Block Tag [" + size + " items]"));
			}
		}

		// fluid info
		var containedFluid = FluidUtil.getFluidContained(stack);
		if (containedFluid.isPresent()) {
			player.sendSystemMessage(Component.literal("Held fluid:"));
			var fluid = containedFluid.orElseThrow();
			var fluidHolder = fluid.getFluid().builtInRegistryHolder();
			// id
			player.sendSystemMessage(copy(fluidHolder.key().location().toString(), ChatFormatting.GREEN, "Fluid ID"));
			// fluid tags
			var fluidTags = fluidHolder.tags().toList();
			for (var tag : fluidTags) {
				var id = "'#%s'".formatted(tag.location());
				var size = fluidRegistry.getTag(tag).map(HolderSet::size).orElse(0);
				player.sendSystemMessage(copy(id, ChatFormatting.YELLOW, "Fluid Tag [" + size + " items]"));
			}
		}

		return 1;
	}

	public static int inventory(ServerPlayer player) {
		return dump(player.getInventory().items, player, "Inventory");
	}

	public static int hotbar(ServerPlayer player) {
		return dump(player.getInventory().items.subList(0, 9), player, "Hotbar");
	}

	public static int dump(List<ItemStack> stacks, ServerPlayer player, String name) {
		var ops = player.server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		var dump = stacks.stream().filter(is -> !is.isEmpty()).map(is -> is.kjs$toItemString0(ops)).toList();
		player.sendSystemMessage(copy(dump.toString(), ChatFormatting.WHITE, name + " Item List"));
		return 1;
	}
}
