package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.Context;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ARGB;

public interface ParticleOptionsWrapper {
	DustParticleOptions ERROR = new DustParticleOptions(ARGB.colorFromFloat(1F, 0F, 0F, 0F), 1F);

	// TODO (26.1?): improve
	static ParticleOptions wrap(Context cx, Object o) {
		if (o instanceof ParticleOptions po) {
			return po;
		} else if (o != null) {
			try {
				var reader = new StringReader(o instanceof JsonElement j ? j.getAsString() : o.toString());
				return ParticleArgument.readParticle(reader, RegistryAccessContainer.of(cx).getRegistryAccess());
			} catch (Exception ex) {
				throw new KubeRuntimeException("Failed to parse ParticleOptions from %s".formatted(o), ex).source(SourceLine.of(cx));
			}
		}

		return ERROR;
	}

	static ParticleOptions create(ParticleOptions options) {
		return options;
	}
}
