package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.kubejs.client.KubeSessionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	//TODO postEffectId does not exist
	private @Nullable Identifier postEffectId;

	@Shadow
	//TODO setPostEffect does not exist
	public abstract void setPostEffect(Identifier identifier);

	@Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
	private void kjs$checkEntityPostEffect(@Nullable Entity cameraEntity, CallbackInfo ci) {
		var data = KubeSessionData.of(minecraft);
		if (data != null && data.activePostShader != null) {
			setPostEffect(data.activePostShader);
			ci.cancel();
		}
	}
}