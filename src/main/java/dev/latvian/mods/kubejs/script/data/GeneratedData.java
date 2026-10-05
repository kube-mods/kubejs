package dev.latvian.mods.kubejs.script.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.util.JsonIO;
import dev.latvian.mods.kubejs.util.Lazy;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

public record GeneratedData(Identifier id, Supplier<byte[]> data) implements IoSupplier<InputStream> {
	public static final GeneratedData INTERNAL_RELOAD = new GeneratedData(KubeJS.id("__internal.reload"), Lazy.of(() -> new byte[0]));

	public static final GeneratedData PACK_META = new GeneratedData(KubeJS.id("pack.mcmeta"), Lazy.of(() -> {
		var json = new JsonObject();
		var pack = new JsonObject();
		pack.addProperty("description", "KubeJS Pack");
		pack.addProperty("pack_format", 8);
		var arr = new JsonArray();
		arr.add(8);
		arr.add(99);
		pack.add("supported_formats", arr);
		json.add("pack", pack);
		return json.toString().getBytes(StandardCharsets.UTF_8);
	}));

	public static final GeneratedData PACK_ICON = new GeneratedData(KubeJS.id("textures/kubejs_logo.png"), () -> {
		try {
			return Files.readAllBytes(KubeJS.thisMod.getModInfo().getOwningFile().getFile().getFilePath().resolve("assets", "kubejs", "textures", "kubejs_logo.png"));
		} catch (Exception ex) {
			ex.printStackTrace();
			return new byte[0];
		}
	});

	public static GeneratedData json(Identifier id, Supplier<JsonElement> json) {
		return new GeneratedData(id.getPath().endsWith(".json") ? id : id.withPath(id.getPath() + ".json"), Lazy.of(() -> JsonIO.toString(json.get()).getBytes(StandardCharsets.UTF_8)));
	}

	@Override
	public InputStream get() {
		return new ByteArrayInputStream(data.get());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof GeneratedData g && id.equals(g.id);
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
