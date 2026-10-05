package dev.latvian.mods.kubejs.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.GLFWInputWrapper;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeybindRegistryKubeEvent implements ClientKubeEvent {
	private final List<Builder> builders = new ArrayList<>();
	public final transient Map<Identifier, KeyMapping.Category> categories;
	public final transient KeyMapping.Category mainCategory;

	public KeybindRegistryKubeEvent() {
		this.categories = new LinkedHashMap<>();
		this.mainCategory = categories.computeIfAbsent(KubeJS.id("kubejs"), KeyMapping.Category::new);
	}

	public Builder register(String id) {
		var builder = new Builder(this, id);
		builders.add(builder);
		return builder;
	}

	public Builder register(String id, String defaultKey) {
		return register(id).defaultKey(defaultKey);
	}

	@HideFromJS
	public List<KubeJSKeybinds.KubeKey> build() {
		return builders.stream().map(Builder::create).toList();
	}

	public static class Builder {
		private final KeybindRegistryKubeEvent event;
		private final String id;
		private KeyConflictContext keyConflictContext = KeyConflictContext.UNIVERSAL;
		private KeyModifier modifier = KeyModifier.NONE;
		private InputConstants.Type inputType = InputConstants.Type.KEYSYM;
		private int defaultKey = -1;
		private KeyMapping.Category category;

		private Builder(KeybindRegistryKubeEvent event, String id) {
			this.event = event;
			this.id = id;
			this.category = event.mainCategory;
		}

		public Builder conflictContext(KeyConflictContext keyConflictContext) {
			this.keyConflictContext = keyConflictContext;
			return this;
		}

		public Builder gui() {
			return conflictContext(KeyConflictContext.GUI);
		}

		public Builder inGame() {
			return conflictContext(KeyConflictContext.IN_GAME);
		}

		public Builder modifier(KeyModifier modifier) {
			this.modifier = modifier;
			return this;
		}

		public Builder inputType(InputConstants.Type inputType) {
			this.inputType = inputType;
			return this;
		}

		public Builder scanCodeInputType() {
			return inputType(InputConstants.Type.SCANCODE);
		}

		public Builder mouseInputType() {
			return inputType(InputConstants.Type.MOUSE);
		}

		public Builder defaultKey(String keyName) {
			this.defaultKey = GLFWInputWrapper.get(keyName);
			return this;
		}

		public Builder category(String category) {
			this.category = event.categories.computeIfAbsent(KubeJS.id(category), KeyMapping.Category::new);
			return this;
		}

		@HideFromJS
		public KubeJSKeybinds.KubeKey create() {
			var key = KubeJSKeybinds.getOrCreate(id);
			key.mapping = new KeyMapping(
				"key.kubejs.%s".formatted(id),
				keyConflictContext,
				modifier,
				inputType,
				defaultKey,
				category
			);
			return key;
		}
	}
}
