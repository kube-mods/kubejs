package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.Context;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.mojang.serialization.DataResult.error;
import static com.mojang.serialization.DataResult.success;
import static dev.latvian.mods.kubejs.plugin.builtin.wrapper.StringUtilsWrapper.tryParseInt;

public interface IntWrapper {
	static ContextIntProvider wrapContextIntProvider(Context cx, @Nullable Object o) {
		return tryWrapContextIntProvider(cx, o)
			.getOrThrow(error -> new KubeRuntimeException("Failed to read ContextIntProvider from %s: %s".formatted(o, error))
			.source(SourceLine.of(cx)));
	}

	private static DataResult<ContextIntProvider> tryWrapContextIntProvider(Context cx, @Nullable Object o) {
		return switch (o) {
			case Number n -> success(toUniformGenerator(n.intValue(), n.intValue()));
			case List<?> list -> switch (list.size()) {
				case 0 -> error(() -> "list cannot be empty");
				case 1 -> tryParseInt(list.get(0)).map(v -> (ContextIntProvider) toUniformGenerator(v, v));
				case 2 -> tryParseInt(list.get(0)).apply2(
					(a, b) -> toUniformGenerator(a, b),
					tryParseInt(list.get(1))
				);
				default -> error(() -> "list can contain at most 2 numbers");
			};
			case Map<?, ?> map -> contextIntProviderFromMap(cx, Cast.to(map));
			case null, default -> error(() -> "Expected a number, list of numbers, or a supported map format");
		};
	}

	private static DataResult<ContextIntProvider> contextIntProviderFromMap(Context cx, Map<String, Object> m) {
		if (m.containsKey("min") && m.containsKey("max")) {
			return tryParseInt(m.get("min")).apply2(IntWrapper::toUniformGenerator, tryParseInt(m.get("max")));
		} else if (m.containsKey("value")) {
			return tryParseInt(m.get("value")).map(f -> toUniformGenerator(f, f));
		}

		return error(() -> "Invalid ContextIntProvider map %s. Expected {min,max} or {value}.".formatted(m));
	}

	private static Holder<ContextIntProvider> constHolder(int f) {
		return Holder.direct(new ConstantValue(f));
	}

	private static UniformGenerator toUniformGenerator(int min, int max) {
		return new UniformGenerator(constHolder(min), constHolder(max));
	}
}
