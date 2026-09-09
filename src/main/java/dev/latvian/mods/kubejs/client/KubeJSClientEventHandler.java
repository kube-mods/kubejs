package dev.latvian.mods.kubejs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.serialization.DynamicOps;
import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.client.highlight.HighlightRenderer;
import dev.latvian.mods.kubejs.command.KubeJSClientCommands;
import dev.latvian.mods.kubejs.fluid.FluidBlockBuilder;
import dev.latvian.mods.kubejs.fluid.FluidBuilder;
import dev.latvian.mods.kubejs.fluid.FluidTypeBuilder;
import dev.latvian.mods.kubejs.gui.KubeJSMenus;
import dev.latvian.mods.kubejs.gui.KubeJSScreen;
import dev.latvian.mods.kubejs.item.DynamicItemTooltipsKubeEvent;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.item.ItemModelPropertiesKubeEvent;
import dev.latvian.mods.kubejs.item.ModifyItemTooltipsKubeEvent;
import dev.latvian.mods.kubejs.plugin.builtin.event.ClientEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.ItemEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.KeyBindEvents;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.PlatformWrapper;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.text.action.DynamicTextAction;
import dev.latvian.mods.kubejs.text.tooltip.ItemTooltipData;
import dev.latvian.mods.kubejs.text.tooltip.TooltipRequirements;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.StackTraceCollector;
import dev.latvian.mods.kubejs.util.Tristate;
import dev.latvian.mods.kubejs.web.LocalWebServer;
import dev.latvian.mods.kubejs.web.WebServerProperties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = KubeJS.MOD_ID, value = Dist.CLIENT)
public class KubeJSClientEventHandler {
	public static final Pattern COMPONENT_ERROR = ConsoleJS.methodPattern(KubeJSClientEventHandler.class, "onItemTooltip");
	private static final List<String> lastComponentError = List.of();

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void setupClient(FMLClientSetupEvent event) {
		KubeJS.PROXY = new KubeJSClient();
		event.enqueueWork(KubeJSClientEventHandler::setupClient0);
	}

	@SubscribeEvent
	public static void addClientPacks(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
			event.addRepositorySource(new KubeJSResourcePackFinder());
		}
	}

	private static void setupClient0() {
		if (!PlatformWrapper.isGeneratingData() && Minecraft.getInstance() != null && WebServerProperties.get().enabled) {
			LocalWebServer.start(Minecraft.getInstance(), true);
		}

		ItemEvents.MODEL_PROPERTIES.post(ScriptType.STARTUP, new ItemModelPropertiesKubeEvent());

		for (var builder : RegistryObjectStorage.BLOCK) {
			if (builder instanceof BlockBuilder b) {
				switch (b instanceof FluidBlockBuilder fb ? fb.fluidBuilder.fluidType.renderType : b.renderType) {
					// TODO: Move these to model json
					case CUTOUT -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.cutout());
					case CUTOUT_MIPPED -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.cutoutMipped());
					case TRANSLUCENT -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.translucent());
				}
			}
		}

		for (var builder : RegistryObjectStorage.FLUID) {
			if (builder instanceof FluidBuilder b) {
				switch (b.fluidType.renderType) {
					case CUTOUT -> {
						ItemBlockRenderTypes.setRenderLayer(b.get().getSource(), RenderType.cutout());
						ItemBlockRenderTypes.setRenderLayer(b.get().getFlowing(), RenderType.cutout());
					}
					case CUTOUT_MIPPED -> {
						ItemBlockRenderTypes.setRenderLayer(b.get().getSource(), RenderType.cutoutMipped());
						ItemBlockRenderTypes.setRenderLayer(b.get().getFlowing(), RenderType.cutoutMipped());
					}
					case TRANSLUCENT -> {
						ItemBlockRenderTypes.setRenderLayer(b.get().getSource(), RenderType.translucent());
						ItemBlockRenderTypes.setRenderLayer(b.get().getFlowing(), RenderType.translucent());
					}
				}
			}
		}

		var list = new ArrayList<ItemTooltipData>();
		ItemEvents.MODIFY_TOOLTIPS.post(ScriptType.CLIENT, new ModifyItemTooltipsKubeEvent(list::add));
		KubeJSClient.clientItemTooltips = List.copyOf(list);
	}

	@SubscribeEvent
	public static void blockColors(RegisterColorHandlersEvent.Block event) {
		for (var builder : RegistryObjectStorage.BLOCK) {
			if (builder instanceof BlockBuilder b && b.tint != null) {
				event.register(new BlockTintFunctionWrapper(b.tint), b.get());
			}
		}
	}

	@SubscribeEvent
	public static void itemColors(RegisterColorHandlersEvent.Item event) {
		for (var builder : RegistryObjectStorage.ITEM) {
			if (builder instanceof ItemBuilder b && b.tint != null) {
				event.register(new ItemTintFunctionWrapper(b.tint), b.get());
			}
		}
	}

	@SubscribeEvent
	public static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(KubeJSMenus.MENU.get(), KubeJSScreen::new);
		ClientEvents.MENU_SCREEN_REGISTRY.post(ScriptType.STARTUP, new MenuScreenRegistryKubeEvent(event));
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		ClientEvents.ENTITY_RENDERER_REGISTRY.post(ScriptType.STARTUP, new EntityRendererRegistryKubeEvent(event));
		ClientEvents.BLOCK_ENTITY_RENDERER_REGISTRY.post(ScriptType.STARTUP, new BlockEntityRendererRegistryKubeEvent(event));
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(HighlightRenderer.keyMapping = new KeyMapping("key.kubejs.kubedex", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.kubejs"));

		var kubeEvent = new KeybindRegistryKubeEvent();
		KeyBindEvents.REGISTRY.post(kubeEvent);

		for (var bind : kubeEvent.build()) {
			event.register(bind.mapping);
		}

		KubeJSKeybinds.triggerReload();
	}

	@SubscribeEvent
	public static void registerCoreShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ID.mc("kubejs/rendertype_highlight"), DefaultVertexFormat.POSITION_COLOR), s -> HighlightRenderer.INSTANCE.highlightShader = s);
	}

	@SubscribeEvent
	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		for (var builder : RegistryObjectStorage.FLUID_TYPE) {
			if (builder instanceof FluidTypeBuilder b) {
				event.registerFluidType(new IClientFluidTypeExtensions() {
					@Override
					public ResourceLocation getStillTexture() {
						return b.actualStillTexture;
					}

					@Override
					public ResourceLocation getFlowingTexture() {
						return b.actualFlowingTexture;
					}

					@Override
					public ResourceLocation getOverlayTexture() {
						return b.blockOverlayTexture;
					}

					@Override
					@Nullable
					public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
						return b.screenOverlayTexture;
					}
				}, b.get());
			}
		}
	}

	@SubscribeEvent
	public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
		if (ClientEvents.PARTICLE_PROVIDER_REGISTRY.hasListeners()) {
			ClientEvents.PARTICLE_PROVIDER_REGISTRY.post(new ParticleProviderRegistryKubeEvent(event));
		}
	}

	@SubscribeEvent
	public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		KubeJSClientCommands.register(event.getDispatcher());
		// TODO: custom client commands...?
	}

	@SubscribeEvent
	public static void debugInfo(CustomizeGuiOverlayEvent.DebugText event) {
		var mc = Minecraft.getInstance();

		if (mc.player != null) {
			if (ClientEvents.DEBUG_LEFT.hasListeners()) {
				ClientEvents.DEBUG_LEFT.post(new DebugInfoKubeEvent(mc.player, event.getLeft()));
			}
			if (ClientEvents.DEBUG_RIGHT.hasListeners()) {
				ClientEvents.DEBUG_RIGHT.post(new DebugInfoKubeEvent(mc.player, event.getRight()));
			}
		}
	}

	private static <T> List<String> appendComponentValue(DynamicOps<Tag> ops, MutableComponent line, DataComponentType<T> type, T value) {
		if (value == null) {
			line.append(Component.literal("null").kjs$red());
			return List.of();
		} else if (value instanceof Component c) {
			line.append(Component.empty().kjs$gold().append(c));
		}

		try {
			var tag = type.codecOrThrow().encodeStart(ops, value).getOrThrow();
			line.append(NbtUtils.toPrettyComponent(tag));
			return List.of();
		} catch (Throwable ex) {
			line.append(Component.literal(String.valueOf(value)).kjs$red());
			var lines = new ArrayList<String>();
			ex.printStackTrace(new StackTraceCollector(lines, COMPONENT_ERROR, ConsoleJS.ERROR_REDUCE));
			return lines;
		}
	}

	public static boolean testRequirements(Minecraft mc, DynamicItemTooltipsKubeEvent event, TooltipRequirements r) {
		if (!r.advanced().test(event.advanced)) {
			return false;
		}

		if (!r.creative().test(event.creative)) {
			return false;
		}

		if (!r.shift().test(event.shift)) {
			return false;
		}

		if (!r.ctrl().test(event.ctrl)) {
			return false;
		}

		if (!r.alt().test(event.alt)) {
			return false;
		}

		if (!r.stages().isEmpty()) {
			var stages = mc.player.kjs$getStages();

			for (var entry : r.stages().entrySet()) {
				if (entry.getValue() != Tristate.DEFAULT && !entry.getValue().test(stages.has(entry.getKey()))) {
					return false;
				}
			}
		}

		return true;
	}

	private static void handleItemTooltips(Minecraft mc, ItemTooltipData tooltip, DynamicItemTooltipsKubeEvent event) {
		if ((tooltip.filter().isEmpty() || tooltip.filter().get().test(event.item)) && (tooltip.requirements().isEmpty() || testRequirements(mc, event, tooltip.requirements().get()))) {
			for (var action : tooltip.actions()) {
				if (action instanceof DynamicTextAction(String id)) {
					try {
						ItemEvents.DYNAMIC_TOOLTIPS.post(ScriptType.CLIENT, id, event);
					} catch (Exception ex) {
						ConsoleJS.CLIENT.error("Item " + event.item.kjs$getId() + " dynamic tooltip error", ex);
					}
				} else {
					action.apply(event.lines);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		var stack = event.getItemStack();

		if (stack.isEmpty()) {
			return;
		}

		var mc = Minecraft.getInstance();
		var lines = event.getToolTip();
		var flags = event.getFlags();
		var sessionData = KubeSessionData.of(mc);

		var dynamicEvent = new DynamicItemTooltipsKubeEvent(stack, flags, lines, sessionData == null);

		for (var tooltip : KubeJSClient.clientItemTooltips) {
			handleItemTooltips(mc, tooltip, dynamicEvent);
		}

		if (sessionData != null) {
			for (var tooltip : sessionData.itemTooltips) {
				handleItemTooltips(mc, tooltip, dynamicEvent);
			}
		}
	}

	@SubscribeEvent
	public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		ScheduledClientEvent.EVENTS.clear();
		ClientEvents.LOGGED_IN.post(ScriptType.CLIENT, new ClientPlayerKubeEvent(event.getPlayer()));
	}

	@SubscribeEvent
	public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientEvents.LOGGED_OUT.post(ScriptType.CLIENT, new ClientPlayerKubeEvent(event.getPlayer()));
	}

	@SubscribeEvent
	public static void hudPostDraw(RenderGuiEvent.Post event) {
		var mc = Minecraft.getInstance();
		HighlightRenderer.INSTANCE.hudPostDraw(mc, event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));

		/*
		if (PlatformWrapper.isDevelopmentEnvironment()) {
			var fb = ImageGenerator.FB_CACHE.get(128);

			if (fb != null) {
				var graphics = event.getGuiGraphics();
				graphics.pose().pushPose();
				graphics.pose().translate(event.getGuiGraphics().guiWidth() - 66F - 3F, 4F, 0F);
				graphics.fill(0, 0, 66, 66, 0xFF000000);
				graphics.fill(1, 1, 65, 65, 0xFF222222);
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderTexture(0, fb.getColorTextureId());
				var m = graphics.pose().last().pose();
				var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
				builder.addVertex(m, 1F, 1F, 10F).setUv(0F, 1F);
				builder.addVertex(m, 1F, 65F, 10F).setUv(0F, 0F);
				builder.addVertex(m, 65F, 65F, 10F).setUv(1F, 0F);
				builder.addVertex(m, 65F, 1F, 10F).setUv(1F, 1F);
				BufferUploader.drawWithShader(builder.buildOrThrow());
				graphics.pose().popPose();
			}
		}
		*/
	}

	@SubscribeEvent
	public static void screenPostDraw(ScreenEvent.Render.Post event) {
		var mc = Minecraft.getInstance();

		if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
			HighlightRenderer.INSTANCE.screen(mc, event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY(), event.getPartialTick());
		}
	}

	@SubscribeEvent
	public static void clientTick(ClientTickEvent.Pre event) {
		var mc = Minecraft.getInstance();
		HighlightRenderer.INSTANCE.tickPre(mc);
		KubeJSKeybinds.triggerKeyEvents(mc);
	}

	@SubscribeEvent
	public static void worldRender(RenderLevelStageEvent event) {
		var mc = Minecraft.getInstance();

		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
			HighlightRenderer.INSTANCE.clearBuffers(mc);
			mc.getMainRenderTarget().bindWrite(true);
		} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			HighlightRenderer.INSTANCE.renderAfterEntities(mc, event);
		} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			HighlightRenderer.INSTANCE.renderAfterLevel(mc, event);
		}
	}

	@Nullable
	public static Screen setScreen(Screen screen) {
		if (screen instanceof TitleScreen && !ConsoleJS.STARTUP.errors.isEmpty() && CommonProperties.get().startupErrorGUI) {
			return new KubeJSErrorScreen(screen, ConsoleJS.STARTUP, false);
		}

		if (screen instanceof TitleScreen && !ConsoleJS.CLIENT.errors.isEmpty() && CommonProperties.get().startupErrorGUI) {
			return new KubeJSErrorScreen(screen, ConsoleJS.CLIENT, false);
		}

		return screen;
	}

	@SubscribeEvent
	public static void guiPostInit(ScreenEvent.Init.Post event) {
		var screen = event.getScreen();

		if (ClientProperties.get().disableRecipeBook && screen instanceof RecipeUpdateListener) {
			var iterator = screen.children().iterator();

			while (iterator.hasNext()) {
				var listener = iterator.next();

				if (listener instanceof ImageButton button && button.sprites.enabled().equals(KubeJSClient.RECIPE_BUTTON_TEXTURE)) {
					screen.renderables.remove(listener);
					screen.narratables.remove(listener);
					iterator.remove();
					return;
				}
			}
		}
	}

	/*private void postAtlasStitch(TextureAtlas atlas) {
		if (!ClientProperties.get().getExportAtlases()) {
			return;
		}

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.getId());
		var w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		var h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		if (w <= 0 || h <= 0) {
			return;
		}

		var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		var pixels = new int[w * h];

		var result = BufferUtils.createIntBuffer(w * h);
		GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, result);
		result.get(pixels);

		image.setRGB(0, 0, w, h, pixels, 0, w);

		var path = KubeJSPaths.EXPORT.resolve(atlas.location().getNamespace() + "/" + atlas.location().getPath());

		if (!Files.exists(path.getParent())) {
			try {
				Files.createDirectories(path.getParent());
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
		}

		if (!Files.exists(path)) {
			try {
				Files.createFile(path);
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
		}

		try (var stream = Files.newOutputStream(path)) {
			ImageIO.write(image, "PNG", stream);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}*/

	// FIXME: implement
	/*private void textureStitch(TextureStitchEvent.Pre event) {
		ClientEvents.ATLAS_SPRITE_REGISTRY.post(new AtlasSpriteRegistryEventJS(event::addSprite), event.getAtlas().location());
	}*/

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void openScreenEvent(ScreenEvent.Opening event) {
		var s = KubeJSClientEventHandler.setScreen(event.getScreen());

		if (s != null && event.getScreen() != s) {
			event.setNewScreen(s);
		}
	}

	@SubscribeEvent
	public static void tagsUpdated(TagsUpdatedEvent event) {
		if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED && Minecraft.getInstance().screen instanceof KubeJSErrorScreen screen && screen.scriptType == ScriptType.SERVER) {
			Minecraft.getInstance().kjs$runCommand("kubejs errors server");
		}
	}
}