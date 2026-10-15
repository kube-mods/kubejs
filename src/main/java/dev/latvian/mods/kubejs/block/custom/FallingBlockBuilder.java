package dev.latvian.mods.kubejs.block.custom;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.color.KubeColor;
import dev.latvian.mods.kubejs.color.SimpleColor;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

@ReturnsSelf
public class FallingBlockBuilder extends BlockBuilder {
	private KubeColor dustColor = new SimpleColor(0x807C7B);

	public FallingBlockBuilder(Identifier i) {
		super(i);
	}

	@Override
	public Block createObject() {
		return new KubeJSFallingBlock(createProperties());
	}

	public FallingBlockBuilder dustColor(KubeColor color) {
		dustColor = color;
		return this;
	}

	static class KubeJSFallingBlock extends FallingBlock {
		public KubeJSFallingBlock(Properties p) {
			super(p);
		}

		@Override
		public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
			return ((FallingBlockBuilder) kjs$getBlockBuilder()).dustColor.kjs$getARGB();
		}
	}
}
