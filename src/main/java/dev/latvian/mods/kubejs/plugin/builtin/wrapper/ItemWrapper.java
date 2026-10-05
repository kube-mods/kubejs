package dev.latvian.mods.kubejs.plugin.builtin.wrapper;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.component.DataComponentWrapper;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.kubejs.util.Lazy;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Info("Various item related helper methods")
public interface ItemWrapper {
	ItemStack[] EMPTY_ARRAY = new ItemStack[0];
	TypeInfo ITEM_TYPE_INFO = TypeInfo.of(Item.class);
	TypeInfo TYPE_INFO = TypeInfo.of(ItemStack.class);

	@HideFromJS
	@Deprecated(forRemoval = true)
	Lazy<List<String>> CACHED_ITEM_TYPE_LIST = Lazy.of(() -> {
		var cachedItemTypeList = new ArrayList<String>();

		for (var item : BuiltInRegistries.ITEM) {
			cachedItemTypeList.add(item.kjs$getId());
		}

		return cachedItemTypeList;
	});

	@HideFromJS
	@Deprecated(forRemoval = true)
	Lazy<Map<Identifier, Collection<ItemStackTemplate>>> CACHED_ITEM_MAP = Lazy.map(map -> {
		var stackList = ItemStackLinkedSet.createTypeAndComponentsSet();

		stackList.addAll(CreativeModeTabs.searchTab().getDisplayItems());

		for (var stack : stackList) {
			if (!stack.isEmpty()) {
				map.computeIfAbsent(
					stack.getItem().kjs$getIdLocation(),
					_rl -> new ArrayList<>()
				).add(ItemStackTemplate.fromNonEmptyStack(stack.kjs$withCount(1)));
			}
		}

		for (var itemId : CACHED_ITEM_TYPE_LIST.get()) {
			var itemRl = Identifier.parse(itemId);
			map.computeIfAbsent(itemRl, id -> BuiltInRegistries.ITEM.get(id).map(h -> List.of(new ItemStackTemplate(h.value()))).orElse(List.of()));
		}
	});

	@HideFromJS
	@Deprecated(forRemoval = true)
	Lazy<List<ItemStackTemplate>> CACHED_ITEM_LIST = Lazy.of(() -> CACHED_ITEM_MAP.get().values().stream().flatMap(Collection::stream).toList());

	@Info("Returns an ItemStack of the input")
	static ItemStack of(ItemStack in) {
		return in;
	}

	@Info("Returns an ItemStack of the input, with the specified data components")
	static ItemStack of(ItemStack in, DataComponentMap components) {
		in.applyComponents(components);
		return in;
	}

	@Info("Returns an ItemStack of the input, with the specified count")
	static ItemStack of(ItemStack in, int count) {
		return in.kjs$withCount(count);
	}

	@Info("Returns an ItemStack of the input, with the specified count and data components")
	static ItemStack of(ItemStack in, int count, DataComponentMap components) {
		in.setCount(count);
		in.applyComponents(components);
		return in;
	}

	@HideFromJS
	@Nullable
	private static ItemStack wrapTrivial(Context cx, @Nullable Object from) {
		while (from instanceof Wrapper w) {
			from = w.unwrap();
		}

		return switch (from) {
			case null -> ItemStack.EMPTY;
			case ItemStack s -> s.isEmpty() ? ItemStack.EMPTY : s;
			case ItemLike i when i.asItem() == Items.AIR -> ItemStack.EMPTY;
			case Ingredient i -> throw new KubeRuntimeException("Use .first of an ingredient to get its ItemStack!").source(SourceLine.of(cx));
			case SizedIngredient sized -> throw new KubeRuntimeException("Use .ingredient.first on a sized ingredient to get its ItemStack!").source(SourceLine.of(cx));
			case ItemLike i -> i.asItem().getDefaultInstance();
			default -> null;
		};
	}

	@HideFromJS
	static DataResult<ItemStack> wrapResult(Context cx, @Nullable Object from) {
		if (from instanceof Wrapper w) {
			from = w.unwrap();
		}

		var trivial = wrapTrivial(cx, from);
		if (trivial != null) {
			return DataResult.success(trivial);
		}

		var registries = RegistryAccessContainer.of(cx);

		// safe because wrapTrivial has handled the null case
		assert from != null;
		return switch (from) {
			case Identifier id -> findItem(id).map(Holder::value).map(Item::getDefaultInstance);
			case JsonElement json -> parseJson(cx, registries.nbt(), json);
			case StringTag tag -> wrapResult(cx, tag.value());
			case CharSequence charSequence -> {
				var os = from.toString().trim();
				var s = os;
				var cached = registries.itemStackParseCache().get(os);

				if (cached != null) {
					yield DataResult.success(cached.copy());
				}

				int count;
				var spaceIndex = s.indexOf(' ');

				if (spaceIndex >= 2 && s.indexOf('x') == spaceIndex - 1) {
					count = Integer.parseInt(s.substring(0, spaceIndex - 1));
					s = s.substring(spaceIndex + 1);
				} else {
					count = 1;
				}

				yield parseString(cx, registries.nbt(), s)
					.map(stack -> stack.kjs$withCount(count))
					.ifSuccess(stack -> registries.itemStackParseCache().put(os, stack.copy()))
					;
			}
			default -> {
				var map = cx.optionalMapOf(from);

				if (map != null) {
					// TODO: data results instead?
					var item = ItemWrapper.wrapItem(cx, map.get("item"));
					var amount = StringUtilsWrapper.parseInt(map.get("count"), 1);
					var components = DataComponentWrapper.patchOrEmptyOf(cx, map.get("components"));

					yield DataResult.success(new ItemStack(item.kjs$asHolder(), amount, components));
				}

				var invalid = from;
				yield DataResult.error(() -> "Could not parse input %s for item stack".formatted(invalid));
			}
		};
	}

	@HideFromJS
	static ItemStack wrap(Context cx, @Nullable Object from) {
		var trivial = wrapTrivial(cx, from);
		if (trivial != null) {
			return trivial;
		}

		return wrapResult(cx, from)
			.getOrThrow(error -> new KubeRuntimeException("Failed to read item stack from %s: %s".formatted(from, error))
				.source(SourceLine.of(cx)));
	}

	@HideFromJS
	static Item wrapItem(Context cx, @Nullable Object o) {
		return switch (o) {
			case null -> Items.AIR;
			case ItemLike item -> item.asItem();
			case CharSequence cs -> findItem(cs.toString())
				.getOrThrow(error -> new KubeRuntimeException("Failed to read item from %s: %s".formatted(cs, error))
					.source(SourceLine.of(cx)));
			default -> wrap(cx, o).getItem();
		};
	}

	static DataResult<Item> findItem(String s) {
		s = s.trim();
		return switch (s) {
			case "", "-", "air", "minecraft:air" -> DataResult.success(Items.AIR);
			default -> Identifier.read(s).flatMap(ItemWrapper::findItem).map(Holder::value);
		};
	}

	static DataResult<Holder<Item>> findItem(Identifier id) {
		var key = ResourceKey.create(Registries.ITEM, id);

		return BuiltInRegistries.ITEM
			.get(key)
			.map(h -> DataResult.success((Holder<Item>) h))
			.orElseGet(() -> DataResult.error(() -> "Item with ID " + id + " does not exist!"));
	}

	// TODO: remove or rework
	@Info("Get a list of most items in the game. Items not in a creative tab are ignored")
	static List<ItemStackTemplate> getList() {
		return CACHED_ITEM_LIST.get();
	}

	@Info("Get a list of all the item ids in the game")
	// TODO: remove or rework
	static List<String> getTypeList(Context cx) {
		return RegistryAccessContainer.of(cx).item()
			.keySet()
			.stream()
			.map(Identifier::toString)
			.toList();
	}


	@Info("Get the item that represents air/an empty slot")
	static ItemStack getEmpty() {
		return ItemStack.EMPTY;
	}

	@Info("Returns a Firework with the input properties")
	static Fireworks fireworks(Fireworks fireworks) {
		return fireworks;
	}

	@Info("Gets an Item from an item id")
	static Item getItem(Identifier id) {
		return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new KubeRuntimeException("Unknown item: " + id));
	}

	@Info("Gets an items id from the Item")
	static Identifier getId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item);
	}

	@Info("Checks if the provided item id exists in the registry")
	static boolean exists(Identifier id) {
		return BuiltInRegistries.ITEM.containsKey(id);
	}

	@Info("""
		Checks if the passed in object is an ItemStack.
		Note that this does not mean it will not function as an ItemStack if passed to something that requests one.
		""")
	static boolean isItem(@Nullable Object o) {
		return o instanceof ItemStack;
	}

	static ItemStack playerHead(String name) {
		var stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(name));
		return stack;
	}

	static ItemStack playerHeadFromBase64(@Nullable UUID uuid, @Nullable String textureBase64) {
		if (uuid == null || uuid.equals(Util.NIL_UUID)) {
			throw new IllegalArgumentException("UUID can't be null!");
		}

		if (textureBase64 == null || textureBase64.isBlank()) {
			throw new IllegalArgumentException("Texture Base 64 can't be empty!");
		}

		var stack = new ItemStack(Items.PLAYER_HEAD);

		var props = PropertyMap.EMPTY;
		props = new PropertyMap(ImmutableMultimap.of(
			"textures", new Property("textures", textureBase64)
		));

		var profile = new GameProfile(uuid, "", props);
		stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
		return stack;
	}


	static ItemStack playerHeadFromUrl(String url) {
		var root = new JsonObject();
		var textures = new JsonObject();
		var skin = new JsonObject();
		skin.addProperty("url", url);
		textures.add("SKIN", skin);
		root.add("textures", textures);
		var bytes = JsonUtils.toString(root).getBytes(StandardCharsets.UTF_8);
		return playerHeadFromBase64(UUID.nameUUIDFromBytes(bytes), Base64.getEncoder().encodeToString(bytes));
	}

	static ItemStack playerHeadFromSkinHash(String hash) {
		return playerHeadFromUrl("https://textures.minecraft.net/texture/" + hash);
	}

	static @Nullable ItemAbility wrapItemAbility(@Nullable Object object) {
		if (object instanceof ItemAbility ta) {
			return ta;
		} else if (object != null) {
			return ItemAbility.get(object.toString());
		} else {
			return null;
		}
	}

	static boolean isItemStackLike(@Nullable Object from) {
		return from instanceof ItemStack || from instanceof ItemLike;
	}

	static DataResult<ItemStack> parseJson(Context cx, DynamicOps<Tag> registryOps, @Nullable JsonElement json) {
		return switch (json) {
			case null -> DataResult.success(ItemStack.EMPTY);
			case JsonNull jsonNull -> DataResult.success(ItemStack.EMPTY);
			case JsonPrimitive primitive -> parseString(cx, registryOps, primitive.getAsString());
			case JsonObject obj -> ItemStack.OPTIONAL_CODEC.decode(JsonOps.INSTANCE, obj).map(Pair::getFirst);
			default -> DataResult.error(() -> "Could not parse item stack from JSON " + json);
		};
	}

	static DataResult<ItemStack> parseString(Context cx, DynamicOps<Tag> registryOps, String s) {
		return switch (s) {
			case "", "-", "air", "minecraft:air" -> DataResult.success(ItemStack.EMPTY);
			default -> {
				try {
					yield read(cx, registryOps, new StringReader(s));
				} catch (CommandSyntaxException ex) {
					yield DataResult.error(() -> "Error parsing item from string: " + ex);
				}
			}
		};
	}

	static DataResult<ItemStack> read(Context cx, DynamicOps<Tag> registryOps, StringReader reader) throws CommandSyntaxException {
		reader.skipWhitespace();

		if (!reader.canRead() || reader.peek() == '-') {
			return DataResult.success(ItemStack.EMPTY);
		}

		int count;

		if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
			count = Mth.ceil(reader.readDouble());
			reader.skipWhitespace();
			reader.expect('x');
			reader.skipWhitespace();

			if (count < 1) {
				return DataResult.error(() -> "Item count smaller than 1 is not allowed!");
			}
		} else {
			count = 1;
		}

		var itemStack = ID.read(reader)
			.flatMap(ItemWrapper::findItem)
			.map(item -> new ItemStack(item, count));

		var next = reader.canRead() ? reader.peek() : 0;

		if (next == '[' || next == '{') {
			return itemStack.flatMap(stack -> {
				try {
					var components = DataComponentWrapper.readPatch(registryOps, reader);
					stack.applyComponents(components);
					return DataResult.success(stack);
				} catch (CommandSyntaxException e) {
					return DataResult.error(e::getMessage);
				}
			});
		}

		return itemStack;
	}
}