package dev.latvian.mods.kubejs.block.state;

import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.level.ruletest.AllMatchRuleTest;
import dev.latvian.mods.kubejs.level.ruletest.AlwaysFalseRuleTest;
import dev.latvian.mods.kubejs.level.ruletest.AnyMatchRuleTest;
import dev.latvian.mods.kubejs.level.ruletest.InvertRuleTest;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.BlockWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.NBTWrapper;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatch;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.RegExpKJS;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.kubejs.util.Tags;
import dev.latvian.mods.rhino.Context;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public sealed interface BlockStatePredicate extends Predicate<BlockState>, ReplacementMatch {
	@Override
	boolean test(BlockState state);

	default boolean testBlock(Block block) {
		return test(block.defaultBlockState());
	}

	@Nullable
	default RuleTest asRuleTest() {
		return null;
	}

	static BlockStatePredicate fromString(Context cx, String s) {
		return switch (s) {
			case "*" -> Simple.ALL;
			case "-" -> Simple.NONE;
			case String str when str.startsWith("#") -> new TagMatch(Tags.block(Identifier.parse(str.substring(1))));
			case String str when str.indexOf('[') != -1 -> {
				var state = BlockWrapper.parseBlockState(cx, str);
				yield state != Blocks.AIR.defaultBlockState() ? new StateMatch(state) : Simple.NONE;
			}
			default -> {
				var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(s));
				yield block != Blocks.AIR ? new BlockMatch(block) : Simple.NONE;
			}
		};
	}

	static BlockStatePredicate wrap(Context cx, @Nullable Object o) {
		if (o == null || o == Simple.ALL) {
			return Simple.ALL;
		} else if (o == Simple.NONE) {
			return Simple.NONE;
		}

		var list = ListJS.orSelf(o);

		if (list.isEmpty()) {
			return Simple.NONE;
		} else if (list.size() > 1) {
			var predicates = new ArrayList<BlockStatePredicate>();

			for (var o1 : list) {
				var p = wrap(cx, o1);
				if (p == Simple.ALL) {
					return Simple.ALL;
				} else if (p != Simple.NONE) {
					predicates.add(p);
				}
			}

			return predicates.isEmpty() ? Simple.NONE : predicates.size() == 1 ? predicates.getFirst() : new OrMatch(predicates);
		}

		var first = list.getFirst();
		var map = cx.optionalMapOf(first);

		if (map != null) {
			if (map.isEmpty()) {
				return Simple.ALL;
			}

			var predicates = new ArrayList<BlockStatePredicate>();

			if (map.get("or") != null) {
				predicates.add(wrap(cx, map.get("or")));
			}

			if (map.get("not") != null) {
				predicates.add(new NotMatch(wrap(cx, map.get("not"))));
			}

			return new AndMatch(predicates);
		}

		return ofSingle(cx, first);
	}

	static RuleTest wrapRuleTest(Context cx, @Nullable Object o) {
		var nbt = RegistryAccessContainer.of(cx).nbt();
		return switch (o) {
			case RuleTest rule -> rule;
			case BlockStatePredicate bsp when bsp.asRuleTest() != null ->
				//noinspection DataFlowIssue
				bsp.asRuleTest(); // safe
			case null, default -> Optional.ofNullable(NBTWrapper.wrapCompound(cx, o))
				.map(tag -> RuleTest.CODEC.parse(nbt, tag))
				.flatMap(DataResult::result)
				.or(() -> Optional.ofNullable(wrap(cx, o).asRuleTest()))
				.orElseThrow(() -> new KubeRuntimeException("Could not parse valid rule test from %s!".formatted(o)).source(SourceLine.of(cx)));
		};
	}

	@SuppressWarnings("unchecked")
	private static BlockStatePredicate ofSingle(Context cx, Object o) {
		return switch (o) {
			case BlockStatePredicate bsp -> bsp;
			case Block block -> new BlockMatch(block);
			case BlockState state -> new StateMatch(state);
			case TagKey tag -> new TagMatch((TagKey<Block>) tag);
			default -> {
				var pattern = RegExpKJS.wrap(o);
				yield pattern == null ? BlockStatePredicate.fromString(cx, o.toString()) : new RegexMatch(pattern);
			}
		};
	}

	default Collection<BlockState> getBlockStates() {
		var states = new HashSet<BlockState>();
		for (var state : BlockWrapper.getAllBlockStates()) {
			if (test(state)) {
				states.add(state);
			}
		}
		return states;
	}

	default Collection<Block> getBlocks() {
		var blocks = new HashSet<Block>();
		for (var state : getBlockStates()) {
			blocks.add(state.getBlock());
		}
		return blocks;
	}

	default Set<Identifier> getBlockIds() {
		Set<Identifier> set = new LinkedHashSet<>();

		for (var block : getBlocks()) {
			set.add(block.kjs$getIdLocation());
		}

		return set;
	}

	default boolean check(List<BlockReplacement> targetStates) {
		for (var state : targetStates) {
			if (test(state.state())) {
				return true;
			}
		}

		return false;
	}

	enum Simple implements BlockStatePredicate {
		ALL(true),
		NONE(false);

		private final boolean match;

		Simple(boolean match) {
			this.match = match;
		}

		@Override
		public boolean test(BlockState state) {
			return match;
		}

		@Override
		public boolean testBlock(Block block) {
			return match;
		}

		@Override
		public RuleTest asRuleTest() {
			return match ? AlwaysTrueTest.INSTANCE : AlwaysFalseRuleTest.INSTANCE;
		}

		@Override
		public Collection<BlockState> getBlockStates() {
			return match ? BlockWrapper.getAllBlockStates() : List.of();
		}
	}

	record BlockMatch(Block block) implements BlockStatePredicate {
		@Override
		public boolean test(BlockState state) {
			return state.is(block);
		}

		@Override
		public boolean testBlock(Block block) {
			return this.block == block;
		}

		@Override
		public Collection<Block> getBlocks() {
			return Collections.singleton(block);
		}

		@Override
		public Collection<BlockState> getBlockStates() {
			return block.getStateDefinition().getPossibleStates();
		}

		@Override
		public Set<Identifier> getBlockIds() {
			return Set.of(block.kjs$getIdLocation());
		}

		@Override
		public RuleTest asRuleTest() {
			return new BlockMatchTest(block);
		}
	}

	record StateMatch(BlockState state) implements BlockStatePredicate {
		@Override
		public boolean test(BlockState s) {
			return state == s;
		}

		@Override
		public boolean testBlock(Block block) {
			return state.getBlock() == block;
		}

		@Override
		public Collection<Block> getBlocks() {
			return Collections.singleton(state.getBlock());
		}

		@Override
		public Collection<BlockState> getBlockStates() {
			return Collections.singleton(state);
		}

		@Override
		public Set<Identifier> getBlockIds() {
			return Set.of(state.getBlock().kjs$getIdLocation());
		}

		@Override
		public RuleTest asRuleTest() {
			return new BlockStateMatchTest(state);
		}
	}

	record TagMatch(TagKey<Block> tag) implements BlockStatePredicate {
		@Override
		public boolean test(BlockState state) {
			return state.is(tag);
		}

		@Override
		public boolean testBlock(Block block) {
			return block.builtInRegistryHolder().is(tag);
		}

		@Override
		public Collection<Block> getBlocks() {
			return Util.make(new LinkedHashSet<>(), set -> {
				for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
					set.add(holder.value());
				}
			});
		}

		@Override
		public RuleTest asRuleTest() {
			return new TagMatchTest(tag);
		}
	}

	final class RegexMatch implements BlockStatePredicate {
		public final Pattern pattern;
		private final LinkedHashSet<Block> matchedBlocks;

		public RegexMatch(Pattern p) {
			pattern = p;
			matchedBlocks = new LinkedHashSet<>();
			for (var block : BuiltInRegistries.BLOCK) {
				if (!matchedBlocks.contains(block) && pattern.matcher(block.kjs$getId()).find()) {
					matchedBlocks.add(block);
				}
			}
		}

		@Override
		public boolean test(BlockState state) {
			return matchedBlocks.contains(state.getBlock());
		}

		@Override
		public boolean testBlock(Block block) {
			return matchedBlocks.contains(block);
		}

		@Override
		public Collection<Block> getBlocks() {
			return matchedBlocks;
		}

		@Override
		public RuleTest asRuleTest() {
			var test = new AnyMatchRuleTest();
			for (var block : matchedBlocks) {
				test.rules.add(new BlockMatchTest(block));
			}
			return test;
		}
	}

	record OrMatch(List<BlockStatePredicate> list) implements BlockStatePredicate {
		@Override
		public boolean test(BlockState state) {
			for (var predicate : list) {
				if (predicate.test(state)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean testBlock(Block block) {
			for (var predicate : list) {
				if (predicate.testBlock(block)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public Collection<Block> getBlocks() {
			var set = new HashSet<Block>();

			for (var predicate : list) {
				set.addAll(predicate.getBlocks());
			}

			return set;
		}

		@Override
		public Collection<BlockState> getBlockStates() {
			var set = new HashSet<BlockState>();

			for (var predicate : list) {
				set.addAll(predicate.getBlockStates());
			}

			return set;
		}

		@Override
		public Set<Identifier> getBlockIds() {
			Set<Identifier> set = new LinkedHashSet<>();

			for (var predicate : list) {
				set.addAll(predicate.getBlockIds());
			}

			return set;
		}

		@Override
		public RuleTest asRuleTest() {
			var test = new AnyMatchRuleTest();
			for (var predicate : list) {
				var t = predicate.asRuleTest();
				if (t != null) {
					test.rules.add(t);
				}
			}
			return test;
		}
	}

	final class NotMatch implements BlockStatePredicate {
		private final BlockStatePredicate predicate;
		private final Collection<BlockState> cachedStates;

		public NotMatch(BlockStatePredicate predicate) {
			this.predicate = predicate;

			cachedStates = new LinkedHashSet<>();

			for (var block : BuiltInRegistries.BLOCK) {
				for (var state : block.getStateDefinition().getPossibleStates()) {
					if (!predicate.test(state)) {
						cachedStates.add(state);
					}
				}
			}
		}

		@Override
		public boolean test(BlockState state) {
			return !predicate.test(state);
		}

		@Override
		public boolean testBlock(Block block) {
			return !predicate.testBlock(block);
		}

		@Override
		public Collection<Block> getBlocks() {
			Set<Block> set = new HashSet<>();
			for (var blockState : getBlockStates()) {
				set.add(blockState.getBlock());
			}
			return set;
		}

		@Override
		public Collection<BlockState> getBlockStates() {
			return cachedStates;
		}

		@Override
		public Set<Identifier> getBlockIds() {
			var set = new HashSet<Identifier>();

			for (var block : getBlocks()) {
				set.add(block.kjs$getIdLocation());
			}

			return set;
		}

		@Override
		@Nullable
		public RuleTest asRuleTest() {
			var t = predicate.asRuleTest();
			if (t != null) {
				return new InvertRuleTest(t);
			} else {
				return null;
			}
		}
	}

	final class AndMatch implements BlockStatePredicate {
		private final List<BlockStatePredicate> list;
		private final Collection<BlockState> cachedStates;

		public AndMatch(List<BlockStatePredicate> list) {
			this.list = list;
			cachedStates = new LinkedHashSet<>();

			for (var block : BuiltInRegistries.BLOCK) {
				for (var state : block.getStateDefinition().getPossibleStates()) {
					var match = true;
					for (var predicate : list) {
						if (!predicate.test(state)) {
							match = false;
							break;
						}
					}
					if (match) {
						cachedStates.add(state);
					}
				}
			}
		}

		@Override
		public boolean test(BlockState state) {
			for (var predicate : list) {
				if (!predicate.test(state)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean testBlock(Block block) {
			for (var predicate : list) {
				if (!predicate.testBlock(block)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Collection<Block> getBlocks() {
			Set<Block> set = new HashSet<>();
			for (var blockState : getBlockStates()) {
				set.add(blockState.getBlock());
			}
			return set;
		}

		@Override
		public Collection<BlockState> getBlockStates() {
			return cachedStates;
		}

		@Override
		public RuleTest asRuleTest() {
			var test = new AllMatchRuleTest();
			for (var predicate : list) {
				var t = predicate.asRuleTest();
				if (t != null) {
					test.rules.add(t);
				}
			}
			return test;
		}
	}
}
