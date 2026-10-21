package dev.latvian.mods.kubejs.util.registrypredicate;

import net.minecraft.core.Holder;

public record RegistryHolderPredicate<T>(Holder<T> value) implements RegistryPredicate<T> {
	@Override
	public boolean test(Holder<T> holder) {
		return holder.value() == value.value();
	}

	@Override
	public String toString() {
		try {
			if (value instanceof Holder.Reference<T> ref) {
				return ref.key().identifier().toString();
			} else {
				return value.key().identifier().toString();
			}
		} catch (Exception ex) {
			return String.valueOf(value.value());
		}
	}
}
