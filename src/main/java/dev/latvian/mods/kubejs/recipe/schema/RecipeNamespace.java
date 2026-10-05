package dev.latvian.mods.kubejs.recipe.schema;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

public class RecipeNamespace extends LinkedHashMap<String, RecipeSchemaType> {
	public final RecipeSchemaStorage storage;
	public final String name;
	public final Identifier emptyId;

	public RecipeNamespace(RecipeSchemaStorage storage, String name) {
		this.storage = storage;
		this.name = name;
		this.emptyId = Identifier.fromNamespaceAndPath(name, "empty");
	}

	public RecipeNamespace register(String id, RecipeSchema type) {
		put(id, new RecipeSchemaType(this, emptyId.withPath(id), type));
		return this;
	}

	public RecipeNamespace register(String id, RegistryAwareSchema type) {
		return register(id, type.create(storage.registries()));
	}

	public RecipeNamespace registerBasic(String id, RecipeKey<?>... keys) {
		return register(id, new RecipeSchema(keys));
	}

	public RecipeNamespace shaped(String id) {
		return withExistingParent(id, Identifier.withDefaultNamespace("shaped"));
	}

	public RecipeNamespace shapeless(String id) {
		return withExistingParent(id, Identifier.withDefaultNamespace("shapeless"));
	}

	public RecipeNamespace special(String id) {
		return withExistingParent(id, Identifier.withDefaultNamespace("special"));
	}

	public RecipeNamespace withExistingParent(String id, Identifier parent) {
		return register(id, storage.namespace(parent.getNamespace()).getRegisteredOrThrow(parent.getPath()).schema);
	}

	public RecipeSchemaType getRegisteredOrThrow(String id) {
		var value = get(id);
		if (value != null) {
			return value;
		} else {
			throw new NoSuchElementException("Required schema %s not found!".formatted(id));
		}
	}

	@Override
	public String toString() {
		return name;
	}
}
