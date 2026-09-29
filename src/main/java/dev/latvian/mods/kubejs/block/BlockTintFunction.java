package dev.latvian.mods.kubejs.block;

import dev.latvian.mods.kubejs.client.BlockTintFunctionWrapper;
import dev.latvian.mods.kubejs.color.KubeColor;
import dev.latvian.mods.kubejs.color.SimpleColor;
import dev.latvian.mods.kubejs.color.SimpleColorWithAlpha;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ColorWrapper;
import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.type.TypeInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

import java.util.List;

@FunctionalInterface
public interface BlockTintFunction {
	TypeInfo TYPE_INFO = TypeInfo.of(BlockTintFunction.class);

	@Nullable
	KubeColor getColor(BlockState state, @Nullable BlockAndLightGetter level, @Nullable BlockPos pos, int index);

	default int getMaxTintIndex() {
		return 0;
	}

	record Fixed(KubeColor color) implements BlockTintFunction {
		@Override
		public KubeColor getColor(BlockState state, @Nullable BlockAndLightGetter level, @Nullable BlockPos pos, int index) {
			return color;
		}
	}

	class Mapped implements BlockTintFunction {
		public final Int2ObjectMap<@Nullable BlockTintFunction> map = new Int2ObjectArrayMap<>(1);

		@Override
		public @Nullable KubeColor getColor(BlockState state, @Nullable BlockAndLightGetter level, @Nullable BlockPos pos, int index) {
			var f = map.get(index);
			return f == null ? null : f.getColor(state, level, pos, index);
		}

		@Override
		public int getMaxTintIndex() {
			int maxIndex = 0;

			for (var entry : map.int2ObjectEntrySet()) {
				maxIndex = Math.max(maxIndex, entry.getIntKey());
			}

			return maxIndex;
		}
	}

	BlockTintFunction GRASS = (s, l, p, i) -> new SimpleColor(l == null || p == null ? GrassColor.get(0.5, 1.0) : BlockTintFunctionWrapper.getAverageGrassColor(l, p));
	KubeColor DEFAULT_FOLIAGE_COLOR = new SimpleColor(FoliageColor.FOLIAGE_DEFAULT);
	BlockTintFunction FOLIAGE = (s, l, p, i) -> l == null || p == null ? DEFAULT_FOLIAGE_COLOR : new SimpleColor(BlockTintFunctionWrapper.getAverageFoliageColor(l, p));
	Fixed EVERGREEN_FOLIAGE = new Fixed(new SimpleColor(FoliageColor.FOLIAGE_EVERGREEN));
	Fixed BIRCH_FOLIAGE = new Fixed(new SimpleColor(FoliageColor.FOLIAGE_BIRCH));
	Fixed MANGROVE_FOLIAGE = new Fixed(new SimpleColor(FoliageColor.FOLIAGE_MANGROVE));
	BlockTintFunction WATER = (s, l, p, i) -> l == null || p == null ? null : new SimpleColorWithAlpha(BlockTintFunctionWrapper.getAverageWaterColor(l, p));
	@Nullable KubeColor[] REDSTONE_COLORS = new KubeColor[16];
	BlockTintFunction REDSTONE = (state, level, pos, index) -> {
		if (REDSTONE_COLORS[0] == null) {
			for (int i = 0; i < REDSTONE_COLORS.length; i++) {
				REDSTONE_COLORS[i] = new SimpleColor(RedstoneWireBlock.getColorForPower(i));
			}
		}

		return REDSTONE_COLORS[state.getValue(BlockStateProperties.POWER)];
	};

	@Nullable
	static BlockTintFunction wrap(Context cx, @Nullable Object o) {
		return switch (o) {
			case null -> null;
			case Undefined undefined -> null;
			case Scriptable s when Undefined.isUndefined(s) -> null;
			case BlockTintFunction f -> f;
			case List<?> list -> {
				var map = new Mapped();
				for (int i = 0; i < list.size(); i++) {
					var f = wrap(cx, list.get(i));
					if (f != null) {
						map.map.put(i, f);
					}
				}
				yield map;
			}
			case CharSequence cs -> switch (cs.toString()) {
				case "grass" -> GRASS;
				case "foliage" -> FOLIAGE;
				case "evergreen_foliage" -> EVERGREEN_FOLIAGE;
				case "birch_foliage" -> BIRCH_FOLIAGE;
				case "mangrove_foliage" -> MANGROVE_FOLIAGE;
				case "water" -> WATER;
				case "redstone" -> REDSTONE;
				default -> new Fixed(ColorWrapper.wrap(o));
			};
			case BaseFunction function -> (BlockTintFunction) cx.createInterfaceAdapter(TYPE_INFO, function);
			default -> new Fixed(ColorWrapper.wrap(o));
		};

	}
}
