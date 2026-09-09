package dev.latvian.mods.kubejs.recipe.component;

import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.codec.KubeJSCodecs;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.util.OpsContainer;
import dev.latvian.mods.rhino.type.TypeInfo;

public class CharacterComponent extends SimpleRecipeComponent<Character> {
	public static final RecipeComponentType<Character> CHARACTER = RecipeComponentType.unit(KubeJS.id("character"), CharacterComponent::new);

	public CharacterComponent(RecipeComponentType<?> type) {
		super(type, KubeJSCodecs.CHARACTER, TypeInfo.CHARACTER);
	}

	@Override
	public boolean hasPriority(RecipeMatchContext cx, Object from) {
		return from instanceof Character || from instanceof CharSequence || from instanceof JsonPrimitive json && json.isString();
	}

	@Override
	public Character wrap(RecipeScriptContext cx, Object from) {
		// hack: Rhino stores numeric object keys like '0' as integer index properties
		return from instanceof Number ? String.valueOf(from).charAt(0) : super.wrap(cx, from);
	}

	@Override
	public boolean isEmpty(Character value) {
		return value == '\0';
	}

	@Override
	public String toString(OpsContainer ops, Character value) {
		return "'" + value + "'";
	}
}
