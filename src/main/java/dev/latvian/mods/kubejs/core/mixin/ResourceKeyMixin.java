package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.latvian.mods.rhino.util.SpecialEquality;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.latvian.mods.kubejs.registry.RegistryType.Scanner;

@RemapPrefixForJS("kjs$")
@Mixin(value = ResourceKey.class, priority = 1001)
public abstract class ResourceKeyMixin implements SpecialEquality {
	@Shadow
	@Final
	private Identifier identifier;

	@Unique
	public String kjs$getNamespace() {
		return identifier.getNamespace();
	}

	@Unique
	public String kjs$getPath() {
		return identifier.getPath();
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void kjs$getKeyStackTraces(Identifier registryName, Identifier identifier, CallbackInfo ci) {
		Scanner.scan(registryName, identifier);
	}

	@Override
	public boolean specialEquals(Context cx, @Nullable Object o, boolean shallow) {
		return switch (o) {
			case null -> false;
			case ResourceKey<?> _ -> o == this;
			case Identifier id -> identifier.equals(id);
			default -> identifier.toString().equals(o.toString());
		};
	}
}
