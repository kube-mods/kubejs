package dev.latvian.mods.kubejs.recipe.component;

import com.mojang.serialization.MapCodec;
import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

@FunctionalInterface
public interface RecipeComponentType<T> {
	ResourceKey<Registry<RecipeComponentType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(KubeJS.id("recipe_component_type"));

	static ResourceKey<RecipeComponentType<?>> key(Identifier id) {
		return ResourceKey.create(REGISTRY_KEY, id);
	}

	@ApiStatus.Internal
	static ResourceKey<RecipeComponentType<?>> builtin(String name) {
		return key(KubeJS.id(name));
	}

	MapCodec<RecipeComponent<?>> mapCodec();
}
