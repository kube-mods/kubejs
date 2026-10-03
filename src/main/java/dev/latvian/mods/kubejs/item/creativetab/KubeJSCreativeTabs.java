package dev.latvian.mods.kubejs.item.creativetab;

import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public interface KubeJSCreativeTabs {
	DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KubeJS.MOD_ID);

	Supplier<CreativeModeTab> TAB = REGISTRY.register("tab", () -> CreativeModeTab.builder()
		.title(CommonProperties.get().getCreativeModeTabName())
		.icon(() -> {
			var is = ItemStack.OPTIONAL_CODEC.parse(RegistryAccessContainer.BUILTIN.json(), CommonProperties.get().creativeModeTabIcon).result().orElse(ItemStack.EMPTY);
			return is.isEmpty() ? Items.DYE.purple().getDefaultInstance() : is;
		})
		.displayItems((params, output) -> {
			for (var b : RegistryObjectStorage.ITEM) {
				output.accept(b.get().getDefaultInstance());
			}
		})
		.build()
	);
}
