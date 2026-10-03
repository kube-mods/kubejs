package dev.latvian.mods.kubejs.util;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.core.RegistryObjectKJS;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import net.minecraft.IdentifierException;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

public interface ID {
	Identifier UNKNOWN = Identifier.fromNamespaceAndPath("unknown", "unknown");
	Identifier AIR = Identifier.withDefaultNamespace("air");
	UnaryOperator<String> BLOCKSTATE = s -> "blockstates/" + s;
	UnaryOperator<String> BLOCK = s -> "block/" + s;
	UnaryOperator<String> ITEM = s -> "item/" + s;
	UnaryOperator<String> MODEL = s -> "models/" + s;
	UnaryOperator<String> BLOCK_MODEL = s -> "models/block/" + s;
	UnaryOperator<String> ITEM_MODEL = s -> "models/item/" + s;
	UnaryOperator<String> ITEM_DEFINITION = s -> "items/" + s;
	UnaryOperator<String> BLOCK_LOOT_TABLE = s -> "loot_table/blocks/" + s;
	UnaryOperator<String> PNG_TEXTURE = s -> "textures/" + s + ".png";
	UnaryOperator<String> PNG_TEXTURE_MCMETA = s -> "textures/" + s + ".png.mcmeta";
	UnaryOperator<String> PARTICLE = s -> "particles/" + s;

	static String string(@Nullable String id) {
		if (id == null || id.isEmpty()) {
			return "";
		}

		if (id.indexOf(':') == -1) {
			return "minecraft:" + id;
		}

		return id;
	}

	static String kjsString(String id) {
		if (id == null || id.isEmpty()) {
			return "";
		}

		if (id.indexOf(':') == -1) {
			return KubeJS.MOD_ID + ":" + id;
		}

		return id;
	}

	static String namespace(@Nullable String s) {
		if (s == null || s.isEmpty()) {
			return "minecraft";
		}

		var i = s.indexOf(':');
		return i == -1 ? "minecraft" : s.substring(0, i);
	}

	static String path(@Nullable String s) {
		if (s == null || s.isEmpty()) {
			return "air";
		}

		var i = s.indexOf(':');
		return i == -1 ? s : s.substring(i + 1);
	}

	@Nullable
	@Contract("null, _ -> null; !null, _ -> !null")
	static Identifier of(@Nullable Object o, boolean preferKJS) {
		return switch (o) {
			case null -> null;
			case Identifier id -> id;
			case ResourceKey<?> key -> key.identifier();
			case Holder<?> holder -> holder.key().identifier();
			case RegistryObjectKJS<?> key -> key.kjs$getIdLocation();
			default -> {
				var s = o instanceof JsonPrimitive p ? p.getAsString() : o.toString();

				if (s.indexOf(':') == -1 && preferKJS) {
					s = "kubejs:" + s;
				}

				try {
					yield Identifier.parse(s);
				} catch (IdentifierException ex) {
					throw new KubeRuntimeException("Could not create ID from '%s'!".formatted(s));
				}
			}
		};
	}

	@Nullable
	@Contract("null -> null; !null -> !null")
	static Identifier mc(@Nullable Object o) {
		return of(o, false);
	}

	@Nullable
	@Contract("null -> null; !null -> !null")
	static Identifier kjs(@Nullable Object o) {
		return of(o, true);
	}

	static boolean isKey(@Nullable Object from) {
		return from instanceof CharSequence || from instanceof Identifier || from instanceof ResourceKey<?>;
	}

	static boolean isValidKey(@Nullable Object from) {
		return from instanceof Identifier || from instanceof ResourceKey<?> || (from instanceof CharSequence && Identifier.tryParse(from.toString()) != null);
	}

	static String url(Identifier id) {
		return URLEncoder.encode(id.getNamespace(), StandardCharsets.UTF_8) + "/" + URLEncoder.encode(id.getPath(), StandardCharsets.UTF_8);
	}

	static String reduce(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
	}

	static String reduceKjs(Identifier id) {
		return id.getNamespace().equals(KubeJS.MOD_ID) ? id.getPath() : id.toString();
	}

	static String resourcePath(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : (id.getNamespace() + "/" + id.getPath());
	}

	static DataResult<Identifier> read(StringReader reader) {
		return Identifier.read(readGreedy(reader));
	}

	private static String readGreedy(StringReader reader) {
		int i = reader.getCursor();

		while (reader.canRead() && Identifier.isAllowedInIdentifier(reader.peek())) {
			reader.skip();
		}

		return reader.getString().substring(i, reader.getCursor());
	}
}
