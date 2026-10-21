package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.core.LazyComponentKJS;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

@Mixin(CommandSourceStack.class)
@RemapPrefixForJS("kjs$")
public abstract class CommandSourceStackMixin {
	@Shadow
	@HideFromJS
	public abstract void sendSuccess(Supplier<Component> messageSupplier, boolean broadcast);

	@Unique
	public void kjs$sendSuccess(Component component, boolean broadcastToAdmins) {
		kjs$sendSuccessLazy(() -> component, broadcastToAdmins);
	}

	@Unique
	public void kjs$sendSuccessLazy(LazyComponentKJS component, boolean broadcastToAdmins) {
		sendSuccess(component, broadcastToAdmins);
	}
}
