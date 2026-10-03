package dev.latvian.mods.kubejs.recipe;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.schema.RecipeFactoryRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeOptional;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaData;
import dev.latvian.mods.kubejs.recipe.schema.function.RecipeSchemaFunction;
import dev.latvian.mods.kubejs.recipe.schema.postprocessing.KeyPatternCleanupPostProcessor;
import dev.latvian.mods.kubejs.recipe.schema.postprocessing.RecipePostProcessor;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Util;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// A base provider for generating recipe schemas.
///
/// **Important!** KubeJS must be added as an existing mod via
/// `programArguments.addAll('--existing-mod', 'kubejs')` in your data gen runs block for this to work!
@ApiStatus.Experimental
public abstract class RecipeSchemaProvider implements DataProvider {

	private final CompletableFuture<HolderLookup.Provider> lookupProvider;
	private final String name;
	private final RegistryAccessContainer registryAccessContainer;
	private final PackOutput.PathProvider path;
	private final ImmutableMap.Builder<Identifier, RecipeSchemaData> map;
	private final ServerScriptManager scriptManager;
	private final Codec<RecipeSchemaData> codec;

	public RecipeSchemaProvider(String name, GatherDataEvent event) {
		this(name, event, RegistryAccessContainer.BUILTIN);
	}

	public RecipeSchemaProvider(String name, GatherDataEvent event, RegistryAccessContainer registryAccessContainer) {
		this.lookupProvider = event.getReloadableLookupProvider();
		this.name = name;
		this.registryAccessContainer = registryAccessContainer;
		path = event.getGenerator().getPackOutput().createPathProvider(PackOutput.Target.DATA_PACK, "kubejs/recipe_schema");
		map = ImmutableMap.builder();
		scriptManager = ServerScriptManager.createForDataGen();
		scriptManager.recipeSchemaStorage.fireEvents(registryAccessContainer, event.getResourceManager(PackType.SERVER_DATA));
		codec = RecipeSchemaData.CODEC;
	}

	public final RegistryAccessContainer registryAccessContainer() {
		return registryAccessContainer;
	}

	public final ServerScriptManager serverScriptManager() {
		return scriptManager;
	}

	public abstract void add(HolderLookup.Provider lookup);

	public void add(Identifier id, RecipeSchemaData schema) {
		map.put(id, schema);
	}

	public void add(Identifier id, Consumer<SchemaDataBuilder> builder) {
		add(id, Util.make(new SchemaDataBuilder(), builder).build());
	}

	public void onlyKeys(Identifier id, RecipeKey<?>... keys) {
		add(id, b -> b.keys(keys));
	}

	public RecipeSchemaData.RecipeKeyData keyData(RecipeKey<?> key) {
		if (key.functionNames == null) {
			key.noFunctions();
		}
		return new RecipeSchemaData.RecipeKeyData(
			key.name,
			key.role,
			key.component,
			Optional.ofNullable(key.optional)
				// this is `null` for default so we can safely ignore it
				.map(RecipeOptional::getInformativeValue)
				.map(value -> key.codec.encodeStart(
					registryAccessContainer.json(),
					Cast.to(value)
				).getOrThrow()),
			key.optional == RecipeOptional.DEFAULT,
			new ArrayList<>(key.names),
			key.excluded,
			key.functionNames,
			key.alwaysWrite
		);
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		return lookupProvider.thenCompose(p -> {
			add(p);
			return CompletableFuture.allOf(
				map.buildOrThrow().entrySet().stream()
					.map(e -> DataProvider.saveStable(output, p, codec, e.getValue(), path.json(e.getKey())))
					.toArray(CompletableFuture[]::new)
			);
		});
	}

	@Override
	public String getName() {
		return name;
	}

	@NullUnmarked
	public class SchemaDataBuilder {
		private Identifier parent, overrideType, recipeFactory;
		private List<RecipeSchemaData.RecipeKeyData> keys;
		private List<RecipeSchemaData.ConstructorData> constructors;
		private Map<String, RecipeSchemaFunction> functions;
		private final Map<String, JsonElement> overrideKeys = new HashMap<>();
		boolean hidden = false;
		private final List<String> mappings = new ArrayList<>();
		private List<String> unique;
		private List<RecipePostProcessor> postProcessors;
		private RecipeSchemaData.MergeData mergeData = RecipeSchemaData.MergeData.DEFAULT;

		/// Sets the parent recipe type, which acts as a fallback/proxy for recipe methods. See vanilla's smoking &
		/// blasting recipe types for examples.
		public SchemaDataBuilder parent(Identifier parent) {
			this.parent = parent;
			return this;
		}

		/// Specifies an alternative recipe serializer to use instead of the one associated with this recipe
		public SchemaDataBuilder overrideType(Identifier type) {
			overrideType = type;
			return this;
		}

		/// Set the [`recipe factory`][dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory] to be used for this recipe
		/// type. See [`#registerRecipeFactories`][dev.latvian.mods.kubejs.plugin.KubeJSPlugin#registerRecipeFactories(RecipeFactoryRegistry)]
		/// for registering custom factories
		public SchemaDataBuilder recipeFactory(Identifier factory) {
			recipeFactory = factory;
			return this;
		}

		/// Adds the `RecipeKey`s, automatically converting them to `RecipeKeyData`s
		public SchemaDataBuilder keys(RecipeKey<?>... keys) {
			return keys(List.of(keys));
		}

		/// Adds the `RecipeKey`s, automatically converting them to `RecipeKeyData`s
		public SchemaDataBuilder keys(List<RecipeKey<?>> keys) {
			return keyDatas(keys.stream().map(RecipeSchemaProvider.this::keyData).toList());
		}

		/// Adds the raw `RecipeKeyData`s
		public SchemaDataBuilder keyDatas(RecipeSchemaData.RecipeKeyData... keys) {
			return keyDatas(List.of(keys));
		}

		/// Adds the raw `RecipeKeyData`s
		public SchemaDataBuilder keyDatas(List<RecipeSchemaData.RecipeKeyData> keys) {
			if (this.keys == null) {
				this.keys = new ArrayList<>(keys);
			} else {
				this.keys.addAll(keys);
			}
			return this;
		}

		/// Add custom constructors which can be used instead of the key-based default
		public SchemaDataBuilder constructors(RecipeSchemaData.ConstructorData... constructors) {
			return constructors(List.of(constructors));
		}

		/// Add custom constructors which can be used instead of the key-based default
		public SchemaDataBuilder constructors(List<RecipeSchemaData.ConstructorData> constructors) {
			if (this.constructors == null) {
				this.constructors = new ArrayList<>(constructors);
			} else {
				this.constructors.addAll(constructors);
			}
			return this;
		}

		/// Add the function to the recipe, used like `event.recipes.my.recipe(<recipe args>).<name>(<function args>)`
		public SchemaDataBuilder function(String name, RecipeSchemaFunction function) {
			return functions(Map.of(name, function));
		}

		/// Add the functions to the recipe, see [#function(String, RecipeSchemaFunction)]
		public SchemaDataBuilder functions(Map<String, RecipeSchemaFunction> functions) {
			if (this.functions == null) {
				this.functions = new HashMap<>(functions);
			} else {
				this.functions.putAll(functions);
			}
			return this;
		}

		/// Specify the default optional value of a key
		public <T> SchemaDataBuilder overrideKey(RecipeKey<T> key, @Nullable T optionalValue) {
			var encoded = optionalValue != null
				? key.codec.encodeStart(registryAccessContainer.json(), optionalValue).getOrThrow()
				: null;
			this.overrideKeys.put(key.name, encoded);
			return this;
		}

		/// Nominally, if this recipe type should be hidden... but this value appears to be unused
		public SchemaDataBuilder hidden() {
			return hidden(true);
		}

		/// Nominally, if this recipe type should be hidden... but this value appears to be unused
		public SchemaDataBuilder hidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		/// The names for accessing this recipe via a special method, e.g. `event.recipes.myMapping(...`
		public SchemaDataBuilder mappings(String... mappings) {
			return mappings(List.of(mappings));
		}

		/// The names for accessing this recipe via a special method, e.g. `event.recipes.myMapping(...`
		public SchemaDataBuilder mappings(List<String> mappings) {
			this.mappings.addAll(mappings);
			return this;
		}

		/// The keys to use when generating a unique id for the recipe
		public SchemaDataBuilder keysForUniqueId(String... keys) {
			return keysForUniqueId(List.of(keys));
		}

		/// The keys to use when generating a unique id for the recipe
		public SchemaDataBuilder keysForUniqueId(List<String> keys) {
			if (unique == null) {
				unique = new ArrayList<>(keys);
			} else {
				unique.addAll(keys);
			}
			return this;
		}

		/// Post processors to apply to recipes, see [`KeyPatternCleanupPostProcessor`][KeyPatternCleanupPostProcessor]
		public SchemaDataBuilder postProcessors(RecipePostProcessor... processors) {
			return postProcessors(List.of(processors));
		}

		/// Post processors to apply to recipes, see [`KeyPatternCleanupPostProcessor`][KeyPatternCleanupPostProcessor]
		public SchemaDataBuilder postProcessors(List<RecipePostProcessor> processors) {
			if (postProcessors == null) {
				postProcessors = new ArrayList<>(processors);
			} else {
				postProcessors.addAll(processors);
			}
			return this;
		}

		/// If values from [`parent schemas`][#parent(Identifier)] should be merged when baking the recipe schema
		public SchemaDataBuilder mergeData(boolean keys, boolean constructors, boolean unique, boolean postProcessors) {
			mergeData = new RecipeSchemaData.MergeData(keys, constructors, unique, postProcessors);
			return this;
		}

		RecipeSchemaData build() {
			return new RecipeSchemaData(
				parent,
				overrideType,
				recipeFactory,
				keys,
				constructors,
				functions,
				overrideKeys,
				hidden,
				mappings,
				unique,
				postProcessors,
				mergeData
			);
		}
	}
}
