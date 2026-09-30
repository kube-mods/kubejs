package dev.latvian.mods.kubejs.core.mixin;

import com.mojang.authlib.GameProfile;
import dev.latvian.mods.kubejs.core.ServerPlayerKJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.rhino.util.RemapForJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayer.class)
@RemapPrefixForJS("kjs$")
public abstract class ServerPlayerMixin extends Player implements ServerPlayerKJS {
	public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	@Shadow
	@RemapForJS("getStatsCounter")
	public abstract ServerStatsCounter getStats();

	@Shadow
	@Info(value = "Changes the player's gamemode.", params = {
		@Param(name = "gameMode", value = "One of: `'survival'`, `'creative'`, `'adventure'`, `'spectator'`.")
	})
	public abstract boolean setGameMode(GameType mode);
}
