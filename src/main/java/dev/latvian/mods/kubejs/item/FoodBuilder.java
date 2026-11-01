package dev.latvian.mods.kubejs.item;

import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FoodBuilder {
	private int nutrition;
	private float saturation;
	private boolean alwaysEdible;
	private float eatSeconds;
	private Optional<ItemStack> usingConvertsTo;
	private final List<FoodProperties.PossibleEffect> effects;
	public Consumer<FoodEatenKubeEvent> eaten;

	public FoodBuilder() {
		this.nutrition = 0;
		this.saturation = 0;
		this.alwaysEdible = false;
		this.eatSeconds = 1.6F;
		this.usingConvertsTo = Optional.empty();
		this.effects = new ArrayList<>();
	}

	public FoodBuilder(FoodProperties properties) {
		this.nutrition = properties.nutrition();
		// saturation is a modifier (see docs below), but FoodProperties stores the final value
		this.saturation = properties.nutrition() == 0 ? 0F : properties.saturation() / (properties.nutrition() * 2F);
		this.alwaysEdible = properties.canAlwaysEat();
		this.eatSeconds = properties.eatSeconds();
		this.usingConvertsTo = properties.usingConvertsTo();
		this.effects = new ArrayList<>();
		this.effects.addAll(properties.effects());
	}

	@Info("Sets the hunger restored.")
	public FoodBuilder nutrition(int h) {
		nutrition = h;
		return this;
	}

	@Info("Sets the saturation modifier. Note that the saturation restored is hunger * saturation.")
	public FoodBuilder saturation(float s) {
		saturation = s;
		return this;
	}

	@Info("Sets whether the food is always edible.")
	public FoodBuilder alwaysEdible(boolean flag) {
		alwaysEdible = flag;
		return this;
	}

	@Info("Sets the food is always edible.")
	public FoodBuilder alwaysEdible() {
		return alwaysEdible(true);
	}

	@Info("Sets seconds it takes to eat the food.")
	public FoodBuilder eatSeconds(float seconds) {
		eatSeconds = seconds;
		return this;
	}

	@Info("Sets the food is fast to eat (having half of the eating time).")
	public FoodBuilder fastToEat() {
		return eatSeconds(0.8F);
	}

	public FoodBuilder usingConvertsTo(ItemStack stack) {
		usingConvertsTo = Optional.of(stack);
		return this;
	}

	@Info(value = """
		Adds an effect to the food. Note that the effect duration is in ticks (20 ticks = 1 second).
		""",
		params = {
			@Param(name = "mobEffectId", value = "The id of the effect. Can be either a string or a ResourceLocation."),
			@Param(name = "duration", value = "The duration of the effect in ticks."),
			@Param(name = "amplifier", value = "The amplifier of the effect. 0 means level 1, 1 means level 2, etc."),
			@Param(name = "probability", value = "The probability of the effect being applied. 1 = 100%.")
		})
	public FoodBuilder effect(ResourceLocation mobEffectId, int duration, int amplifier, float probability) {
		effects.add(new FoodProperties.PossibleEffect(new EffectSupplier(mobEffectId, duration, amplifier), probability));
		return this;
	}

	@Info("Removes an effect from the food.")
	public FoodBuilder removeEffect(MobEffect mobEffect) {
		if (mobEffect == null) {
			return this;
		}

		effects.removeIf(e -> e.effectSupplier().get().getEffect().value() == mobEffect);
		return this;
	}

	@Info("""
		Sets a callback that is called when the food is eaten.
		
		Note: This is currently not having effect in `ItemEvents.modification`,
		as firing this callback requires an `ItemBuilder` instance in the `Item`.
		""")
	public FoodBuilder eaten(Consumer<FoodEatenKubeEvent> e) {
		eaten = e;
		return this;
	}

	public FoodProperties build() {
		return new FoodProperties(nutrition, FoodConstants.saturationByModifier(nutrition, saturation), alwaysEdible, eatSeconds, usingConvertsTo, effects);
	}

	private static class EffectSupplier implements Supplier<MobEffectInstance> {
		private final ResourceLocation id;
		private final int duration;
		private final int amplifier;

		private Holder<MobEffect> cachedEffect;

		public EffectSupplier(ResourceLocation id, int duration, int amplifier) {
			this.id = id;
			this.duration = duration;
			this.amplifier = amplifier;
		}

		@Override
		public MobEffectInstance get() {
			if (cachedEffect == null) {
				cachedEffect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);

				if (cachedEffect == null) {
					var effectIds = BuiltInRegistries.MOB_EFFECT.entrySet().stream().map(entry -> entry.getKey().location()).collect(Collectors.toSet());
					throw new RuntimeException(String.format("Missing effect '%s'. Check spelling or maybe potion id was used instead of effect id. Possible ids: %s", id, effectIds));
				}
			}

			return new MobEffectInstance(cachedEffect, duration, amplifier);
		}
	}
}
