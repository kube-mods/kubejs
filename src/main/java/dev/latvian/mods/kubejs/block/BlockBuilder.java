package dev.latvian.mods.kubejs.block;

import dev.latvian.mods.kubejs.block.callback.BounceRestitutionCallback;
import dev.latvian.mods.kubejs.block.callback.BlockExplodedCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateMirrorCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateModifyCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateModifyPlacementCallback;
import dev.latvian.mods.kubejs.block.callback.BlockStateRotateCallback;
import dev.latvian.mods.kubejs.block.callback.CanBeReplacedCallback;
import dev.latvian.mods.kubejs.block.callback.EntityBlockCallback;
import dev.latvian.mods.kubejs.block.callback.EntityFallenOnBlockCallback;
import dev.latvian.mods.kubejs.block.callback.RandomTickCallback;
import dev.latvian.mods.kubejs.block.drop.BlockDropSupplier;
import dev.latvian.mods.kubejs.block.drop.BlockDrops;
import dev.latvian.mods.kubejs.block.entity.BlockEntityBuilder;
import dev.latvian.mods.kubejs.block.entity.BlockEntityInfo;
import dev.latvian.mods.kubejs.client.ModelGenerator;
import dev.latvian.mods.kubejs.client.MultipartBlockStateGenerator;
import dev.latvian.mods.kubejs.client.VariantBlockStateGenerator;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.AABBWrapper;
import dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.registry.ModelledBuilderBase;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ReturnsSelf
public abstract class BlockBuilder extends ModelledBuilderBase<Block> {
	private static final BlockBehaviour.StatePredicate ALWAYS_FALSE_STATE_PREDICATE = (blockState, blockGetter, blockPos) -> false;
	private static final BlockBehaviour.StateArgumentPredicate<?> ALWAYS_FALSE_STATE_ARG_PREDICATE = (blockState, blockGetter, blockPos, type) -> false;
	private static final BlockBehaviour.StateArgumentPredicate<AABB> ALWAYS_FALSE_AABB_PREDICATE = ((blockState, blockGetter, blockPos, aabb) -> false);

	public transient @Nullable Block copyPropertiesFrom;
	public transient @Nullable SoundType soundType;
	public transient @Nullable Function<BlockState, MapColor> mapColorFn;
	public transient float hardness;
	public transient float resistance;
	public transient float lightLevel;
	public transient boolean opaque;
	public transient boolean fullBlock;
	public transient boolean requiresTool;
	public transient BlockRenderType renderType;
	public transient @Nullable BlockTintFunction tint;
	public transient @Nullable ItemBuilder itemBuilder;
	public transient List<AABB> customShape;
	public transient boolean noCollision;
	public transient boolean notSolid;
	public transient float slipperiness = Float.NaN;
	public transient float speedFactor = Float.NaN;
	public transient float jumpFactor = Float.NaN;
	public @Nullable Consumer<RandomTickCallback> randomTickCallback;
	public @Nullable BlockDropSupplier drops;
	public transient boolean noValidSpawns;
	public transient boolean suffocating;
	public transient boolean viewBlocking;
	public transient boolean redstoneConductor;
	public transient boolean transparent;
	public transient @Nullable NoteBlockInstrument instrument;
	public transient Set<Property<?>> blockStateProperties;
	public transient @Nullable Consumer<BlockStateModifyCallback> defaultStateModification;
	public transient @Nullable Consumer<BlockStateModifyPlacementCallback> placementStateModification;
	public transient @Nullable Predicate<CanBeReplacedCallback> canBeReplacedFunction;
	public transient @Nullable Consumer<EntityBlockCallback> insideCallback;
	public transient @Nullable Consumer<EntityBlockCallback> stepOnCallback;
	public transient @Nullable Consumer<EntityFallenOnBlockCallback> fallOnCallback;
	public transient @Nullable Consumer<BounceRestitutionCallback> bounceRestitutionCallback;
	public transient @Nullable Consumer<BlockExplodedCallback> explodedCallback;
	public transient @Nullable Consumer<BlockStateRotateCallback> rotateStateModification;
	public transient @Nullable Consumer<BlockStateMirrorCallback> mirrorStateModification;
	public transient @Nullable Consumer<BlockRightClickedKubeEvent> rightClick;
	public transient @Nullable BlockEntityInfo blockEntityInfo;

	public BlockBuilder(Identifier id) {
		super(id);
		this.baseTexture = id.withPath(ID.BLOCK).toString();

		this.soundType = null;
		this.mapColorFn = null;
		this.hardness = 1.5F;
		this.resistance = 3F;
		this.lightLevel = 0F;
		this.opaque = true;
		this.fullBlock = false;
		this.requiresTool = false;
		this.renderType = BlockRenderType.SOLID;
		this.itemBuilder = getOrCreateItemBuilder();

		this.customShape = new ArrayList<>();
		this.noCollision = false;
		this.notSolid = false;
		this.randomTickCallback = null;
		this.drops = null;
		this.noValidSpawns = false;
		this.suffocating = true;
		this.viewBlocking = true;
		this.redstoneConductor = true;
		this.transparent = false;
		this.blockStateProperties = new HashSet<>();
		this.defaultStateModification = null;
		this.placementStateModification = null;
		this.canBeReplacedFunction = null;
	}

	@Override
	public Block transformObject(Block obj) {
		obj.kjs$setBlockBuilder(this);
		return obj;
	}

	@Override
	public void createAdditionalObjects(AdditionalObjectRegistry registry) {
		if (itemBuilder != null) {
			registry.add(Registries.ITEM, itemBuilder);
		}

		if (blockEntityInfo != null) {
			registry.add(Registries.BLOCK_ENTITY_TYPE, new BlockEntityBuilder(id, blockEntityInfo));
		}
	}

	@Override
	@Info("""
		Sets the display name for this object, e.g. `Stone`.
		
		This will be overridden by a lang file if it exists.
		""")
	public BuilderBase<Block> displayName(Component name) {
		if (itemBuilder != null) {
			itemBuilder.displayName(name);
		}

		return super.displayName(name);
	}

	@Override
	public void generateData(KubeDataGenerator generator) {
		var table = generateLootTable(generator);

		if (table != null) {
			generator.json(id.withPath(ID.BLOCK_LOOT_TABLE), generator.getRegistries().json().withEncoder(LootTable.CODEC).apply(Holder.direct(table)).getOrThrow());
		}
	}

	@Nullable
	public LootTable generateLootTable(KubeDataGenerator generator) {
		if (drops == BlockDropSupplier.NO_DROPS) {
			return null;
		}

		var blockDrops = drops == null ? BlockDrops.createDefault(get().asItem()) : drops.get();

		if (blockDrops.items().length == 0 && blockDrops.defaultItem() == null) {
			return null;
		}

		var pool = new LootPool.Builder();

		if (blockDrops.rolls() != null) {
			pool.setRolls(Holder.direct(blockDrops.rolls()));
		}

		pool.when(ExplosionCondition.survivesExplosion());

		if (blockDrops.defaultItem() != null) {
			pool.add(LootItem.lootTableItem(blockDrops.defaultItem()));
		}

		for (var drop : blockDrops.items()) {
			var item = LootItem.lootTableItem(drop.getItem());

			if (drop.getCount() > 1) {
				item.apply(SetItemCountFunction.setCount(Holder.direct(new ConstantValue(drop.getCount()))));
			}

			if (!drop.isComponentsPatchEmpty()) {
				item.apply(LootItemConditionalFunction.simpleBuilder(c -> Holder.direct(new SetComponentsFunction(c, drop.getComponentsPatch())).value()));
			}

			pool.add(item);
		}

		return new LootTable.Builder().withPool(pool).build();
	}

	@Override
	public void generateAssets(KubeAssetGenerator generator) {
		if (useMultipartBlockState()) {
			generator.multipartState(id, this::generateMultipartBlockState);
		} else {
			generator.blockState(id, this::generateBlockState);
		}

		generateBlockModels(generator);

		if (itemBuilder != null) {
			if (itemBuilder.hasCustomModel()) {
				itemBuilder.generateItemModels(generator);
			} else {
				generator.itemModel(itemBuilder.id, this::generateItemModel, KubeAssetGenerator.createItemTintSources(itemBuilder.getMaxTintIndex()));
			}
		}
	}

	protected void generateBlockModels(KubeAssetGenerator generator) {
		generator.blockModel(id, m -> {
			if (modelGenerator != null) {
				modelGenerator.accept(m);
				return;
			}

			if (parentModel != null) {
				m.parent(parentModel);
				m.textures(textures);
			} else if (textures.isEmpty()) {
				m.parent(KubeAssetGenerator.CUBE_ALL_BLOCK_MODEL);
				m.texture("all", baseTexture);
			} else {
				m.parent(KubeAssetGenerator.CUBE_BLOCK_MODEL);
				m.textures(textures);
			}

			if (tint != null || !customShape.isEmpty()) {
				var boxes = customShape.isEmpty() ? List.of(AABBWrapper.CUBE) : customShape;

				for (var box : boxes) {
					m.element(e -> {
						e.size(box);

						e.allFaces(face -> {
							face.tex("#" + face.side.getSerializedName());
							face.cull();

							if (tint != null) {
								face.tintindex(0);
							}
						});
					});
				}
			}
			if (renderType != BlockRenderType.SOLID) {
				m.custom(json -> json.addProperty("render_type", switch (renderType) {
					case CUTOUT -> "cutout";
					case TRANSLUCENT -> "translucent";
					default -> "solid";
				}));
			}
		});
	}

	protected void generateItemModel(ModelGenerator m) {
		m.parent(id.withPath(ID.BLOCK));
	}

	protected boolean useMultipartBlockState() {
		return false;
	}

	protected void generateBlockState(VariantBlockStateGenerator bs) {
		bs.simpleVariant("", id.withPath(ID.BLOCK));
	}

	protected void generateMultipartBlockState(MultipartBlockStateGenerator bs) {
	}

	public BlockBuilder copyPropertiesFrom(Block block) {
		copyPropertiesFrom = block;
		return this;
	}

	@Info("Sets the block's sound type. Defaults to wood.")
	public BlockBuilder soundType(@Nullable SoundType m) {
		if (m == null || m == SoundType.EMPTY) {
			soundType = SoundType.EMPTY;
			ConsoleJS.STARTUP.error("Invalid sound type!");
			ConsoleJS.STARTUP.warn("Valid sound types: " + SoundTypeWrapper.INSTANCE.getMap().keySet());
			return this;
		}

		soundType = m;
		return this;
	}

	public BlockBuilder noSoundType() {
		soundType = SoundType.EMPTY;
		return this;
	}

	public BlockBuilder woodSoundType() {
		return soundType(SoundType.WOOD);
	}

	public BlockBuilder stoneSoundType() {
		return soundType(SoundType.STONE);
	}

	public BlockBuilder gravelSoundType() {
		return soundType(SoundType.GRAVEL);
	}

	public BlockBuilder grassSoundType() {
		return soundType(SoundType.GRASS);
	}

	public BlockBuilder sandSoundType() {
		return soundType(SoundType.SAND);
	}

	public BlockBuilder cropSoundType() {
		return soundType(SoundType.CROP);
	}

	public BlockBuilder glassSoundType() {
		return soundType(SoundType.GLASS);
	}

	@Info("Sets the block's map color. Defaults to NONE.")
	public BlockBuilder mapColor(MapColor m) {
		mapColorFn = MapColorHelper.reverse(m);
		return this;
	}

	@Info("Sets the block's map color dynamically per block state. If unset, defaults to NONE.")
	public BlockBuilder dynamicMapColor(@Nullable Function<BlockState, Object> m) {
		mapColorFn = m == null ? MapColorHelper.NONE : s -> MapColorHelper.wrap(m.apply(s));
		return this;
	}

	@Info("""
		Sets the hardness of the block. Defaults to 1.5.
		
		Setting this to -1 will make the block unbreakable like bedrock.
		""")
	public BlockBuilder hardness(float h) {
		hardness = h;
		return this;
	}

	@Info("""
		Sets the blast resistance of the block. Defaults to 3.
		""")
	public BlockBuilder resistance(float r) {
		resistance = r;
		return this;
	}

	@Info("Makes the block unbreakable.")
	public BlockBuilder unbreakable() {
		hardness = -1F;
		resistance = Float.MAX_VALUE;
		return this;
	}

	@Info("Sets the light level of the block. Defaults to 0 (no light).")
	public BlockBuilder lightLevel(float light) {
		lightLevel = light;
		return this;
	}

	@Info("Sets the opacity of the block. Opaque blocks do not let light through.")
	public BlockBuilder opaque(boolean o) {
		opaque = o;
		return this;
	}

	@Info("Sets the block should be a full block or not, like cactus or doors.")
	public BlockBuilder fullBlock(boolean f) {
		fullBlock = f;
		return this;
	}

	@Info("Makes the block require a tool to have drops when broken.")
	public BlockBuilder requiresTool(boolean f) {
		requiresTool = f;
		return this;
	}

	@Info("Makes the block require a tool to have drops when broken.")
	public BlockBuilder requiresTool() {
		return requiresTool(true);
	}

	@Info("""
		Sets the render type of the block. Can be `solid`, `cutout`, or `translucent`.
		""")
	public BlockBuilder renderType(BlockRenderType l) {
		renderType = l;
		return this;
	}

	@Info("""
		Set the color of a specific layer of the block.
		""")
	public BlockBuilder color(int index, BlockTintFunction color) {
		if (!(tint instanceof BlockTintFunction.Mapped)) {
			tint = new BlockTintFunction.Mapped();
		}

		((BlockTintFunction.Mapped) tint).map.put(index, color);
		return this;
	}

	@Info("""
		Set the color of a specific layer of the block.
		""")
	public BlockBuilder color(BlockTintFunction color) {
		tint = color;
		return this;
	}

	@Info("""
		Modifies the block's item representation.
		""")
	public BlockBuilder item(@Nullable Consumer<ItemBuilder> i) {
		if (i == null) {
			itemBuilder = null;
			drops = BlockDropSupplier.NO_DROPS;
		} else {
			if (itemBuilder == null) {
				itemBuilder = getOrCreateItemBuilder();

				ScriptType.STARTUP.console.warn("`item` is called with non-null builder callback after block item is set to null! Creating another block item as fallback.");
			}
			i.accept(itemBuilder);
		}

		return this;
	}

	@HideFromJS
	protected ItemBuilder getOrCreateItemBuilder() {
		return itemBuilder == null ? (itemBuilder = new BlockItemBuilder(this, id)) : itemBuilder;
	}

	@Info("""
		Set the block to have no corresponding item.
		""")
	public BlockBuilder noItem() {
		return item(null);
	}

	@Info("Set the shape of the block.")
	public BlockBuilder box(double x0, double y0, double z0, double x1, double y1, double z1, boolean scale16) {
		if (scale16) {
			customShape.add(new AABB(x0 / 16D, y0 / 16D, z0 / 16D, x1 / 16D, y1 / 16D, z1 / 16D));
		} else {
			customShape.add(new AABB(x0, y0, z0, x1, y1, z1));
		}

		return this;
	}

	@Info("Set the shape of the block.")
	public BlockBuilder box(double x0, double y0, double z0, double x1, double y1, double z1) {
		return box(x0, y0, z0, x1, y1, z1, true);
	}

	public static VoxelShape createShape(List<AABB> boxes) {
		if (boxes.isEmpty()) {
			return Shapes.block();
		}

		var shape = Shapes.create(boxes.getFirst());

		for (var i = 1; i < boxes.size(); i++) {
			shape = Shapes.or(shape, Shapes.create(boxes.get(i)));
		}

		return shape;
	}

	@Info("Makes the block not collide with entities.")
	public BlockBuilder noCollision() {
		noCollision = true;
		return this;
	}

	@Info("Makes the block not be solid.")
	public BlockBuilder notSolid() {
		notSolid = true;
		return this;
	}

	@Info("Makes the block can be waterlogged.")
	public BlockBuilder waterlogged() {
		return property(BlockStateProperties.WATERLOGGED);
	}

	@Info("Checks if the block can be waterlogged.")
	public boolean canBeWaterlogged() {
		return blockStateProperties.contains(BlockStateProperties.WATERLOGGED);
	}

	@Info("Change drops of this block")
	public BlockBuilder drops(@Nullable BlockDropSupplier drops) {
		this.drops = drops == null ? BlockDropSupplier.NO_DROPS : drops;
		return this;
	}

	@Info("Clears all drops for the block.")
	public BlockBuilder noDrops() {
		drops = BlockDropSupplier.NO_DROPS;
		return this;
	}

	@Info("Set how slippery the block is.")
	public BlockBuilder slipperiness(float f) {
		slipperiness = f;
		return this;
	}

	@Info("""
		Set how fast you can walk on the block.
		
		Any value above 1 will make you walk insanely fast as your speed is multiplied by this value each tick.
		
		Recommended values are between 0.1 and 1, useful for mimicking soul sand or ice.
		""")
	public BlockBuilder speedFactor(float f) {
		speedFactor = f;
		return this;
	}

	@Info("Set how high you can jump on the block.")
	public BlockBuilder jumpFactor(float f) {
		jumpFactor = f;
		return this;
	}

	/// Sets random tick callback for this black.
	///
	/// @param randomTickCallback A callback using a block container and a random.
	@Info("Sets random tick callback for this black.")
	public BlockBuilder randomTick(@Nullable Consumer<RandomTickCallback> randomTickCallback) {
		this.randomTickCallback = randomTickCallback;
		return this;
	}

	@Info("Makes mobs not spawn on the block.")
	public BlockBuilder noValidSpawns(boolean b) {
		noValidSpawns = b;
		return this;
	}

	@Info("Makes the block suffocating.")
	public BlockBuilder suffocating(boolean b) {
		suffocating = b;
		return this;
	}

	@Info("Makes the block view blocking.")
	public BlockBuilder viewBlocking(boolean b) {
		viewBlocking = b;
		return this;
	}

	@Info("Makes the block a redstone conductor.")
	public BlockBuilder redstoneConductor(boolean b) {
		redstoneConductor = b;
		return this;
	}

	@Info("Makes the block transparent.")
	public BlockBuilder transparent(boolean b) {
		transparent = b;
		return this;
	}

	@Info("Helper method for setting the render type of the block to `cutout` correctly.")
	public BlockBuilder defaultCutout() {
		return renderType(BlockRenderType.CUTOUT).notSolid().noValidSpawns(true).suffocating(false).viewBlocking(false).redstoneConductor(false).transparent(true);
	}

	@Info("Helper method for setting the render type of the block to `translucent` correctly.")
	public BlockBuilder defaultTranslucent() {
		return defaultCutout().renderType(BlockRenderType.TRANSLUCENT);
	}

	@Info("Note block instrument.")
	public BlockBuilder instrument(NoteBlockInstrument i) {
		instrument = i;
		return this;
	}

	@Override
	@Info("Tags both the block and the item with the given tag.")
	public BlockBuilder tag(Identifier[] tag) {
		return tagBoth(tag);
	}

	@Info("Tags both the block and the item with the given tag.")
	public BlockBuilder tagBoth(Identifier[] tag) {
		tagBlock(tag);
		tagItem(tag);
		return this;
	}

	@Info("Tags the block with the given tag.")
	public BlockBuilder tagBlock(Identifier[] tag) {
		super.tag(tag);
		return this;
	}

	@Info("Tags the item with the given tag.")
	public BlockBuilder tagItem(Identifier[] tag) {
		assert itemBuilder != null;
		itemBuilder.tag(tag);
		return this;
	}

	@Info("Set the default state of the block.")
	public BlockBuilder defaultState(Consumer<BlockStateModifyCallback> callbackJS) {
		defaultStateModification = callbackJS;
		return this;
	}

	@Info("Set the callback for determining the blocks state when placed.")
	public BlockBuilder placementState(Consumer<BlockStateModifyPlacementCallback> callbackJS) {
		placementStateModification = callbackJS;
		return this;
	}

	@Info("Set if the block can be replaced by something else.")
	public BlockBuilder canBeReplaced(Predicate<CanBeReplacedCallback> callbackJS) {
		canBeReplacedFunction = callbackJS;
		return this;
	}

	@Info("""
		Set what happens when an entity is inside the block
		This is called every tick for every entity inside the block, so be careful what you do here.
		This will only be called if the entity's bounding box overlaps with the block's collision.
		""")
	public BlockBuilder entityInside(Consumer<EntityBlockCallback> callbackJS) {
		insideCallback = callbackJS;
		return this;
	}

	@Info("""
		Set what happens when an entity steps on the block
		This is called every tick for every entity standing on the block, so be careful what you do here.
		""")
	public BlockBuilder steppedOn(Consumer<EntityBlockCallback> callbackJS) {
		stepOnCallback = callbackJS;
		return this;
	}

	@Info("Set what happens when an entity falls on the block. Do not use this for moving them, use bounce instead!")
	public BlockBuilder fallenOn(Consumer<EntityFallenOnBlockCallback> callbackJS) {
		fallOnCallback = callbackJS;
		return this;
	}

	@Info("Bounces entities that land on this by restitution * their fall velocity. Negative is clamped to 0")
	public BlockBuilder restitution(float restitution) {
		float clamped = Math.max(0F, restitution);
		bounceRestitutionCallback = ctx -> ctx.restitution(clamped);
		return this;
	}

	@Info("Set the restitution when an entity lands on this. Takes precedence over restitution(float)")
	public BlockBuilder restitution(Consumer<BounceRestitutionCallback> callbackJS) {
		bounceRestitutionCallback = callbackJS;
		return this;
	}

	@Info("Set how this block reacts after an explosion. Note the block has already been destroyed at this point")
	public BlockBuilder exploded(Consumer<BlockExplodedCallback> callbackJS) {
		explodedCallback = callbackJS;
		return this;
	}

	@Info("""
		Add a blockstate property to the block.
		
		For example, facing, lit, etc.
		""")
	public BlockBuilder property(Property<?> property) {
		if (property.getPossibleValues().size() <= 1) {
			throw new IllegalArgumentException(String.format("Block \"%s\" has an illegal Blockstate Property \"%s\" which has <= 1 possible values. (%d possible values)", id, property.getName(), property.getPossibleValues().size()));
		}
		blockStateProperties.add(property);
		return this;
	}

	@Info("Set the callback used for determining how the block rotates")
	public BlockBuilder rotateState(Consumer<BlockStateRotateCallback> callbackJS) {
		rotateStateModification = callbackJS;
		return this;
	}

	@Info("Set the callback used for determining how the block is mirrored")
	public BlockBuilder mirrorState(Consumer<BlockStateMirrorCallback> callbackJS) {
		mirrorStateModification = callbackJS;
		return this;
	}

	@Info("Set the callback used for right-clicking on the block")
	public BlockBuilder rightClick(Consumer<BlockRightClickedKubeEvent> callbackJS) {
		rightClick = callbackJS;
		return this;
	}

	@Info("Creates a Block Entity for this block")
	public BlockBuilder blockEntity(Consumer<BlockEntityInfo> callback) {
		blockEntityInfo = new BlockEntityInfo(this);
		callback.accept(blockEntityInfo);
		return this;
	}

	public Block.Properties createProperties() {
		var properties = new KubeJSBlockProperties(this, copyPropertiesFrom);

		properties.setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), this.id));

		if (soundType != null) {
			properties.sound(soundType);
		}

		if (mapColorFn != null) {
			properties.mapColor(mapColorFn);
		}

		if (resistance >= 0F) {
			properties.strength(hardness, resistance);
		} else {
			properties.strength(hardness);
		}

		if (lightLevel > 0F) {
			properties.lightLevel(state -> (int) (lightLevel * 15F));
		}

		if (noCollision) {
			properties.noCollision();
		}

		if (notSolid) {
			properties.noOcclusion();
		}

		if (requiresTool) {
			properties.requiresCorrectToolForDrops();
		}

		if (drops == BlockDropSupplier.NO_DROPS) {
			properties.noLootTable();
		}

		if (!Float.isNaN(slipperiness)) {
			properties.friction(slipperiness);
		}

		if (!Float.isNaN(speedFactor)) {
			properties.speedFactor(speedFactor);
		}

		if (!Float.isNaN(jumpFactor)) {
			properties.jumpFactor(jumpFactor);
		}

		if (noValidSpawns) {
			properties.isValidSpawn(Cast.to(ALWAYS_FALSE_STATE_ARG_PREDICATE));
		}

		if (!suffocating) {
			properties.isSuffocating(ALWAYS_FALSE_STATE_PREDICATE);
		}

		if (!viewBlocking) {
			properties.isViewBlocking(ALWAYS_FALSE_AABB_PREDICATE);
		}

		if (!redstoneConductor) {
			properties.isRedstoneConductor(ALWAYS_FALSE_STATE_PREDICATE);
		}

		if (randomTickCallback != null) {
			properties.randomTicks();
		}

		if (instrument != null) {
			properties.instrument(instrument);
		}
		return properties;
	}
}