package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public interface GameRulesKJS {
	@HideFromJS
	default GameRules kjs$self() {
		return (GameRules) this;
	}

	@Nullable
	MinecraftServer kjs$getServer();

	@Nullable
	default GameRule<?> kjs$getRule(String rule) {
		Identifier id = Identifier.tryParse(rule);
		if (id == null) {
			id = Identifier.withDefaultNamespace(rule);
		}
		return BuiltInRegistries.GAME_RULE.getOptional(id).orElse(null);
	}

	default String kjs$getString(String rule) {
		GameRule<?> r = kjs$getRule(rule);
		if (r == null) {
			return "";
		}
		return kjs$serialize(kjs$self(), r);
	}

	default boolean kjs$getBoolean(String rule) {
		GameRule<?> r = kjs$getRule(rule);
		if (r == null) {
			return false;
		}
		Object v = kjs$self().get((GameRule<Object>) r);
		return v instanceof Boolean b && b;
	}

	default int kjs$getInt(String rule) {
		GameRule<?> r = kjs$getRule(rule);
		if (r == null) {
			return 0;
		}
		Object v = kjs$self().get((GameRule<Object>) r);
		return v instanceof Integer i ? i : 0;
	}

	default void kjs$set(String rule, String value) {
		GameRule r = kjs$getRule(rule);
		if (r == null) {
			return;
		}

		r.deserialize(value).result()
			.ifPresent((v) -> kjs$self().set(r, v, kjs$getServer()));
	}

	static <T> String kjs$serialize(GameRules rules, GameRule<T> rule) {
		return rule.serialize(rules.get(rule));
	}
}
