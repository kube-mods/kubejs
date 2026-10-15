package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.color.KubeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatFormatting.class)
public abstract class ChatFormattingMixin implements KubeColor {
	@Override
	public int kjs$getARGB() {
		TextColor color = TextColor.fromLegacyFormat((ChatFormatting) (Object) this);
		return color == null ? 0xFF000000 : (0xFF000000 | color.getValue());
	}

	@Override
	public int kjs$getRGB() {
		TextColor color = TextColor.fromLegacyFormat((ChatFormatting) (Object) this);
		return color == null ? 0 : color.getValue();
	}
}