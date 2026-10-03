package dev.latvian.mods.kubejs.recipe;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.script.SourceLine;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

import java.util.function.Function;

public class KubeRecipeEventOps<T> extends ConditionalOps<T> {
	private final RecipesKubeEvent event;

	public static final Codec<KubeRecipe> KUBE_RECIPE_CODEC = Codec.of(
		ExtraCodecs.JSON.comap(recipe -> {
			recipe.serialize();
			recipe.json.addProperty("type", recipe.type.idString);
			return recipe.json;
		}),
		new Decoder<>() {
			@Override
			public <V> DataResult<Pair<KubeRecipe, V>> decode(DynamicOps<V> ops, V input) {
				if (ops instanceof KubeRecipeEventOps<?> recipeOps) {
					var json = ops.convertTo(JsonOps.INSTANCE, input);
					return json instanceof JsonObject obj
						? recipeOps.event.parseJson(obj, SourceLine.UNKNOWN).map(recipe -> Pair.of(recipe, ops.empty()))
						: DataResult.error(() -> "Not a JSON object: " + input);
				} else {
					return DataResult.error(() -> "Component requires recipe event context, but wasn't specified!");
				}
			}
		}
	);

	public static final Function<KubeRecipe, KubeRecipe> MARK_SYNTHETIC = r -> {
		r.newRecipe = false;
		return r;
	};

	public static final Codec<KubeRecipe> SYNTHETIC_CODEC = KUBE_RECIPE_CODEC.xmap(MARK_SYNTHETIC, MARK_SYNTHETIC);

	public KubeRecipeEventOps(RecipesKubeEvent event, RegistryOps<T> ops) {
		super(ops, event.registries.context());
		this.event = event;
	}
}
