package dev.latvian.mods.kubejs.item;

import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.item.JukeboxSong;

@ReturnsSelf
public class JukeboxSongBuilder extends BuilderBase<JukeboxSong> {
	public transient Holder<SoundEvent> sound;
	public transient float lengthInSeconds;
	public transient Component description;
	public transient int comparatorOutput;

	public JukeboxSongBuilder(Identifier id) {
		super(id);
		this.sound = SoundEvents.MUSIC_DISC_11;
		this.lengthInSeconds = 71F;
		this.description = Component.translatable(Util.makeDescriptionId("jukebox_song", id));
		this.comparatorOutput = 0;
	}

	@Override
	public JukeboxSong createObject() {
		return new JukeboxSong(sound, description, lengthInSeconds, comparatorOutput);
	}

	public JukeboxSongBuilder song(Holder<SoundEvent> sound, float length) {
		this.sound = sound;
		this.lengthInSeconds = length;
		return this;
	}

	public JukeboxSongBuilder description(Component description) {
		this.description = description;
		return this;
	}

	public JukeboxSongBuilder comparatorOutput(int comparatorOutput) {
		this.comparatorOutput = comparatorOutput;
		return this;
	}
}
