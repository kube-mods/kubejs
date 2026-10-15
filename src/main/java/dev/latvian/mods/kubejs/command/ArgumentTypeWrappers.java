package dev.latvian.mods.kubejs.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.util.ClassWrapper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.TeamColorArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ArgumentTypeWrappers implements ArgumentTypeWrapper {
	// builtin types, other argument types can still be accessed through byName(),
	// however those will be using class wrappers

	// numeric types
	BOOLEAN(BoolArgumentType::bool, BoolArgumentType::getBool),
	FLOAT((Supplier<ArgumentType<?>>) FloatArgumentType::floatArg, FloatArgumentType::getFloat),
	DOUBLE((Supplier<ArgumentType<?>>) DoubleArgumentType::doubleArg, DoubleArgumentType::getDouble),
	INTEGER((Supplier<ArgumentType<?>>) IntegerArgumentType::integer, IntegerArgumentType::getInteger),
	LONG((Supplier<ArgumentType<?>>) LongArgumentType::longArg, LongArgumentType::getLong),
	// string types
	STRING(StringArgumentType::string, StringArgumentType::getString),
	GREEDY_STRING(StringArgumentType::greedyString, StringArgumentType::getString),
	WORD(StringArgumentType::word, StringArgumentType::getString),
	// entity / player types
	ENTITY(EntityArgument::entity, EntityArgument::getEntity),
	ENTITIES(EntityArgument::entities, EntityArgument::getEntities),
	PLAYER(EntityArgument::player, EntityArgument::getPlayer),
	PLAYERS(EntityArgument::players, EntityArgument::getPlayers),
	GAME_PROFILE(GameProfileArgument::gameProfile, GameProfileArgument::getGameProfiles),
	// position types
	BLOCK_POS(BlockPosArgument::blockPos, BlockPosArgument::getSpawnablePos),
	BLOCK_POS_LOADED(BlockPosArgument::blockPos, BlockPosArgument::getLoadedBlockPos),
	COLUMN_POS(ColumnPosArgument::columnPos, ColumnPosArgument::getColumnPos),
	// by default, vector arguments are automatically placed at the **center** of the block
	// if no explicit offset is given, since devs may not necessarily want that, we provide both options
	VEC3(() -> Vec3Argument.vec3(false), Vec3Argument::getVec3),
	VEC2(() -> Vec2Argument.vec2(false), Vec2Argument::getVec2),
	VEC3_CENTERED((Supplier<ArgumentType<?>>) Vec3Argument::vec3, Vec3Argument::getVec3),
	VEC2_CENTERED((Supplier<ArgumentType<?>>) Vec2Argument::vec2, Vec2Argument::getVec2),
	// block-based types
	BLOCK_STATE(BlockStateArgument::block, BlockStateArgument::getBlock),
	BLOCK_PREDICATE(BlockPredicateArgument::blockPredicate, BlockPredicateArgument::getBlockPredicate),
	// item-based types
	ITEM_STACK(ItemArgument::item, ItemArgument::getItem),
	ITEM_PREDICATE(ItemPredicateArgument::itemPredicate, ItemPredicateArgument::getItemPredicate),
	// message / chat types
	COLOR(TeamColorArgument::teamColor, TeamColorArgument::getTeamColor),
	COMPONENT(ComponentArgument::textComponent, ComponentArgument::getRawComponent),
	MESSAGE(MessageArgument::message, MessageArgument::getMessage),
	// nbt
	NBT_COMPOUND(CompoundTagArgument::compoundTag, CompoundTagArgument::getCompoundTag),
	NBT_TAG(NbtTagArgument::nbtTag, NbtTagArgument::getNbtTag),
	NBT_PATH(NbtPathArgument::nbtPath, NbtPathArgument::getPath),
	// random / misc
	PARTICLE(ParticleArgument::particle, ParticleArgument::getParticle),
	ANGLE(AngleArgument::angle, AngleArgument::getAngle),
	ROTATION(RotationArgument::rotation, RotationArgument::getRotation),
	SWIZZLE(SwizzleArgument::swizzle, SwizzleArgument::getSwizzle), // i have no idea wtf this is
	ITEM_SLOT(SlotArgument::slot, SlotArgument::getSlot),
	RESOURCE_LOCATION(IdentifierArgument::id, IdentifierArgument::getId),
	ENTITY_ANCHOR(EntityAnchorArgument::anchor, EntityAnchorArgument::getAnchor),
	INT_RANGE(RangeArgument::intRange, RangeArgument.Ints::getRange),
	FLOAT_RANGE(RangeArgument::floatRange, RangeArgument.Floats::getRange),
	DIMENSION(DimensionArgument::dimension, DimensionArgument::getDimension),
	TIME(() -> TimeArgument.time(), IntegerArgumentType::getInteger),
	UUID(UuidArgument::uuid, UuidArgument::getUuid),
	OBJECTIVE(ObjectiveArgument::objective, ObjectiveArgument::getObjective),
	;

	private final Function<CommandBuildContext, ? extends ArgumentType<?>> factory;
	private final ArgumentFunction<?> getter;

	private static @Nullable Map<Identifier, ClassWrapper<?>> byNameCache;

	public static ClassWrapper<?> byName(Identifier name) {
		var wrapper = getOrCacheByName().get(name);
		if (wrapper == null) {
			throw new IllegalStateException("No argument type found for " + name);
		}
		return wrapper;
	}

	public static <T> ArgumentTypeWrapper registry(CommandRegistryKubeEvent event, Identifier reg) {
		return new ArgumentTypeWrapper() {
			final ResourceKey<Registry<T>> key = ResourceKey.createRegistryKey(reg);

			@Override
			public ArgumentType<?> create(CommandRegistryKubeEvent event) {
				return ResourceArgument.resource(event.context, key);
			}

			@Override
			public Object getResult(CommandContext<CommandSourceStack> context, String input) throws CommandSyntaxException {
				return ResourceArgument.getResource(context, input, key);
			}
		};
	}

	public ArgumentTypeWrapper time(int minRequired) {
		return new ArgumentTypeWrapper() {
			@Override
			public ArgumentType<?> create(CommandRegistryKubeEvent event) {
				return TimeArgument.time(minRequired);
			}

			@Override
			public Object getResult(CommandContext<CommandSourceStack> context, String input) {
				return IntegerArgumentType.getInteger(context, input);
			}
		};
	}

	public static void printAll() {
		for (var argType : getOrCacheByName().entrySet()) {
			ConsoleJS.SERVER.info("Argument type: " + argType.getKey() + " -> " + argType.getValue());
		}
	}

	private static Map<Identifier, ClassWrapper<?>> getOrCacheByName() {
		if (byNameCache == null) {
			return byNameCache = Util.make(new HashMap<>(), map -> {
				for (var entry : ArgumentTypeInfos.BY_CLASS.entrySet()) {
					var id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(entry.getValue());
					assert id != null;
					byNameCache.put(id, new ClassWrapper<>(entry.getKey()));
				}
			});
		}
		return byNameCache;
	}

	ArgumentTypeWrappers(Supplier<? extends ArgumentType<?>> factory, ArgumentFunction<?> getter) {
		this.factory = (ctx) -> factory.get();
		this.getter = getter;
	}

	ArgumentTypeWrappers(Function<CommandBuildContext, ? extends ArgumentType<?>> argType, ArgumentFunction<?> getter) {
		this.factory = argType;
		this.getter = getter;
	}

	@Override
	public ArgumentType<?> create(CommandRegistryKubeEvent event) {
		return factory.apply(event.context);
	}

	@Override
	public Object getResult(CommandContext<CommandSourceStack> context, String input) throws CommandSyntaxException {
		return getter.getResult(context, input);
	}
}
