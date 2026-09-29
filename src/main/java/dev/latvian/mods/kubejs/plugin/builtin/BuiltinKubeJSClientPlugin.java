package dev.latvian.mods.kubejs.plugin.builtin;

import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.client.KubeJSKeybinds;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.client.ScheduledClientEvent;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.builtin.event.ClientEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.KeyBindEvents;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.SDLInputWrapper;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.PlatformWrapper;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ScheduledEvents;
import dev.latvian.mods.kubejs.web.LocalWebServerAPIRegistry;
import dev.latvian.mods.kubejs.web.LocalWebServerRegistry;
import dev.latvian.mods.kubejs.web.local.client.KubeJSClientWeb;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

public class BuiltinKubeJSClientPlugin implements KubeJSPlugin {
	@Override
	public void registerEvents(EventGroupRegistry registry) {
		registry.register(ClientEvents.GROUP);
		registry.register(KeyBindEvents.GROUP);
	}

	@Override
	public void registerBindings(BindingRegistry bindings) {
		if (bindings.type().isStartup() || bindings.type().isClient()) {
			bindings.addLazy("Client", Minecraft::getInstance);
		}

		if (bindings.type().isClient()) {
			var se = ScheduledClientEvent.EVENTS;
			bindings.add("setTimeout", new ScheduledEvents.TimeoutJSFunction(se, false, false));
			bindings.add("clearTimeout", new ScheduledEvents.TimeoutJSFunction(se, true, false));
			bindings.add("setInterval", new ScheduledEvents.TimeoutJSFunction(se, false, true));
			bindings.add("clearInterval", new ScheduledEvents.TimeoutJSFunction(se, true, true));
		}
		bindings.add("SDLInput", SDLInputWrapper.MAP.get());
	}

	@Override
	public void registerLocalWebServerAPIs(LocalWebServerAPIRegistry registry) {
		KubeJSClientWeb.registerAPIs(registry);
	}

	@Override
	public void registerLocalWebServer(LocalWebServerRegistry registry) {
		KubeJSClientWeb.register(registry);
	}

	@Override
	public void registerLocalWebServerWithAuth(LocalWebServerRegistry registry) {
		KubeJSClientWeb.registerWithAuth(registry);
	}

	@Override
	public void generateLang(LangKubeEvent event) {
		event.add(KubeJS.MOD_ID, "key.categories.kubejs", "KubeJS");
		event.add(KubeJS.MOD_ID, "key.kubejs.kubedex", "Kubedex");

		if (ModList.get().isLoaded("jade")) {
			for (var mod : PlatformWrapper.getMods().values()) {
				if (!mod.getCustomName().isEmpty()) {
					event.add(KubeJS.MOD_ID, "jade.modName." + mod.getId(), mod.getCustomName());
				}
			}
		}

		KubeJSKeybinds.generateLang(event);
	}

	@Override
	public void afterScriptsLoaded(ScriptManager manager) {
		if (manager.scriptType == ScriptType.CLIENT) {
			KubeJSKeybinds.triggerReload();
		}
	}
}
