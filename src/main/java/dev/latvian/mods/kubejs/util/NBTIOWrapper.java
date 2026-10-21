package dev.latvian.mods.kubejs.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface NBTIOWrapper {
	@Nullable
	static CompoundTag read(Path path) throws IOException {
		if (!Files.isRegularFile(path)) {
			return null;
		}

		return NbtIo.readCompressed(Files.newInputStream(path), NbtAccounter.unlimitedHeap());
	}

	static void write(Path path, @Nullable CompoundTag nbt) throws IOException {
		if (nbt == null || nbt.isEmpty()) {
			Files.deleteIfExists(path);
			return;
		}

		if (Files.notExists(path.getParent())) {
			Files.createDirectories(path.getParent());
		}

		NbtIo.writeCompressed(nbt, Files.newOutputStream(path));
	}
}