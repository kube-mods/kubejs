package dev.latvian.mods.kubejs.core.mixin;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.SpecialEquality;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Identifier.class, priority = 1001)
public abstract class IdentifierMixin implements SpecialEquality {
	@Override
	public boolean specialEquals(Context cx, @Nullable Object o, boolean shallow) {
		return switch (o) {
			case Identifier _ -> equals(o);
			case null, default -> toString().equals(String.valueOf(o));
		};
	}
}
