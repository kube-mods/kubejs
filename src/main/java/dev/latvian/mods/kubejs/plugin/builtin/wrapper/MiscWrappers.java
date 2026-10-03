package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.level.LevelBlock;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.valueproviders.ClampedInt;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.util.valueproviders.ClampedNormalInt;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.mojang.serialization.DataResult.error;
import static com.mojang.serialization.DataResult.success;
import static dev.latvian.mods.kubejs.plugin.builtin.wrapper.StringUtilsWrapper.tryParseFloat;
import static dev.latvian.mods.kubejs.plugin.builtin.wrapper.StringUtilsWrapper.tryParseInt;

public interface MiscWrappers {
	static IntProvider wrapIntProvider(Context cx, @Nullable Object o) {
		return tryWrapIntProvider(cx, o)
			.getOrThrow(error -> new KubeRuntimeException("Failed to read IntProvider from %s: %s".formatted(o, error))
				.source(SourceLine.of(cx)));
	}

	static FloatProvider wrapFloatProvider(Context cx, @Nullable Object o) {
		return tryWrapFloatProvider(cx, o)
			.getOrThrow(error -> new KubeRuntimeException("Failed to read FloatProvider from %s: %s".formatted(o, error))
				.source(SourceLine.of(cx)));
	}

	static Vec3 wrapVec3(Context cx, @Nullable Object o) {
		return switch (o) {
			case Vec3 vec -> vec;
			case Position vec -> new Vec3(vec.x(), vec.y(), vec.z());
			case List<?> list when list.size() == 3 -> new Vec3(StringUtilsWrapper.parseDouble(list.get(0), 0), StringUtilsWrapper.parseDouble(list.get(1), 0), StringUtilsWrapper.parseDouble(list.get(2), 0));
			case List<?> list -> throw new KubeRuntimeException("Vec3 list requires 3 entries, got %s".formatted(list)).source(SourceLine.of(cx));
			case BlockPos pos -> new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			case Entity entity -> entity.position();
			case LevelBlock block -> new Vec3(block.getCenterX(), block.getCenterY(), block.getCenterZ());
			case null -> throw new KubeRuntimeException("Vec3 cannot be null!").source(SourceLine.of(cx));
			default -> throw new KubeRuntimeException("Invalid Vec3 input! Expected list, position, block position, entity or block, got %s.".formatted(o)).source(SourceLine.of(cx));
		};
	}

	static BlockPos wrapBlockPos(Context cx, @Nullable Object o) {
		return switch (o) {
			case BlockPos pos -> pos;
			case List<?> list when list.size() == 3 -> new BlockPos(StringUtilsWrapper.parseInt(list.get(0), 0), StringUtilsWrapper.parseInt(list.get(1), 0), StringUtilsWrapper.parseInt(list.get(2), 0));
			case List<?> list -> throw new KubeRuntimeException("BlockPos list requires 3 entries, got %s".formatted(list)).source(SourceLine.of(cx));
			case LevelBlock block -> block.getPos();
			case Position vec -> BlockPos.containing(vec.x(), vec.y(), vec.z());
			case null -> throw new KubeRuntimeException("BlockPos cannot be null!").source(SourceLine.of(cx));
			default -> throw new KubeRuntimeException("Invalid BlockPos input! Expected list, position or block, got %s.".formatted(o)).source(SourceLine.of(cx));
		};
	}

	private static DataResult<IntProvider> tryWrapIntProvider(Context cx, @Nullable Object o) {
		return switch (o) {
			case Number n -> success(ConstantInt.of(n.intValue()));
			case List<?> list -> switch (list.size()) {
				case 0 -> error(() -> "list cannot be empty");
				case 1 -> tryParseInt(list.get(0)).map(ConstantInt::of);
				case 2 -> tryParseInt(list.get(0)).apply2(MiscWrappers::toUniform, tryParseInt(list.get(1)));
				default -> error(() -> "list can contain at most 2 numbers");
			};
			case Map<?, ?> m -> {
				Map<String, Object> map = Cast.to(m);

				if (map.containsKey("clamped")) {
					yield tryWrapIntProvider(cx, map.get("clamped"))
						.apply2(MiscWrappers::toClamped, parseIntBounds(map));
				} else if (map.containsKey("clamped_normal")) {
					yield tryParseInt(map.get("mean"))
						.apply3(MiscWrappers::toClampedNormal, tryParseInt(map.get("deviation")), parseIntBounds(map));
				} else if (hasBounds(map)) {
					yield parseIntBounds(map).map(v -> v);
				} else {
					yield IntProviders.CODEC.parse(RegistryAccessContainer.of(cx).nbt(), NBTWrapper.wrapCompound(cx, map))
						.mapError(error -> "Failed to decode IntProvider from %s: %s".formatted(map, error));
				}
			}
			case null, default -> error(() -> "Expected a number, a numeric list, or a supported map format");
		};
	}

	private static DataResult<FloatProvider> tryWrapFloatProvider(Context cx, @Nullable Object o) {
		return switch (o) {
			case Number n -> success(ConstantFloat.of(n.floatValue()));
			case List<?> list -> switch (list.size()) {
				case 0 -> error(() -> "list cannot be empty");
				case 1 -> tryParseFloat(list.get(0)).map(ConstantFloat::of);
				case 2 -> tryParseFloat(list.get(0)).apply2(MiscWrappers::toUniform, tryParseFloat(list.get(1)));
				default -> error(() -> "list can contain at most 2 numbers");
			};
			case Map<?, ?> map -> floatProviderFromMap(cx, Cast.to(map));
			case null, default -> error(() -> "Expected a number, a numeric list, or a supported map format");
		};
	}

	private static DataResult<UniformInt> parseIntBounds(Map<String, Object> m) {
		if (m.get("bounds") instanceof List<?> bounds) {
			if (bounds.size() < 2) {
				return error(() -> "int bounds must contain at least 2 numbers, got %s".formatted(bounds));
			}

			return tryParseInt(bounds.get(0))
				.apply2(MiscWrappers::toUniform, tryParseInt(bounds.get(1)));
		} else if (m.containsKey("min") && m.containsKey("max")) {
			return tryParseInt(m.get("min")).apply2(MiscWrappers::toUniform, tryParseInt(m.get("max")));
		} else if (m.containsKey("min_inclusive") && m.containsKey("max_inclusive")) {
			return tryParseInt(m.get("min_inclusive")).apply2(MiscWrappers::toUniform, tryParseInt(m.get("max_inclusive")));
		} else if (m.containsKey("value")) {
			return tryParseInt(m.get("value")).map(f -> UniformInt.of(f, f));
		}
		return error(() -> "Failed to parse int bounds!");
	}

	private static DataResult<UniformFloat> parseFloatBounds(Map<String, Object> m) {
		if (m.get("bounds") instanceof List<?> bounds) {
			if (bounds.size() < 2) {
				return error(() -> "float bounds must contain at least 2 numbers, got %s".formatted(bounds));
			}

			return tryParseFloat(bounds.get(0)).apply2(MiscWrappers::toUniform, tryParseFloat(bounds.get(1)));
		} else if (m.containsKey("min") && m.containsKey("max")) {
			return tryParseFloat(m.get("min")).apply2(MiscWrappers::toUniform, tryParseFloat(m.get("max")));
		} else if (m.containsKey("min_inclusive") && m.containsKey("max_inclusive")) {
			return tryParseFloat(m.get("min_inclusive")).apply2(MiscWrappers::toUniform, tryParseFloat(m.get("max_inclusive")));
		} else if (m.containsKey("value")) {
			return tryParseFloat(m.get("value")).map(f -> UniformFloat.of(f, f));
		}
		return error(() -> "Failed to parse float bounds!");
	}

	private static DataResult<IntProvider> intProviderFromMap(Context cx, Map<String, Object> m) {
		if (m.containsKey("clamped")) {
			return tryWrapIntProvider(cx, m.get("clamped")).apply2(MiscWrappers::toClamped, parseIntBounds(m));
		} else if (m.containsKey("clamped_normal")) {
			return tryParseInt(m.get("mean"))
				.apply3(MiscWrappers::toClampedNormal, tryParseInt(m.get("deviation")), parseIntBounds(m));
		} else if (hasBounds(m)) {
			return parseIntBounds(m).map(v -> v);
		} else {
			return IntProviders.CODEC.parse(RegistryAccessContainer.of(cx).nbt(), NBTWrapper.wrapCompound(cx, m)).map(v -> v).mapError(error -> "Failed to decode IntProvider from %s: %s".formatted(m, error));
		}
	}

	private static DataResult<FloatProvider> floatProviderFromMap(Context cx, Map<String, Object> m) {
		if (m.containsKey("clamped_normal")) {
			return tryParseInt(m.get("mean"))
				.apply3(MiscWrappers::toClampedNormal, tryParseFloat(m.get("deviation")), parseFloatBounds(m));
		} else if (hasBounds(m)) {
			return parseFloatBounds(m).map(v -> v);
		} else {
			return FloatProviders.CODEC.parse(RegistryAccessContainer.of(cx).nbt(), NBTWrapper.wrapCompound(cx, m))
				.mapError(error -> "Failed to decode FloatProvider from %s: %s".formatted(m, error));
		}
	}

	private static boolean hasBounds(Map<String, Object> m) {
		return m.get("bounds") instanceof List
			|| (m.containsKey("min") && m.containsKey("max"))
			|| (m.containsKey("min_inclusive") && m.containsKey("max_inclusive"))
			|| m.containsKey("value");
	}

	private static UniformInt toUniform(int x, int y) {
		int min = Math.min(x, y), max = Math.max(x, y);
		return UniformInt.of(min, max);
	}

	private static UniformFloat toUniform(float x, float y) {
		float min = Math.min(x, y), max = Math.max(x, y);
		return UniformFloat.of(min, max);
	}

	private static IntProvider toClamped(IntProvider source, UniformInt clampTo) {
		return ClampedInt.of(source, clampTo.minInclusive(), clampTo.maxInclusive());
	}

	private static IntProvider toClampedNormal(int mean, int deviation, UniformInt clampTo) {
		return ClampedNormalInt.of(mean, deviation, clampTo.minInclusive(), clampTo.maxInclusive());
	}

	private static FloatProvider toClampedNormal(float mean, float deviation, UniformFloat clampTo) {
		return ClampedNormalFloat.of(mean, deviation, clampTo.min(), clampTo.max());
	}

	@Nullable
	static Path wrapPath(Context cx, @Nullable Object o) {
		try {
			if (o instanceof Path p) {
				return KubeJSPaths.verifyFilePath(p);
			} else if (o == null || o.toString().isEmpty()) {
				return null;
			}

			return KubeJSPaths.verifyFilePath(KubeJSPaths.GAMEDIR.resolve(o.toString()));
		} catch (Exception ex) {
			throw new KubeRuntimeException("Invalid path '%s'".formatted(o), ex).source(SourceLine.of(cx));
		}
	}

	@Nullable
	static File wrapFile(Context cx, @Nullable Object o) {
		try {
			if (o instanceof File f) {
				return KubeJSPaths.verifyFilePath(f.toPath()).toFile();
			} else if (o == null || o.toString().isEmpty()) {
				return null;
			}

			return KubeJSPaths.verifyFilePath(KubeJSPaths.GAMEDIR.resolve(o.toString())).toFile();
		} catch (Exception ex) {
			throw new KubeRuntimeException("Invalid file path '%s'".formatted(o), ex).source(SourceLine.of(cx));
		}
	}
}
