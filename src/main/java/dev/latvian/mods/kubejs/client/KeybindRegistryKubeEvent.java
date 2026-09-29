package dev.latvian.mods.kubejs.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.SDLInputWrapper;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.ArrayList;
import java.util.List;

public class KeybindRegistryKubeEvent implements ClientKubeEvent {
	private final List<Builder> builders = new ArrayList<>();

	public Builder register(String id) {
		var builder = new Builder(id);
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

	@HideFromJS
	public List<Identifier> categories() {
		var out = new ArrayList<Identifier>();
		for (var b : builders) {
			out.add(b.categoryId);
		}
		return out;
	}

	public static class Builder {
		private final String id;
		private KeyConflictContext keyConflictContext = KeyConflictContext.UNIVERSAL;
		private KeyModifier modifier = KeyModifier.NONE;
		private InputConstants.Type inputType = InputConstants.Type.KEYBOARD;
		private int defaultKey = -1;
		private Identifier categoryId;

		private Builder(String id) {
			this.id = id;
			this.categoryId = Identifier.fromNamespaceAndPath("kubejs", "kubejs");
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

		public Builder keyboardInputType() {
			return inputType(InputConstants.Type.KEYBOARD);
		}

		/**
		 * @deprecated <code>InputConstants.Type.SCANCODE</code> was merged into <code>KEYBOARD</code> (keys are SDL scancodes now). Use {@link #keyboardInputType()} instead.
		 */
		@Deprecated
		public Builder scanCodeInputType() {
			return keyboardInputType();
		}

		public Builder mouseInputType() {
			return inputType(InputConstants.Type.MOUSE);
		}

		public Builder defaultKey(String keyName) {
			this.defaultKey = SDLInputWrapper.get(keyName);
			return this;
		}

		public Builder category(String category) {
			this.categoryId = Identifier.fromNamespaceAndPath("kubejs", category);
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
				new KeyMapping.Category(categoryId)
			);
			return key;
		}
	}
}
