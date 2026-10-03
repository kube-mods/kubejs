package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@RemapPrefixForJS("kjs$")
public interface AdvancementNodeKJS {
	default AdvancementNode kjs$self() {
		return (AdvancementNode) this;
	}

	default Identifier kjs$getId() {
		return kjs$self().holder().id();
	}

	@Nullable
	default AdvancementNode kjs$getParent() {
		return kjs$self().parent();
	}

	default Set<AdvancementNode> kjs$getChildren() {
		return (Set) kjs$self().children();
	}

	default void kjs$addChild(AdvancementNode a) {
		kjs$self().addChild(a);
	}

	default Component kjs$getDisplayText() {
		return kjs$self().advancement().name().orElse(Component.empty());
	}

	default boolean kjs$hasDisplay() {
		return kjs$self().advancement().display().isPresent();
	}

	default Component kjs$getTitle() {
		return kjs$self().advancement().display().map(DisplayInfo::title).orElse(Component.empty());
	}

	default Component kjs$getDescription() {
		return kjs$self().advancement().display().map(DisplayInfo::description).orElse(Component.empty());
	}

	@Nullable
	default DisplayInfo kjs$getDisplay() {
		return kjs$self().advancement().display().orElse(null);
	}
}
