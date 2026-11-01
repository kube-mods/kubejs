package dev.latvian.mods.kubejs.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.client.EditorExt;
import dev.latvian.mods.kubejs.client.KubeJSClient;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Util;

import java.util.concurrent.CompletableFuture;

public class KubeJSClientCommands {
	private static LiteralArgumentBuilder<CommandSourceStack> clientCommands(String name) {
		return Commands.literal(name)
			.then(Commands.literal("reload")
				.then(Commands.literal("client-scripts")
					.requires(source -> true)
					.executes(context -> reloadClient(context.getSource()))
				)
				.then(Commands.literal("textures")
					.requires(source -> true)
					.executes(context -> reloadTextures(context.getSource()))
				)
				.then(Commands.literal("lang")
					.requires(source -> true)
					.executes(context -> reloadLang(context.getSource()))
				)
			)
			.then(Commands.literal("logs")
				.then(Commands.literal("startup")
					.requires(source -> true)
					.executes(context -> logs(context.getSource(), ScriptType.STARTUP))
				)
				.then(Commands.literal("server")
					.requires(source -> true)
					.executes(context -> logs(context.getSource(), ScriptType.SERVER))
				)
				.then(Commands.literal("client")
					.requires(source -> true)
					.executes(context -> logs(context.getSource(), ScriptType.CLIENT))
				)
			)
			.then(Commands.literal("browse")
				.executes(source -> {
					Util.getPlatform().openPath(KubeJSPaths.DIRECTORY);
					return 1;
				}));
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(clientCommands("kubejs"));
		dispatcher.register(clientCommands("kjs"));
	}

	private static int logs(CommandSourceStack source, ScriptType type) {
		EditorExt.openFile(type.getLogFile(), 0, 0);
		source.sendSystemMessage(
			Component.literal("The log file for %s scripts can be found at ".formatted(type.name))
				.append(Component.literal("[%s]".formatted(type.getLogFile())).kjs$aqua().kjs$hover(Component.literal("Click to open")).kjs$clickOpenFile(type.getLogFile().toString()))
		);
		return 1;
	}

	private static int reloadClient(CommandSourceStack source) {
		KubeJSClient.reloadClientScripts();
		source.sendSystemMessage(Component.literal("Done! To reload textures, models and other assets, press F3 + T"));
		return 1;
	}

	private static int reloadTextures(CommandSourceStack source) {
		reloadResources(Minecraft.getInstance().getTextureManager());
		return 1;
	}

	private static int reloadLang(CommandSourceStack source) {
		KubeJSClient.reloadClientScripts();
		reloadResources(Minecraft.getInstance().getLanguageManager());
		return 1;
	}

	private static void reloadResources(PreparableReloadListener listener) {
		var mc = Minecraft.getInstance();
		mc.getResourceManager().getResource(GeneratedData.INTERNAL_RELOAD.id());

		var shared = new PreparableReloadListener.SharedState(mc.getResourceManager());
		listener.prepareSharedState(shared);

		PreparableReloadListener.PreparationBarrier barrier = CompletableFuture::completedFuture;

		listener.reload(shared, Util.backgroundExecutor(), barrier, mc)
			.thenAccept(unused -> mc.player.sendSystemMessage(Component.literal("Done! You still may have to reload all assets with F3 + T")));
	}
}
