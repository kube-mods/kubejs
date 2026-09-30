package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.Context;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.UniformGenerator;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.mojang.serialization.DataResult.error;
import static com.mojang.serialization.DataResult.success;
import static dev.latvian.mods.kubejs.plugin.builtin.wrapper.StringUtilsWrapper.tryParseFloat;

public interface FloatWrapper {
	static ContextFloatProvider wrapContextFloatProvider(Context cx, @Nullable Object o) {
		return tryWrapContextFloatProvider(cx, o)
			.getOrThrow(error -> new KubeRuntimeException("Failed to read ContextFloatProvider from %s: %s".formatted(o, error))
				.source(SourceLine.of(cx)));
	}

	private static DataResult<ContextFloatProvider> contextFloatProviderFromMap(Context cx, Map<String, Object> m) {
		if (m.containsKey("min") && m.containsKey("max")) {
			return tryParseFloat(m.get("min")).apply2(FloatWrapper::toUniformGenerator, tryParseFloat(m.get("max")));
		} else if (m.containsKey("value")) {
			return tryParseFloat(m.get("value")).map(f -> toUniformGenerator(f, f));
		}

		return error(() -> "Invalid ContextFloatProvider map %s. Expected {min,max} or {value}.".formatted(m));
	}

	private static DataResult<ContextFloatProvider> tryWrapContextFloatProvider(Context cx, @Nullable Object o) {
		return switch (o) {
			case Number n -> success(toUniformGenerator(n.floatValue(), n.floatValue()));
			case List<?> list -> switch (list.size()) {
				case 0 -> error(() -> "list cannot be empty");
				case 1 -> tryParseFloat(list.get(0)).map(v -> (ContextFloatProvider) toUniformGenerator(v, v));
				case 2 -> tryParseFloat(list.get(0)).apply2(
					(a, b) -> (ContextFloatProvider) toUniformGenerator(a, b),
					tryParseFloat(list.get(1)));
				default -> error(() -> "list can contain at most 2 numbers");
			};
			case Map<?, ?> map -> contextFloatProviderFromMap(cx, Cast.to(map));
			case null, default -> error(() -> "Expected a number, list of numbers, or a supported map format");
		};
	}

	private static Holder<ContextFloatProvider> constHolder(float f) {
		return Holder.direct(new ConstantValue(f));
	}

	private static UniformGenerator toUniformGenerator(float min, float max) {
		return new UniformGenerator(constHolder(min), constHolder(max));
	}
}
