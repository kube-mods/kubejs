package dev.latvian.mods.kubejs.component;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.latvian.mods.kubejs.core.mixin.DataComponentPatchAccessorMixin;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.script.DataComponentTypeInfoRegistry;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.kubejs.util.Lazy;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.EvaluatorException;
import dev.latvian.mods.rhino.NativeJavaMap;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.CustomData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.mojang.serialization.DataResult.error;
import static com.mojang.serialization.DataResult.success;

public interface DataComponentWrapper {
	DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(object -> Component.translatableEscape("arguments.item.component.unknown", object));
	Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType((object, object2) -> Component.translatableEscape("arguments.item.component.malformed", object, object2));
	SimpleCommandExceptionType ERROR_EXPECTED_COMPONENT = new SimpleCommandExceptionType(Component.translatable("arguments.item.component.expected"));

	TypeInfo COMPONENT_TYPE = TypeInfo.of(DataComponentType.class);

	Lazy<Map<DataComponentType<?>, TypeInfo>> TYPE_INFOS = Lazy.identityMap(map -> {
		DataComponentTypeInfoRegistry registry = map::put;

		registry.scanClass(DataComponents.class);
		KubeJSPlugins.forEachPlugin(registry, KubeJSPlugin::registerDataComponentTypeDescriptions);
	});

	Lazy<Set<DataComponentType<?>>> VISUAL_DIFFERENCE = Lazy.of(() -> {
		var set = new HashSet<DataComponentType<?>>();

		set.add(DataComponents.DAMAGE);
		set.add(DataComponents.MAX_DAMAGE);
		set.add(DataComponents.ENCHANTMENTS);
		set.add(DataComponents.STORED_ENCHANTMENTS);
		set.add(DataComponents.CUSTOM_MODEL_DATA);
		set.add(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
		set.add(DataComponents.DYED_COLOR);
		set.add(DataComponents.POTION_CONTENTS);
		set.add(DataComponents.TRIM);
		set.add(DataComponents.ENTITY_DATA);
		set.add(DataComponents.BLOCK_ENTITY_DATA);
		set.add(DataComponents.FIREWORK_EXPLOSION);
		set.add(DataComponents.FIREWORKS);
		set.add(DataComponents.PROFILE);
		set.add(DataComponents.BANNER_PATTERNS);
		set.add(DataComponents.BASE_COLOR);
		set.add(DataComponents.POT_DECORATIONS);
		set.add(DataComponents.BLOCK_STATE);

		return set;
	});

	static TypeInfo getTypeInfo(DataComponentType<?> type) {
		return TYPE_INFOS.get().getOrDefault(type, TypeInfo.NONE);
	}

	static DataComponentType<?> wrapType(Object object) {
		if (object instanceof DataComponentType) {
			return (DataComponentType<?>) object;
		}

		var id = ID.mc(object);
		return BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).orElseThrow(() -> new KubeRuntimeException("Unknown data component type: " + object));
	}

	static DataComponentMap readMap(@Nullable DynamicOps<Tag> registryOps, StringReader reader) throws CommandSyntaxException {
		reader.skipWhitespace();

		if (!reader.canRead()) {
			return DataComponentMap.EMPTY;
		}

		DataComponentMap.Builder builder = null;

		if (reader.canRead() && reader.peek() == '[') {
			reader.skip();

			while (reader.canRead() && reader.peek() != ']') {
				reader.skipWhitespace();
				var dataComponentType = readComponentType(reader);

				reader.skipWhitespace();
				reader.expect('=');
				reader.skipWhitespace();
				int i = reader.getCursor();

				var ops = registryOps == null ? NbtOps.INSTANCE : registryOps;
				var parsed = TagParser.create(ops).parseAsArgument(reader);
				var dataResult = dataComponentType.codecOrThrow().parse(ops, parsed);

				if (builder == null) {
					builder = DataComponentMap.builder();
				}

				builder.set(dataComponentType, Cast.to(dataResult.getOrThrow(string -> {
					reader.setCursor(i);
					return ERROR_MALFORMED_COMPONENT.createWithContext(reader, dataComponentType.toString(), string);
				})));

				reader.skipWhitespace();
				if (!reader.canRead() || reader.peek() != ',') {
					break;
				}

				reader.skip();
				reader.skipWhitespace();
				if (!reader.canRead()) {
					throw ERROR_EXPECTED_COMPONENT.createWithContext(reader);
				}
			}

			reader.expect(']');
		}

		if (reader.canRead() && reader.peek() == '{') {
			var compound = TagParser.parseCompoundAsArgument(reader);

			if (builder == null) {
				builder = DataComponentMap.builder();
			}

			builder.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));
		}

		return builder == null ? DataComponentMap.EMPTY : builder.build();
	}

	static DataComponentPatch readPatch(@Nullable DynamicOps<Tag> registryOps, StringReader reader) throws CommandSyntaxException {
		reader.skipWhitespace();

		if (!reader.canRead()) {
			return DataComponentPatch.EMPTY;
		}

		var ops = registryOps == null ? NbtOps.INSTANCE : registryOps;
		DataComponentPatch.Builder builder = null;

		if (reader.peek() == '[') {
			reader.skip();

			while (reader.canRead() && reader.peek() != ']') {
				reader.skipWhitespace();

				boolean remove = reader.canRead() && reader.peek() == '!';
				if (remove) {
					reader.skip();
				}

				var dataComponentType = readComponentType(reader);

				if (remove) {
					if (builder == null) {
						builder = DataComponentPatch.builder();
					}

					builder.remove(dataComponentType);

					reader.skipWhitespace();
					if (reader.canRead() && reader.peek() == ',') {
						reader.skip();
						reader.skipWhitespace();
					}
					continue;
				}

				reader.skipWhitespace();
				reader.expect('=');
				reader.skipWhitespace();

				int i = reader.getCursor();
				var input = TagParser.create(ops).parseAsArgument(reader);
				var dataResult = dataComponentType.codecOrThrow().parse(ops, input);

				if (builder == null) {
					builder = DataComponentPatch.builder();
				}

				builder.set(dataComponentType, Cast.to(dataResult.getOrThrow(string -> {
					reader.setCursor(i);
					return ERROR_MALFORMED_COMPONENT.createWithContext(reader, dataComponentType.toString(), string);
				})));

				reader.skipWhitespace();
				if (!reader.canRead() || reader.peek() != ',') {
					break;
				}

				reader.skip();
				reader.skipWhitespace();
				if (!reader.canRead()) {
					throw ERROR_EXPECTED_COMPONENT.createWithContext(reader);
				}
			}

			reader.expect(']');
		}

		if (reader.canRead() && reader.peek() == '{') {
			var tag = TagParser.create(ops).parseAsArgument(reader);

			if (builder == null) {
				builder = DataComponentPatch.builder();
			}

			builder.set(DataComponents.CUSTOM_DATA, CustomData.of((CompoundTag) tag));
		}

		return builder == null ? DataComponentPatch.EMPTY : builder.build();
	}


	static DataComponentType<?> readComponentType(StringReader stringReader) throws CommandSyntaxException {
		if (!stringReader.canRead()) {
			throw ERROR_EXPECTED_COMPONENT.createWithContext(stringReader);
		}

		int i = stringReader.getCursor();
		Identifier identifier = Identifier.read(stringReader);
		DataComponentType<?> dataComponentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
		if (dataComponentType != null && !dataComponentType.isTransient()) {
			return dataComponentType;
		} else {
			stringReader.setCursor(i);
			throw ERROR_UNKNOWN_COMPONENT.createWithContext(stringReader, identifier);
		}
	}

	static DataComponentMap readPredicate(@Nullable DynamicOps<Tag> registryOps, StringReader reader) throws CommandSyntaxException {
		return reader.canRead() ? readMap(registryOps, reader) : DataComponentMap.EMPTY;
	}


	static boolean filter(Object from, TypeInfo target) {
		return from == null || from instanceof DataComponentMap || from instanceof DataComponentPatch || from instanceof Map || from instanceof NativeJavaMap || from instanceof String s && (s.isEmpty() || s.charAt(0) == '[');
	}

	static DataComponentMap mapOf(Context cx, Object from) {
		return tryMapOf(cx, from)
			.getOrThrow(error -> new KubeRuntimeException("Failed to wrap DataComponentMap: %s".formatted(error))
				.source(SourceLine.of(cx)));
	}

	static DataComponentPatch patchOf(Context cx, Object from) {
		return tryPatchOf(cx, from)
			.getOrThrow(error -> new KubeRuntimeException("Failed to wrap DataComponentPatch: %s".formatted(error))
				.source(SourceLine.of(cx)));
	}

	static DataComponentMap mapOrEmptyOf(Context cx, Object from) {
		return tryMapOf(cx, from)
			.resultOrPartial()
			.orElse(DataComponentMap.EMPTY);
	}

	static DataComponentPatch patchOrEmptyOf(Context cx, Object from) {
		return tryPatchOf(cx, from)
			.resultOrPartial()
			.orElse(DataComponentPatch.EMPTY);
	}

	static DataResult<DataComponentMap> tryMapOf(Context cx, @Nullable Object o) {
		var reg = RegistryAccessContainer.of(cx);
		return switch (o) {
			case DataComponentMap map -> success(map);
			case DataComponentPatch patch -> success(PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
			case BaseFunction fn -> fnToBuilder(cx, MapBuilder.class, fn,
				builder -> Util.make(DataComponentMap.builder(), builder).build());
			case JsonObject json -> DataComponentMap.CODEC.parse(reg.json(), json);
			case CompoundTag tag -> DataComponentMap.CODEC.parse(reg.nbt(), tag);
			case Map<?, ?> map -> {
				var builder = DataComponentMap.builder();

				Map<DataComponentType<?>, ?> wrapped = Objects.requireNonNull(cx.optionalMapOf(map, COMPONENT_TYPE, TypeInfo.NONE));
				Map<DataComponentType<?>, String> errors = new HashMap<>();

				for (var entry : wrapped.entrySet()) {
					wrapEntry(cx, entry, null, builder::set, errors::put);
				}

				if (!errors.isEmpty()) {
					var joiner = new StringJoiner("; ");
					errors.forEach((type, error) -> {
						var id = reg.registry(Registries.DATA_COMPONENT_TYPE).getKeyOrNull(type);
						joiner.add("'%s' -> %s".formatted(id, error));
					});
					yield error(() -> "Invalid component map format, errored input: [%s]".formatted(joiner.toString()), builder.build());
				} else {
					yield success(builder.build());
				}
			}
			case null -> success(DataComponentMap.EMPTY);
			case String s -> {
				try {
					yield success(readMap(reg.nbt(), new StringReader(s)));
				} catch (CommandSyntaxException ex) {
					yield error(() -> "Invalid string format '%s' for DataComponentMap: %s".formatted(s, ex.getMessage()));
				}
			}
			default -> error(() -> "Don't know how to convert %s to DataComponentMap!".formatted(o));
		};
	}

	static DataResult<DataComponentPatch> tryPatchOf(Context cx, @Nullable Object o) {
		var reg = RegistryAccessContainer.of(cx);
		return switch (o) {
			case DataComponentPatch patch -> success(patch);
			case BaseFunction fn -> fnToBuilder(cx, PatchBuilder.class, fn,
				builder -> Util.make(DataComponentPatch.builder(), builder).build());
			case JsonObject json -> DataComponentPatch.CODEC.parse(reg.json(), json);
			case CompoundTag tag -> DataComponentPatch.CODEC.parse(reg.nbt(), tag);
			case Map<?, ?> map -> {
				var builder = DataComponentPatch.builder();

				Map<DataComponentType<?>, ?> wrapped = Objects.requireNonNull(cx.optionalMapOf(map, COMPONENT_TYPE, TypeInfo.NONE));
				Map<DataComponentType<?>, String> errors = new HashMap<>();

				for (var entry : wrapped.entrySet()) {
					wrapEntry(cx, entry, builder::remove, builder::set, errors::put);
				}

				if (!errors.isEmpty()) {
					var joiner = new StringJoiner("; ");
					errors.forEach((type, error) -> {
						var id = reg.registry(Registries.DATA_COMPONENT_TYPE).getKey(type);
						joiner.add("'%s' -> %s".formatted(id, error));
					});
					yield error(() -> "Invalid component map format, errored input: [%s]".formatted(joiner.toString()), builder.build());
				} else {
					yield success(builder.build());
				}
			}
			case null -> success(DataComponentPatch.EMPTY);
			case String s -> {
				try {
					yield success(readPatch(reg.nbt(), new StringReader(s)));
				} catch (CommandSyntaxException ex) {
					yield error(() -> "Invalid string format '%s' for DataComponentPatch: %s".formatted(s, ex.getMessage()));
				}
			}
			default -> error(() -> "Don't know how to convert %s to DataComponentPatch!".formatted(o));
		};
	}

	static <T> DataResult<Optional<T>> tryWrapComponent(Context cx, DataComponentType<T> type, @Nullable Object value) {
		var reg = RegistryAccessContainer.of(cx);

		var valueType = getTypeInfo(type);

		if (value == null || value instanceof Undefined) {
			return success(Optional.empty());
		}

		var evalError = new MutableObject<@Nullable EvaluatorException>();

		if (valueType.shouldConvert() && cx.canConvert(value, valueType)) {
			try {
				Object converted = cx.jsToJava(value, valueType);
				if (converted != null) {
					return success(Optional.of(Cast.to(converted)));
				}
			} catch (EvaluatorException e) {
				evalError.setValue(e);
			}
		}


		var codec = type.codec();

		if (codec != null) {
			//noinspection unchecked
			return (DataResult<Optional<T>>) switch (codec.parse(reg.json(), JsonUtils.of(cx, value))) {
				case DataResult.Success<?> success -> success.map(Optional::of);
				case DataResult.Error<?> error -> error.mapError(err -> evalError.get() != null
					? "Failed to parse component from type wrappers and codec! Native: %s, Codec: %s".formatted(evalError.get().details(), err)
					: "Failed to parse component from codec: %s!".formatted(err));
			};
		} else {
			return error(() -> "Component has non-serializable type");
		}
	}

	private static void wrapEntry(
		Context cx,
		Map.Entry<DataComponentType<?>, ?> entry,
		@Nullable Consumer<DataComponentType<?>> ifNull,
		@SuppressWarnings("rawtypes") BiConsumer<DataComponentType, Object> builder,
		BiConsumer<DataComponentType<?>, String> errorBuilder
	) {
		var type = entry.getKey();
		var value = entry.getValue();

		tryWrapComponent(cx, type, value)
			.ifSuccess(success -> {
				if (success.isPresent()) {
					builder.accept(type, success.get());
				} else if (ifNull != null) {
					ifNull.accept(type);
				}
			})
			.ifError(error -> errorBuilder.accept(type, error.message()));
	}

	private static <B, T> DataResult<T> fnToBuilder(Context cx, Class<B> builderType, BaseFunction fn, Function<B, T> build) {
		try {
			B builder = Cast.to(cx.createInterfaceAdapter(TypeInfo.of(builderType), fn));
			return success(build.apply(builder));
		} catch (Exception e) {
			return error(() -> "Failed to create %s from builder: %s".formatted(builderType.toString(), e));
		}
	}

	static StringBuilder mapToString(StringBuilder builder, @Nullable DynamicOps<Tag> ops, DataComponentMap map) {
		builder.append('[');

		boolean first = true;

		for (var comp : map) {
			var id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(comp.type());
			var codec = comp.type().codec();

			if (id == null || codec == null) {
				continue;
			}

			if (first) {
				first = false;
			} else {
				builder.append(',');
			}

			builder.append(ID.reduce(id)).append('=');
			try {
				var value = codec == Codec.BOOL ? comp.value() : codec.encodeStart(ops == null ? NbtOps.INSTANCE : ops, Cast.to(comp.value())).getOrThrow();
				builder.append(value);
			} catch (Throwable ex) {
				builder.append("ERROR[").append(ex.getMessage()).append("]");
			}
		}

		builder.append(']');
		return builder;
	}

	static StringBuilder patchToString(StringBuilder builder, @Nullable DynamicOps<Tag> ops, DataComponentPatch patch) {
		builder.append('[');

		boolean first = true;

		var map = ((DataComponentPatchAccessorMixin) (Object) patch).getMap();

		for (var comp : map.entrySet()) {
			var id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(comp.getKey());
			var codec = comp.getKey().codec();

			if (id == null || codec == null) {
				continue;
			}

			if (first) {
				first = false;
			} else {
				builder.append(',');
			}

			if (comp.getValue() != null) {
				builder.append(ID.reduce(id)).append('=');

				try {
					var value = codec == Codec.BOOL ? comp.getValue() : codec.encodeStart(ops == null ? NbtOps.INSTANCE : ops, Cast.to(comp.getValue())).getOrThrow();
					builder.append(value);
				} catch (Throwable ex) {
					builder.append("ERROR[").append(ex.getMessage()).append("]");
				}
			} else {
				builder.append('!').append(ID.reduce(id));
			}
		}

		builder.append(']');
		return builder;
	}

	static DataComponentPatch visualPatch(DataComponentPatch patch) {
		if (patch.isEmpty()) {
			return DataComponentPatch.EMPTY;
		}

		var builder = DataComponentPatch.builder();
		var map = ((DataComponentPatchAccessorMixin) (Object) patch).getMap();
		for (var entry : map.entrySet()) {
			if (VISUAL_DIFFERENCE.get().contains(entry.getKey())) {
				if (entry.getValue() != null) {
					builder.set(entry.getKey(), Cast.to(entry.getValue()));
				} else {
					builder.remove(entry.getKey());
				}
			}
		}

		return builder.build();
	}

	interface MapBuilder extends Consumer<DataComponentMap.Builder> {
		@Override
		void accept(DataComponentMap.Builder builder);
	}

	interface PatchBuilder extends Consumer<DataComponentPatch.Builder> {
		@Override
		void accept(DataComponentPatch.Builder builder);
	}
}
