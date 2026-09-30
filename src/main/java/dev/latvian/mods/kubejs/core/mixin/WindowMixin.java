package dev.latvian.mods.kubejs.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import dev.latvian.mods.kubejs.core.WindowKJS;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(Window.class)
public class WindowMixin implements WindowKJS {
	@ModifyExpressionValue(method = "setIcon(Lnet/minecraft/server/packs/PackMetadataResources;Lcom/mojang/blaze3d/platform/IconSet;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/IconSet;getStandardIcons(Lnet/minecraft/server/packs/PackMetadataResources;)Ljava/util/List;"))
	private List<IoSupplier<InputStream>> kjs$icons(List<IoSupplier<InputStream>> original) throws IOException {
		return kjs$loadIcons(original);
	}
}