package dev.latvian.mods.kubejs.util.registrypredicate;

import dev.latvian.mods.kubejs.util.RegExpKJS;
import net.minecraft.core.Holder;

import java.util.regex.Pattern;

public record RegistryRegExpPredicate<T>(Pattern pattern) implements RegistryPredicate<T> {
	@Override
	public boolean test(Holder<T> holder) {
		try {
			if (holder instanceof Holder.Reference<T> ref) {
				return pattern.matcher(ref.key().identifier().toString()).find();
			} else {
				return pattern.matcher(holder.key().identifier().toString()).find();
			}
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public String toString() {
		return RegExpKJS.toRegExpString(pattern);
	}
}
