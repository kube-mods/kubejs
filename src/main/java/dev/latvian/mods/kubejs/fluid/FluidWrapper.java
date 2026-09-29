package dev.latvian.mods.kubejs.fluid;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.component.DataComponentWrapper;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.RegExpKJS;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.mojang.serialization.DataResult.error;
import static com.mojang.serialization.DataResult.success;

public interface FluidWrapper {
	TypeInfo TYPE_INFO = TypeInfo.of(FluidStack.class);
	TypeInfo FLUID_TYPE_INFO = TypeInfo.of(Fluid.class);
	TypeInfo INGREDIENT_TYPE_INFO = TypeInfo.of(FluidIngredient.class);
	TypeInfo SIZED_INGREDIENT_TYPE_INFO = TypeInfo.of(SizedFluidIngredient.class);

	DataResult<FluidStack> EMPTY_STACK_RESULT = success(FluidStack.EMPTY);
	DataResult<FluidIngredient> EMPTY_INGREDIENT_RESULT = error(() -> "Empty fluid ingredients aren't supported!");
	DataResult<SizedFluidIngredient> EMPTY_SIZED_RESULT = Cast.to(EMPTY_INGREDIENT_RESULT);

	private static DataResult<FluidIngredient> unknownTag(Identifier tag) {
		return DataResult.error(() -> "Fluid tag " + tag + " does not exist!");
	}

	@HideFromJS
	static DataResult<FluidStack> tryWrap(Context cx, @Nullable Object from) {
		while (from instanceof Wrapper w) {
			from = w.unwrap();
		}

		return switch (from) {
			case null -> EMPTY_STACK_RESULT;
			case FluidStack s -> s.isEmpty() ? EMPTY_STACK_RESULT : success(s);
			case Fluid fluid when fluid.kjs$isEmpty() -> EMPTY_STACK_RESULT;
			case Fluid fluid -> success(new FluidStack(fluid, FluidType.BUCKET_VOLUME));
			case FluidIngredient i -> throw new KubeRuntimeException("Using FluidIngredient in places where FluidStack is expected is dangerous and unsupported!").source(SourceLine.of(cx));
			case SizedFluidIngredient sized -> throw new KubeRuntimeException("Using SizedFluidIngredient in places where FluidStack is expected is dangerous and unsupported!").source(SourceLine.of(cx));
			default -> parseString(cx, from.toString());
		};
	}

	@HideFromJS
	static FluidStack wrap(Context cx, @Nullable Object from) {
		return tryWrap(cx, from)
			.getOrThrow(err -> new KubeRuntimeException("Failed to read FluidStack from %s: %s".formatted(from, err))
				.source(SourceLine.of(cx)));
	}

	static FluidIngredient ingredientOf(FluidIngredient of) {
		return of;
	}

	@Info("Returns an ingredient that accepts the given set of fluids under the given component filter.")
	static FluidIngredient ingredientOf(HolderSet<Fluid> base, DataComponentMap data) {
		return ingredientOf(base, data, false);
	}

	@Info("Returns an ingredient that accepts the given set of items under the given (optionally strict) component filter.")
	static FluidIngredient ingredientOf(HolderSet<Fluid> base, DataComponentMap data, boolean strict) {
		return DataComponentFluidIngredient.of(strict, data, base);
	}

	@HideFromJS
	static DataResult<FluidIngredient> tryWrapIngredient(Context cx, Object from) {
		while (from instanceof Wrapper w) {
			from = w.unwrap();
		}

		var registries = RegistryAccessContainer.of(cx);

		return switch (from) {
			case null -> EMPTY_INGREDIENT_RESULT;
			case FluidStack stack when stack.isEmpty() -> EMPTY_INGREDIENT_RESULT;
			case Fluid fluid when fluid.kjs$isEmpty() -> EMPTY_INGREDIENT_RESULT;
			case FluidStack stack -> success(FluidIngredient.of(stack));
			case Fluid fluid -> success(FluidIngredient.of(fluid));
			case FluidIngredient in -> success(in);
			case SizedFluidIngredient s -> success(s.ingredient());
			default -> ingredientOfString(cx, from.toString());
		};
	}

	@HideFromJS
	static FluidIngredient wrapIngredient(Context cx, Object from) {
		return tryWrapIngredient(cx, from)
			.getOrThrow(err -> new KubeRuntimeException("Failed to read FluidIngredient from %s: %s".formatted(from, err))
				.source(SourceLine.of(cx)));
	}

	static SizedFluidIngredient sizedIngredientOf(SizedFluidIngredient of) {
		return of;
	}

	static SizedFluidIngredient sizedIngredientOf(FluidIngredient in, int amount) {
		return new SizedFluidIngredient(in, amount);
	}

	@HideFromJS
	static DataResult<SizedFluidIngredient> tryWrapSizedIngredient(Context cx, @Nullable Object o) {
		var registries = RegistryAccessContainer.of(cx);

		return switch (o) {
			case null -> EMPTY_SIZED_RESULT;
			case FluidStack stack when stack.isEmpty() -> EMPTY_SIZED_RESULT;
			case Fluid fluid when fluid.kjs$isEmpty() -> EMPTY_SIZED_RESULT;
			case FluidStack stack -> success(SizedFluidIngredient.of(stack.getFluid(), stack.getAmount()));
			case Fluid fluid -> success(SizedFluidIngredient.of(fluid, FluidType.BUCKET_VOLUME));
			case FluidIngredient in -> success(new SizedFluidIngredient(in, FluidType.BUCKET_VOLUME));
			case SizedFluidIngredient s -> success(s);
			default -> sizedIngredientOfString(cx, o.toString());
		};
	}

	@HideFromJS
	static SizedFluidIngredient wrapSizedIngredient(Context cx, Object from) {
		return tryWrapSizedIngredient(cx, from)
			.getOrThrow(err -> new KubeRuntimeException("Failed to read SizedFluidIngredient from %s: %s".formatted(from, err))
				.source(SourceLine.of(cx)));
	}

	@Info("Returns a FluidStack of the input")
	static FluidStack of(FluidStack o) {
		return o;
	}

	@Info("Returns a FluidStack of the input, with the specified amount")
	static FluidStack of(FluidStack o, int amount) {
		o.setAmount(amount);
		return o;
	}

	@Info("Returns a FluidStack of the input, with the specified data components")
	static FluidStack of(FluidStack o, DataComponentMap components) {
		o.applyComponents(components);
		return o;
	}

	@Info("Returns a FluidStack of the input, with the specified amount and data components")
	static FluidStack of(FluidStack o, int amount, DataComponentMap components) {
		o.setAmount(amount);
		o.applyComponents(components);
		return o;
	}

	static FluidStack water() {
		return water(FluidType.BUCKET_VOLUME);
	}

	static FluidStack lava() {
		return lava(FluidType.BUCKET_VOLUME);
	}

	static FluidStack water(int amount) {
		return new FluidStack(Fluids.WATER, amount);
	}

	static FluidStack lava(int amount) {
		return new FluidStack(Fluids.LAVA, amount);
	}

	static Fluid getType(Identifier id) {
		return BuiltInRegistries.FLUID.getOptional(id).orElseThrow(() -> new KubeRuntimeException("Unknown fluid: " + id));
	}

	static List<String> getTypes() {
		var types = new ArrayList<String>();

		for (var fluid : BuiltInRegistries.FLUID) {
			types.add(fluid.kjs$getId());
		}

		return types;
	}

	static FluidStack getEmpty() {
		return FluidStack.EMPTY;
	}

	static boolean exists(Identifier id) {
		return BuiltInRegistries.FLUID.containsKey(id);
	}

	static Identifier getId(Fluid fluid) {
		return BuiltInRegistries.FLUID.getKey(fluid);
	}

	interface ReadFn<T> {
		DataResult<T> read(Context cx, StringReader reader) throws CommandSyntaxException;
	}

	static <T> DataResult<T> readWithContext(Context cx, String s, ReadFn<T> fn, String name) {
		try {
			var reader = new StringReader(s);
			reader.skipWhitespace();

			return fn.read(cx, reader);
		} catch (CommandSyntaxException ex) {
			return error(() -> "Error parsing %s from string: %s".formatted(name, ex));
		}
	}

	static DataResult<FluidStack> parseString(Context cx, String s) {
		return switch (s) {
			case "", "-", "empty", "minecraft:empty" -> success(FluidStack.EMPTY);
			default -> readWithContext(cx, s, FluidWrapper::read, "FluidStack");
		};
	}

	static DataResult<FluidStack> read(Context cx, StringReader reader) throws CommandSyntaxException {
		var registries = RegistryAccessContainer.of(cx);

		if (!reader.canRead() || reader.peek() == '-') {
			return success(FluidStack.EMPTY);
		}

		var amount = readFluidAmount(reader);
		var fluid = ID.read(reader).flatMap(FluidWrapper::findFluid);
		var fluidStack = fluid.apply2(FluidStack::new, amount);

		var next = reader.canRead() ? reader.peek() : 0;

		if (next == '[' || next == '{') {
			return fluidStack.flatMap(stack -> {
				try {
					var components = DataComponentWrapper.readPatch(registries.nbt(), reader);
					stack.applyComponents(components);
					return success(stack);
				} catch (CommandSyntaxException e) {
					return error(e::getMessage);
				}
			});
		}

		return fluidStack;
	}

	static DataResult<FluidIngredient> ingredientOfString(Context cx, String s) {
		return switch (s) {
			case "", "-", "empty", "none", "minecraft:empty" -> EMPTY_INGREDIENT_RESULT;
			default -> readWithContext(cx, s, FluidWrapper::readIngredient, "FluidIngredient");
		};
	}

	@HideFromJS
	static DataResult<FluidIngredient> readIngredient(Context cx, StringReader reader) {
		var registries = RegistryAccessContainer.of(cx);

		if (!reader.canRead() || reader.peek() == '-') {
			return EMPTY_INGREDIENT_RESULT;
		}

		return switch (reader.peek()) {
			case '#' -> {
				reader.skip();
				yield ID.read(reader).map(FluidTags::create).flatMap(tagKey ->
					registries.get(tagKey)
						.map(set -> DataResult.success(FluidIngredient.of(set)))
						.orElseGet(() -> unknownTag(tagKey.location()))
				);
			}
			case '@' -> {
				reader.skip();
				var id = reader.readUnquotedString();
				yield success(new NamespaceFluidIngredient(id));
			}
			case '/' -> RegExpKJS.tryRead(reader).map(RegExFluidIngredient::new);
			default -> {
				var fluid = ID.read(reader).flatMap(FluidWrapper::findFluid);

				var next = reader.canRead() ? reader.peek() : 0;

				if (next == '[' || next == '{') {
					try {
						var components = DataComponentWrapper.readPredicate(registries.nbt(), reader);
						yield fluid.map(holder -> DataComponentFluidIngredient.of(false, components, HolderSet.direct(holder)));
					} catch (CommandSyntaxException e) {
						yield error(e::getMessage);
					}
				}

				yield fluid.map(holder -> FluidIngredient.of(holder.value()));
			}
		};
	}

	static DataResult<SizedFluidIngredient> sizedIngredientOfString(Context cx, String s) {
		return switch (s) {
			case "", "-", "empty", "none", "minecraft:empty" -> EMPTY_SIZED_RESULT;
			default -> readWithContext(cx, s, FluidWrapper::readSizedIngredient, "SizedFluidIngredient");
		};
	}

	static DataResult<SizedFluidIngredient> readSizedIngredient(Context cx, StringReader reader) throws CommandSyntaxException {
		if (!reader.canRead()) {
			return EMPTY_SIZED_RESULT;
		}

		var amount = readFluidAmount(reader);
		var ingredient = readIngredient(cx, reader);

		return ingredient.apply2(SizedFluidIngredient::new, amount);
	}

	@HideFromJS
	static DataResult<Holder<Fluid>> findFluid(Identifier id) {
		return BuiltInRegistries.FLUID
			.get(id)
			.map(DataResult::success)
			.orElseGet(() -> error(() -> "Fluid with ID " + id + " does not exist!"))
			.map(Function.identity());
	}

	@HideFromJS
	static DataResult<Integer> readFluidAmount(StringReader reader) throws CommandSyntaxException {
		if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
			var amountd = reader.readDouble();
			reader.skipWhitespace();

			if (reader.canRead() && (reader.peek() == 'b' || reader.peek() == 'B')) {
				reader.skip();
				reader.skipWhitespace();
				amountd *= FluidType.BUCKET_VOLUME;
			}

			if (reader.canRead() && reader.peek() == '/') {
				reader.skip();
				reader.skipWhitespace();
				amountd = amountd / reader.readDouble();
			}

			var amount = (int) amountd;
			reader.expect('x');
			reader.skipWhitespace();

			if (amount < 1) {
				return error(() -> "Fluid amount smaller than 1 is not allowed!");
			}

			return success(amount);
		}

		return success(FluidType.BUCKET_VOLUME);
	}
}