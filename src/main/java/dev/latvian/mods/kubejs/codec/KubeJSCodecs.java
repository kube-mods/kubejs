package dev.latvian.mods.kubejs.codec;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.TimeJS;
import dev.latvian.mods.rhino.type.ClassTypeInfo;
import dev.latvian.mods.rhino.type.EnumTypeInfo;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;

import java.time.Duration;
import java.util.Map;

public interface KubeJSCodecs {
	Codec<Character> CHARACTER = Codec.STRING.comapFlatMap(str -> {
		if (str.length() != 1) {
			return DataResult.error(() -> "Expected a single character, got empty string");
		} else {
			return DataResult.success(str.charAt(0));
		}
	}, Object::toString);

	Codec<ResourceLocation> KUBEJS_ID = Codec.STRING.comapFlatMap(s -> {
		try {
			if (s.indexOf(':') == -1) {
				return DataResult.success(KubeJS.id(s));
			} else {
				return DataResult.success(ResourceLocation.parse(s));
			}
		} catch (ResourceLocationException ex) {
			return DataResult.error(() -> "Not a valid resource location: " + s + " " + ex.getMessage());
		}
	}, ID::reduceKjs).stable();

	Codec<Class<?>> ENUM_CLASS = Codec.STRING.comapFlatMap(str -> {
		try {
			var c = Class.forName(str);

			if (!c.isEnum()) {
				return DataResult.error(() -> "Class '" + str + "' is not an enum");
			}

			return DataResult.success(c);
		} catch (ClassNotFoundException e) {
			return DataResult.error(() -> "Could not find enum class: " + str);
		}
	}, Class::getName);

	Codec<EnumTypeInfo> ENUM_TYPE_INFO = ENUM_CLASS.comapFlatMap(c -> {
		if (TypeInfo.of(c) instanceof EnumTypeInfo info) {
			return DataResult.success(info);
		} else {
			return DataResult.error(() -> "Class " + c.getTypeName() + " is not an enum!");
		}
	}, ClassTypeInfo::asClass);

	Codec<ResourceKey<? extends Registry<?>>> REGISTRY_KEY = ResourceLocation.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);

	MapCodec<EntityType<?>> ENTITY_TYPE_FIELD = BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id");

	Codec<Duration> DURATION = Codec.STRING.comapFlatMap(TimeJS::readDuration, Duration::toString);

	Codec<ResourceKey<? extends Registry<?>>> REGISTRY_KEY_CODEC = ResourceLocation.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);

	Codec<Map<String, JsonElement>> JSON_MAP = Codec.unboundedMap(Codec.STRING, ExtraCodecs.JSON);

	Codec<Integer> NON_NEGATIVE_INT = Codec.INT.validate(v -> v >= 0 ? DataResult.success(v) : DataResult.error(() -> "Value must be non-negative: " + v));
	Codec<Integer> POSITIVE_INT = Codec.INT.validate(v -> v > 0 ? DataResult.success(v) : DataResult.error(() -> "Value must be positive: " + v));
	Codec<Long> NON_NEGATIVE_LONG = Codec.LONG.validate(v -> v >= 0L ? DataResult.success(v) : DataResult.error(() -> "Value must be non-negative: " + v));
	Codec<Long> POSITIVE_LONG = Codec.LONG.validate(v -> v > 0L ? DataResult.success(v) : DataResult.error(() -> "Value must be positive: " + v));
	Codec<Float> NON_NEGATIVE_FLOAT = Codec.FLOAT.validate(v -> v >= 0F ? DataResult.success(v) : DataResult.error(() -> "Value must be non-negative: " + v));
	Codec<Float> POSITIVE_FLOAT = Codec.FLOAT.validate(v -> v > 0F ? DataResult.success(v) : DataResult.error(() -> "Value must be positive: " + v));
	Codec<Double> NON_NEGATIVE_DOUBLE = Codec.DOUBLE.validate(v -> v >= 0D ? DataResult.success(v) : DataResult.error(() -> "Value must be non-negative: " + v));
	Codec<Double> POSITIVE_DOUBLE = Codec.DOUBLE.validate(v -> v > 0D ? DataResult.success(v) : DataResult.error(() -> "Value must be positive: " + v));
}
