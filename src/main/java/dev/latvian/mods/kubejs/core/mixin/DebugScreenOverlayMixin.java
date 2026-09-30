package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.latvian.mods.kubejs.client.DebugInfoKubeEvent;
import dev.latvian.mods.kubejs.plugin.builtin.event.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V",
			ordinal = 0
		)
	)
	private void kubejs$debugText(GuiGraphicsExtractor graphics, CallbackInfo ci, @Local(ordinal = 0) List<String> leftLines, @Local(ordinal = 1) List<String> rightLines) {
		var mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}

		if (ClientEvents.DEBUG_LEFT.hasListeners()) {
			ClientEvents.DEBUG_LEFT.post(new DebugInfoKubeEvent(mc.player, leftLines));
		}

		if (ClientEvents.DEBUG_RIGHT.hasListeners()) {
			ClientEvents.DEBUG_RIGHT.post(new DebugInfoKubeEvent(mc.player, rightLines));
		}
	}
}
