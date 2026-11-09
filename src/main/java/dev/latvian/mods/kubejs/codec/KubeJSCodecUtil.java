package dev.latvian.mods.kubejs.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.StringUtilsWrapper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface KubeJSCodecUtil {
	static <E> Codec<E> stringResolverCodec(Function<E, String> toStringFunction, Function<String, E> fromStringFunction) {
		return Codec.STRING.flatXmap(str -> Optional.ofNullable(fromStringFunction.apply(str))
				.map(DataResult::success)
				.orElseGet(() -> DataResult.error(() -> "Unknown element id: " + str)),
			(object) -> DataResult.success(toStringFunction.apply(object))
		);
	}

	static <E extends Enum<E> & StringRepresentable> E byName(Codec<E> codec, String s) {
		return fromJsonOrThrow(new JsonPrimitive(s), codec);
	}

	static <E> E fromJsonOrThrow(JsonElement json, Codec<E> codec) {
		return fromJsonOrThrow(json, codec, str -> {
			throw new JsonSyntaxException("Could not decode element from JSON: " + str);
		});
	}

	static <E> JsonElement toJsonOrThrow(E value, Codec<E> codec) {
		return toJsonOrThrow(value, codec, str -> {
			throw new IllegalArgumentException("Could not encode element to JSON: " + str);
		});
	}

	static <E, X extends Throwable> E fromJsonOrThrow(JsonElement json, Codec<E> codec, Function<String, X> onError) throws X {
		return codec.parse(JsonOps.INSTANCE, json).getOrThrow(onError);
	}

	static <E, X extends Throwable> JsonElement toJsonOrThrow(E value, Codec<E> codec, Function<String, X> onError) throws X {
		return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow(onError);
	}

	static <T> String getUniqueId(T input, Codec<T> codec) {
		return StringUtilsWrapper.getUniqueId(input, o -> toJsonOrThrow(o, codec));
	}

	static JsonElement numberProviderJson(NumberProvider gen) {
		return toJsonOrThrow(gen, NumberProviders.CODEC);
	}

	static <T> Codec<List<T>> listOfOrSelf(Codec<T> codec) {
		return listOfOrSelf(codec.listOf(), codec);
	}

	// TODO: Check if this is correct
	static <T> Codec<List<T>> listOfOrSelf(Codec<List<T>> listCodec, Codec<T> codec) {
		return Codec.either(listCodec, codec).xmap(either -> either.map(Function.identity(), List::of), Either::left);
		// return Codec.withAlternative(listCodec, codec.xmap(List::of, List::getFirst));
	}

	static <V> Codec<V> or(List<Codec<? extends V>> codecs) {
		return new OrCodec<>((List) codecs);
	}

	static <V> Codec<V> or(Codec<? extends V> first, Codec<? extends V> second) {
		return new OrCodec<>((List) List.of(first, second));
	}

	static Codec<Long> longRangeWithMessage(long min, long max, Function<Long, String> errorMessage) {
		return Codec.LONG.validate(v -> v.compareTo(min) >= 0 && v.compareTo(max) <= 0 ? DataResult.success(v) : DataResult.error(() -> errorMessage.apply(v)));
	}

	static <T> boolean filter(DataResult<T> result, Predicate<T> ifSuccess) {
		return filter(result, ifSuccess, false);
	}

	static <T> boolean filter(DataResult<T> result, Predicate<T> ifSuccess, boolean orElse) {
		return switch (result) {
			case DataResult.Success<T>(var obj, var lifecycle) -> ifSuccess.test(obj);
			case DataResult.Error<T> error -> orElse;
		};
	}
}
