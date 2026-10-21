package dev.latvian.mods.kubejs.level.ruletest;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;

import java.util.ArrayList;
import java.util.List;

public class AllMatchRuleTest extends RuleTest {

	public static final MapCodec<AllMatchRuleTest> CODEC = RuleTest.CODEC
		.listOf()
		.fieldOf("rules")
		.xmap(AllMatchRuleTest::new, (t) -> t.rules);

	public final List<RuleTest> rules;

	public AllMatchRuleTest() {
		this(new ArrayList<>());
	}

	@Override
	public boolean test(BlockState blockState, BlockPos blockPos, RandomSource randomSource) {
		for (var test : rules) {
			if (!test.test(blockState, blockPos, randomSource)) {
				return false;
			}
		}
		return true;
	}

	public AllMatchRuleTest(List<RuleTest> rules) {
		this.rules = rules;
	}

	@Override
	protected RuleTestType<?> getType() {
		return KubeJSRuleTests.ALL_MATCH.get();
	}
}
