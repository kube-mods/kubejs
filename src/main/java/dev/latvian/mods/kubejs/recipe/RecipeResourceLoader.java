package dev.latvian.mods.kubejs.recipe;

import com.google.gson.JsonElement;
import dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents;
import dev.latvian.mods.kubejs.recipe.special.SpecialRecipeSerializerManager;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.util.Cast;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.ApiStatus;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/// Runs the recipe event on 26.3+, where recipes are a reloadable registry loaded by
/// [net.minecraft.resources.ResourceManagerRegistryLoadTask] instead of a reload listener, and NeoForge's
/// `ModifyRecipeJsonsEvent` no longer exists. Called by `ResourceManagerRegistryLoadTaskMixin` with the listed recipe
/// files, before they are parsed in parallel; the returned map replaces them.
@ApiStatus.Internal
public final class RecipeResourceLoader {
	private static volatile Set<Identifier> removedIds = Set.of();

	private RecipeResourceLoader() {
	}

	public static Map<Identifier, Resource> postRecipeEvent(
		ServerScriptManager ssm,
		ResourceManager manager,
		List<Registry.PendingTags<?>> pendingTags,
		FileToIdConverter lister,
		RegistryOps<JsonElement> ops,
		Map<Identifier, Resource> resources
	) {
		// Make this reload's tags (including the ones scripts changed) visible to ingredients during the event
		for (var pending : pendingTags) {
			pending.apply();
		}

		for (var entry : ssm.getRegistries().cachedRegistryTags.values()) {
			if (entry.registry() instanceof MappedRegistry<?> mappedRegistry) {
				mappedRegistry.bindTags(Cast.to(entry.lookup().bindingMap()));
			}
		}

		// 26.3 builds default data components only after the reloadable registries are loaded, so on the first load no item
		// has components yet and the event cannot create ItemStacks. Build them early from the context registries (the
		// initializers only use static and world registries); ReloadableServerResources builds and applies them again later.
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(ssm.getRegistries().registryAccess()).forEach(DataComponentInitializers.PendingComponents::apply);

		ssm.recipeSchemaStorage.fireEvents(ssm.getRegistries(), manager);

		SpecialRecipeSerializerManager.INSTANCE.reset();
		ServerEvents.SPECIAL_RECIPES.post(ScriptType.SERVER, SpecialRecipeSerializerManager.INSTANCE);

		var jsons = new HashMap<Identifier, JsonElement>(resources.size());
		var sources = new HashMap<Identifier, PackResources>(resources.size());
		// Files that are not valid JSON are passed on untouched, so vanilla reports them as before
		var unreadable = new LinkedHashMap<Identifier, Resource>();

		for (var entry : resources.entrySet()) {
			var id = lister.fileToId(entry.getKey());

			try (var reader = entry.getValue().openAsReader()) {
				jsons.put(id, StrictJsonParser.parse(reader));
				sources.put(id, entry.getValue().source());
			} catch (Exception ex) {
				unreadable.put(entry.getKey(), entry.getValue());
			}
		}

		var event = new RecipesKubeEvent(ssm);

		ScopedValue.where(RecipesKubeEvent.INSTANCE, event).run(() -> {
			ConsoleJS.SERVER.info("Processing recipes...");
			event.post(ops, jsons);
		});

		// Parsing happens on the loader's worker threads, outside the scoped value
		RecipesKubeEvent.PARSING = event;
		removedIds = event.removedRecipes.stream().map(KubeRecipe::getOrCreateId).filter(id -> !jsons.containsKey(id)).collect(Collectors.toUnmodifiableSet());

		var result = new LinkedHashMap<Identifier, Resource>(jsons.size() + unreadable.size());

		for (var entry : jsons.entrySet()) {
			var bytes = entry.getValue().toString().getBytes(StandardCharsets.UTF_8);
			var source = sources.get(entry.getKey());
			result.put(lister.idToFile(entry.getKey()), new Resource(source != null ? source : ssm.internalDataPack, () -> new ByteArrayInputStream(bytes)));
		}

		for (var entry : unreadable.entrySet()) {
			result.putIfAbsent(entry.getKey(), entry.getValue());
			RecipeManager.LOGGER.warn("Recipe file {} from {} is not valid JSON, KubeJS did not see it", entry.getKey(), entry.getValue().sourcePackId());
		}

		return result;
	}

	public static void finishParsing() {
		RecipesKubeEvent.PARSING = null;
	}

	/// Called right before the recipe registry freezes. Recipes removed by scripts are absent from the registry, but other
	/// entries (vanilla's `recipe_unlocked` advancements, mods) may still hold a reference to them, which would fail the
	/// freeze with "Unbound values". Bind just those to [RemovedRecipe]; removed recipes nothing refers to stay absent.
	public static <T> void bindRemovedRecipes(WritableRegistry<T> registry) {
		var ids = removedIds;
		removedIds = Set.of();
		int bound = 0;

		for (var id : ids) {
			var key = ResourceKey.create(registry.key(), id);
			var holder = registry.get(key);

			if (holder.isPresent() && !holder.get().isBound()) {
				registry.register(key, Cast.to(new RemovedRecipe(id)), RegistrationInfo.BUILT_IN);
				bound++;
			}
		}

		if (bound > 0) {
			ConsoleJS.SERVER.info("Kept %d removed recipes as inert kubejs:removed placeholders because other data refers to them".formatted(bound));
		}
	}
}
