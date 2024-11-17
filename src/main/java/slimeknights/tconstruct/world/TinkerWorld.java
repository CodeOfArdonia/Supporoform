package slimeknights.tconstruct.world;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.GeodeCrackConfig;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.GeodeLayerThicknessConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.PlacedFeatures;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;
import slimeknights.mantle.util.SupplierCreativeTab;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.CongealedSlimeBlock;
import slimeknights.tconstruct.world.block.DirtType;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.block.PiglinHeadBlock;
import slimeknights.tconstruct.world.block.PiglinWallHeadBlock;
import slimeknights.tconstruct.world.block.SlimeDirtBlock;
import slimeknights.tconstruct.world.block.SlimeFungusBlock;
import slimeknights.tconstruct.world.block.SlimeGrassBlock;
import slimeknights.tconstruct.world.block.SlimeLeavesBlock;
import slimeknights.tconstruct.world.block.SlimeNyliumBlock;
import slimeknights.tconstruct.world.block.SlimePropaguleBlock;
import slimeknights.tconstruct.world.block.SlimePropaguleLeavesBlock;
import slimeknights.tconstruct.world.block.SlimeRootsBlock;
import slimeknights.tconstruct.world.block.SlimeSaplingBlock;
import slimeknights.tconstruct.world.block.SlimeTallGrassBlock;
import slimeknights.tconstruct.world.block.SlimeVineBlock;
import slimeknights.tconstruct.world.block.SlimeWartBlock;
import slimeknights.tconstruct.world.block.StickySlimeBlock;
import slimeknights.tconstruct.world.data.WorldRecipeProvider;
import slimeknights.tconstruct.world.entity.EnderSlimeEntity;
import slimeknights.tconstruct.world.entity.SkySlimeEntity;
import slimeknights.tconstruct.world.entity.SlimePlacementPredicate;
import slimeknights.tconstruct.world.entity.TerracubeEntity;
import slimeknights.tconstruct.world.item.SlimeGrassSeedItem;
import slimeknights.tconstruct.world.worldgen.trees.SlimeTree;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Contains blocks and items relevant to structures and world gen
 */
@SuppressWarnings("unused")
public final class TinkerWorld extends TinkerModule {

    /**
     * Tab for anything generated in the world
     */
    @SuppressWarnings("WeakerAccess")
    public static final ItemGroup TAB_WORLD = new SupplierCreativeTab(TConstruct.MOD_ID, "world", () -> new ItemStack(TinkerWorld.cobaltOre));
    static final Logger log = Util.getLogger("tinker_world");

    public static final PlantType SLIME_PLANT_TYPE = PlantType.get("slime");

    /*
     * Block base properties
     */
    private static final Item.Settings WORLD_PROPS = new Item.Settings().tab(TAB_WORLD);
    private static final Function<Block, ? extends BlockItem> DEFAULT_BLOCK_ITEM = (b) -> new BlockItem(b, WORLD_PROPS);
    private static final Function<Block, ? extends BlockItem> TOOLTIP_BLOCK_ITEM = (b) -> new BlockTooltipItem(b, WORLD_PROPS);
    private static final Item.Settings HEAD_PROPS = new Item.Settings().tab(TAB_WORLD).rarity(Rarity.UNCOMMON);

    /*
     * Blocks
     */
    // ores
    public static final ItemObject<Block> cobaltOre = BLOCKS.register("cobalt_ore", () -> new Block(builder(Material.STONE, MaterialColor.NETHER, BlockSoundGroup.NETHER_ORE).requiresCorrectToolForDrops().strength(10.0F)), DEFAULT_BLOCK_ITEM);
    public static final ItemObject<Block> rawCobaltBlock = BLOCKS.register("raw_cobalt_block", () -> new Block(builder(Material.STONE, MaterialColor.COLOR_BLUE, BlockSoundGroup.NETHER_ORE).requiresCorrectToolForDrops().strength(6.0f, 7.0f)), DEFAULT_BLOCK_ITEM);
    public static final ItemObject<Item> rawCobalt = ITEMS.register("raw_cobalt", WORLD_PROPS);

    // slime
    public static final EnumObject<SlimeType, SlimeBlock> slime = Util.make(() -> {
        Function<SlimeType, AbstractBlock.Settings> slimeProps = type -> builder(Material.CLAY, type.getMapColor(), BlockSoundGroup.SLIME).friction(0.8F).sound(BlockSoundGroup.SLIME).noOcclusion();
        return new EnumObject.Builder<SlimeType, SlimeBlock>(SlimeType.class)
                .put(SlimeType.EARTH, () -> (SlimeBlock) Blocks.SLIME_BLOCK)
                // sky slime: sticks to anything, but will not pull back
                .put(SlimeType.SKY, BLOCKS.register("sky_slime", () -> new StickySlimeBlock(slimeProps.apply(SlimeType.SKY), (state, other) -> true), TOOLTIP_BLOCK_ITEM))
                // ichor: does not stick to self, but sticks to anything else
                .put(SlimeType.ICHOR, BLOCKS.register("ichor_slime", () -> new StickySlimeBlock(slimeProps.apply(SlimeType.ICHOR).luminance(s -> SlimeType.ICHOR.getLightLevel()),
                        (state, other) -> other.getBlock() != state.getBlock()), TOOLTIP_BLOCK_ITEM))
                // ender: only sticks to self
                .put(SlimeType.ENDER, BLOCKS.register("ender_slime", () -> new StickySlimeBlock(slimeProps.apply(SlimeType.ENDER), (state, other) -> other.getBlock() == state.getBlock()), TOOLTIP_BLOCK_ITEM))
                // blood slime: not sticky, and honey won't stick to it, good for bounce pads
                .build();
    });
    public static final EnumObject<SlimeType, CongealedSlimeBlock> congealedSlime = BLOCKS.registerEnum(SlimeType.values(), "congealed_slime", type -> new CongealedSlimeBlock(builder(Material.CLAY, type.getMapColor(), BlockSoundGroup.SLIME).strength(0.5F).friction(0.5F).lightLevel(s -> type.getLightLevel())), TOOLTIP_BLOCK_ITEM);

    // island blocks
    public static final EnumObject<DirtType, Block> slimeDirt = BLOCKS.registerEnum(DirtType.TINKER, "slime_dirt", (type) -> new SlimeDirtBlock(builder(Material.DIRT, type.getMapColor(), BlockSoundGroup.SLIME).strength(1.9f)), TOOLTIP_BLOCK_ITEM);
    public static final EnumObject<DirtType, Block> allDirt = new EnumObject.Builder<DirtType, Block>(DirtType.class).put(DirtType.VANILLA, () -> Blocks.DIRT).putAll(slimeDirt).build();

    /**
     * Grass variants, the name represents the dirt type
     */
    public static final EnumObject<FoliageType, Block> vanillaSlimeGrass, earthSlimeGrass, skySlimeGrass, enderSlimeGrass, ichorSlimeGrass;
    /**
     * Map of dirt type to slime grass type. Each slime grass is a map from foliage to grass type
     */
    public static final Map<DirtType, EnumObject<FoliageType, Block>> slimeGrass = new EnumMap<>(DirtType.class);

    static {
        Function<FoliageType, AbstractBlock.Settings> slimeGrassProps = type -> builder(Material.GRASS, type.getMapColor(), BlockSoundGroup.SLIME).strength(2.0f).requiresCorrectToolForDrops().randomTicks();
        Function<FoliageType, Block> slimeGrassRegister = type -> type.isNether() ? new SlimeNyliumBlock(slimeGrassProps.apply(type), type) : new SlimeGrassBlock(slimeGrassProps.apply(type), type);
        // blood is not an exact match for vanilla, but close enough
        FoliageType[] values = FoliageType.values();
        vanillaSlimeGrass = BLOCKS.registerEnum(values, "vanilla_slime_grass", slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
        earthSlimeGrass = BLOCKS.registerEnum(values, "earth_slime_grass", slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
        skySlimeGrass = BLOCKS.registerEnum(values, "sky_slime_grass", slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
        enderSlimeGrass = BLOCKS.registerEnum(values, "ender_slime_grass", slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
        ichorSlimeGrass = BLOCKS.registerEnum(values, "ichor_slime_grass", slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
        slimeGrass.put(DirtType.VANILLA, vanillaSlimeGrass);
        slimeGrass.put(DirtType.EARTH, earthSlimeGrass);
        slimeGrass.put(DirtType.SKY, skySlimeGrass);
        slimeGrass.put(DirtType.ENDER, enderSlimeGrass);
        slimeGrass.put(DirtType.ICHOR, ichorSlimeGrass);
    }

    public static final EnumObject<FoliageType, SlimeGrassSeedItem> slimeGrassSeeds = ITEMS.registerEnum(FoliageType.values(), "slime_grass_seeds", type -> new SlimeGrassSeedItem(WORLD_PROPS, type));

    /**
     * Creates a wood variant properties function
     */
    private static Function<WoodVariant, AbstractBlock.Settings> createSlimewood(MaterialColor planks, MaterialColor bark) {
        return type -> switch (type) {
            case WOOD ->
                    AbstractBlock.Settings.create(Material.NETHER_WOOD, bark).sound(BlockSoundGroup.WOOD).requiresCorrectToolForDrops();
            case LOG ->
                    AbstractBlock.Settings.create(Material.NETHER_WOOD, state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? planks : bark).sound(BlockSoundGroup.WOOD).requiresCorrectToolForDrops();
            default -> AbstractBlock.Settings.create(Material.NETHER_WOOD, planks).sound(BlockSoundGroup.SLIME);
        };
    }

    // wood
    public static final WoodBlockObject greenheart = BLOCKS.registerWood("greenheart", createSlimewood(MaterialColor.COLOR_LIGHT_GREEN, MaterialColor.COLOR_GREEN), false, TAB_WORLD);
    public static final WoodBlockObject skyroot = BLOCKS.registerWood("skyroot", createSlimewood(MaterialColor.COLOR_CYAN, MaterialColor.TERRACOTTA_CYAN), false, TAB_WORLD);
    public static final WoodBlockObject bloodshroom = BLOCKS.registerWood("bloodshroom", createSlimewood(MaterialColor.COLOR_RED, MaterialColor.COLOR_ORANGE), false, TAB_WORLD);
    public static final WoodBlockObject enderbark = BLOCKS.registerWood("enderbark", createSlimewood(MaterialColor.COLOR_BLACK, MaterialColor.COLOR_BLACK), false, TAB_WORLD);
    public static final ItemObject<Block> enderbarkRoots = BLOCKS.register("enderbark_roots", () -> new SlimeRootsBlock(AbstractBlock.Settings.create(Material.WOOD, MaterialColor.COLOR_BLACK).strength(0.7F).randomTicks().sound(BlockSoundGroup.MANGROVE_ROOTS).noOcclusion().isSuffocating(Blocks::never).isViewBlocking(Blocks::never).noOcclusion()), DEFAULT_BLOCK_ITEM);
    public static final EnumObject<SlimeType, Block> slimyEnderbarkRoots = BLOCKS.registerEnum(SlimeType.values(), "enderbark_roots", type -> new SlimeDirtBlock(AbstractBlock.Settings.create(Material.DIRT, type.getMapColor()).strength(0.7F).sound(BlockSoundGroup.MUDDY_MANGROVE_ROOTS).lightLevel(s -> type.getLightLevel())), DEFAULT_BLOCK_ITEM);

    // plants
    public static final EnumObject<FoliageType, SlimeTallGrassBlock> slimeFern, slimeTallGrass;

    static {
        Function<FoliageType, AbstractBlock.Settings> props = type -> {
            AbstractBlock.Settings properties;
            if (type.isNether()) {
                properties = builder(Material.REPLACEABLE_FIREPROOF_PLANT, type.getMapColor(), BlockSoundGroup.ROOTS);
            } else {
                properties = builder(Material.REPLACEABLE_PLANT, type.getMapColor(), BlockSoundGroup.GRASS);
            }
            return properties.breakInstantly().noCollision().offset(Block.OffsetType.XYZ);
        };
        slimeFern = BLOCKS.registerEnum(FoliageType.values(), "slime_fern", type -> new SlimeTallGrassBlock(props.apply(type), type), DEFAULT_BLOCK_ITEM);
        slimeTallGrass = BLOCKS.registerEnum(FoliageType.values(), "slime_tall_grass", type -> new SlimeTallGrassBlock(props.apply(type), type), DEFAULT_BLOCK_ITEM);
    }

    public static final EnumObject<FoliageType, FlowerPotBlock> pottedSlimeFern = BLOCKS.registerPottedEnum(FoliageType.values(), "slime_fern", slimeFern);

    // trees
    public static final EnumObject<FoliageType, Block> slimeSapling = Util.make(() -> {
        Function<FoliageType, AbstractBlock.Settings> props = type -> builder(Material.PLANT, type.getMapColor(), type.isNether() ? BlockSoundGroup.FUNGUS : BlockSoundGroup.GRASS).instabreak().noCollission();
        return new EnumObject.Builder<FoliageType, Block>(FoliageType.class)
                .putAll(BLOCKS.registerEnum(FoliageType.OVERWORLD, "slime_sapling", (type) -> new SlimeSaplingBlock(new SlimeTree(type), type, props.apply(type).ticksRandomly()), TOOLTIP_BLOCK_ITEM))
                .put(FoliageType.BLOOD, BLOCKS.register("blood_slime_sapling", () -> new SlimeFungusBlock(props.apply(FoliageType.BLOOD), () -> RegistryEntry.hackyErase(TinkerStructures.bloodSlimeFungus.getHolder().orElseThrow())), TOOLTIP_BLOCK_ITEM))
                .put(FoliageType.ICHOR, BLOCKS.register("ichor_slime_sapling", () -> new SlimeFungusBlock(props.apply(FoliageType.ICHOR), () -> RegistryEntry.hackyErase(TinkerStructures.ichorSlimeFungus.getHolder().orElseThrow())), HIDDEN_BLOCK_ITEM))
                .put(FoliageType.ENDER, BLOCKS.register("ender_slime_sapling", () -> new SlimePropaguleBlock(new SlimeTree(FoliageType.ENDER), FoliageType.ENDER, props.apply(FoliageType.ENDER)), TOOLTIP_BLOCK_ITEM))
                .build();
    });
    public static final EnumObject<FoliageType, FlowerPotBlock> pottedSlimeSapling = BLOCKS.registerPottedEnum(FoliageType.values(), "slime_sapling", slimeSapling);
    public static final EnumObject<FoliageType, Block> slimeLeaves = new EnumObject.Builder<FoliageType, Block>(FoliageType.class)
            .putAll(BLOCKS.registerEnum(FoliageType.OVERWORLD, "slime_leaves", type -> new SlimeLeavesBlock(builder(Material.LEAVES, type.getMapColor(), BlockSoundGroup.GRASS).strength(1.0f).randomTicks().noOcclusion().isValidSpawn(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never), type), DEFAULT_BLOCK_ITEM))
            .putAll(BLOCKS.registerEnum(FoliageType.NETHER, "slime_leaves", type -> new SlimeWartBlock(builder(Material.GRASS, type.getMapColor(), BlockSoundGroup.WART_BLOCK).strength(1.5F).isValidSpawn((s, w, p, e) -> false), type), DEFAULT_BLOCK_ITEM))
            .put(FoliageType.ENDER, BLOCKS.register("ender_slime_leaves", () -> new SlimePropaguleLeavesBlock(builder(Material.LEAVES, FoliageType.ENDER.getMapColor(), BlockSoundGroup.GRASS).strength(1.0f).randomTicks().noOcclusion().isValidSpawn(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never), FoliageType.ENDER), DEFAULT_BLOCK_ITEM))
            .build();

    // slime vines
    public static final ItemObject<SlimeVineBlock> skySlimeVine, enderSlimeVine;

    static {
        Function<SlimeType, AbstractBlock.Settings> props = type -> builder(Material.REPLACEABLE_PLANT, type.getMapColor(), BlockSoundGroup.GRASS).strength(0.75F).noCollission().randomTicks();
        skySlimeVine = BLOCKS.register("sky_slime_vine", () -> new SlimeVineBlock(props.apply(SlimeType.SKY), SlimeType.SKY), DEFAULT_BLOCK_ITEM);
        enderSlimeVine = BLOCKS.register("ender_slime_vine", () -> new SlimeVineBlock(props.apply(SlimeType.ENDER), SlimeType.ENDER), DEFAULT_BLOCK_ITEM);
    }

    // geodes
    // earth
    public static final GeodeItemObject earthGeode = BLOCKS.registerGeode("earth_slime_crystal", MaterialColor.COLOR_LIGHT_GREEN, Sounds.EARTH_CRYSTAL, Sounds.EARTH_CRYSTAL_CHIME.getSound(), Sounds.EARTH_CRYSTAL_CLUSTER, 3, WORLD_PROPS);
    public static final RegistryObject<ConfiguredFeature<GeodeFeatureConfig, Feature<GeodeFeatureConfig>>> configuredEarthGeode = CONFIGURED_FEATURES.registerGeode(
            "earth_geode", earthGeode, BlockStateProvider.of(Blocks.CALCITE), BlockStateProvider.of(Blocks.CLAY),
            new GeodeLayerThicknessConfig(1.7D, 2.2D, 3.2D, 5.2D), new GeodeCrackConfig(0.95D, 2.0D, 2), UniformIntProvider.create(6, 9), UniformIntProvider.create(3, 4), UniformIntProvider.create(1, 2), 16, 1);
    public static final RegistryObject<PlacedFeature> placedEarthGeode = PLACED_FEATURES.registerGeode("earth_geode", configuredEarthGeode, RarityFilterPlacementModifier.of(128), HeightRangePlacementModifier.uniform(YOffset.aboveBottom(6), YOffset.aboveBottom(54)));
    // sky
    public static final GeodeItemObject skyGeode = BLOCKS.registerGeode("sky_slime_crystal", MaterialColor.COLOR_BLUE, Sounds.SKY_CRYSTAL, Sounds.SKY_CRYSTAL_CHIME.getSound(), Sounds.SKY_CRYSTAL_CLUSTER, 0, WORLD_PROPS);
    public static final RegistryObject<ConfiguredFeature<GeodeFeatureConfig, Feature<GeodeFeatureConfig>>> configuredSkyGeode = CONFIGURED_FEATURES.registerGeode(
            "sky_geode", skyGeode, BlockStateProvider.of(Blocks.CALCITE), BlockStateProvider.of(Blocks.MOSSY_COBBLESTONE),
            new GeodeLayerThicknessConfig(1.5D, 2.0D, 3.0D, 4.5D), new GeodeCrackConfig(0.55D, 0.5D, 2), UniformIntProvider.create(3, 4), ConstantIntProvider.create(2), ConstantIntProvider.create(1), 8, 3);
    public static final RegistryObject<PlacedFeature> placedSkyGeode = PLACED_FEATURES.registerGeode("sky_geode", configuredSkyGeode, RarityFilterPlacementModifier.of(64), HeightRangePlacementModifier.uniform(YOffset.fixed(16), YOffset.fixed(54)));
    // ichor
    public static final GeodeItemObject ichorGeode = BLOCKS.registerGeode("ichor_slime_crystal", MaterialColor.COLOR_ORANGE, Sounds.ICHOR_CRYSTAL, Sounds.ICHOR_CRYSTAL_CHIME.getSound(), Sounds.ICHOR_CRYSTAL_CLUSTER, 10, WORLD_PROPS);
    public static final RegistryObject<ConfiguredFeature<GeodeFeatureConfig, Feature<GeodeFeatureConfig>>> configuredIchorGeode = CONFIGURED_FEATURES.registerGeode(
            "ichor_geode", ichorGeode, BlockStateProvider.of(Blocks.CALCITE), BlockStateProvider.of(Blocks.NETHERRACK),
            new GeodeLayerThicknessConfig(1.7D, 2.2D, 3.2D, 4.2D), new GeodeCrackConfig(0.75D, 2.0D, 2), UniformIntProvider.create(4, 6), UniformIntProvider.create(3, 4), UniformIntProvider.create(1, 2), 24, 20);
    public static final RegistryObject<PlacedFeature> placedIchorGeode = PLACED_FEATURES.registerGeode("ichor_geode", configuredIchorGeode, RarityFilterPlacementModifier.of(52), HeightRangePlacementModifier.uniform(YOffset.belowTop(48), YOffset.belowTop(16)));
    // ender
    public static final GeodeItemObject enderGeode = BLOCKS.registerGeode("ender_slime_crystal", MaterialColor.COLOR_PURPLE, Sounds.ENDER_CRYSTAL, Sounds.ENDER_CRYSTAL_CHIME.getSound(), Sounds.ENDER_CRYSTAL_CLUSTER, 7, WORLD_PROPS);
    public static final RegistryObject<ConfiguredFeature<GeodeFeatureConfig, Feature<GeodeFeatureConfig>>> configuredEnderGeode = CONFIGURED_FEATURES.registerGeode(
            "ender_geode", enderGeode, BlockStateProvider.of(Blocks.CALCITE), BlockStateProvider.of(Blocks.END_STONE),
            new GeodeLayerThicknessConfig(1.7D, 2.2D, 3.2D, 5.2D), new GeodeCrackConfig(0.45, 1.0D, 2), UniformIntProvider.create(4, 10), UniformIntProvider.create(3, 4), UniformIntProvider.create(1, 2), 16, 10000);
    public static final RegistryObject<PlacedFeature> placedEnderGeode = PLACED_FEATURES.registerGeode("ender_geode", configuredEnderGeode, RarityFilterPlacementModifier.of(256), HeightRangePlacementModifier.uniform(YOffset.aboveBottom(16), YOffset.aboveBottom(64)));

    // heads
    public static final EnumObject<TinkerHeadType, SkullBlock> heads = BLOCKS.registerEnumNoItem(TinkerHeadType.values(), "head", TinkerWorld::makeHead);
    public static final EnumObject<TinkerHeadType, WallSkullBlock> wallHeads = BLOCKS.registerEnumNoItem(TinkerHeadType.values(), "wall_head", TinkerWorld::makeWallHead);
    public static final EnumObject<TinkerHeadType, VerticallyAttachableBlockItem> headItems = ITEMS.registerEnum(TinkerHeadType.values(), "head", type -> new VerticallyAttachableBlockItem(heads.get(type), wallHeads.get(type), HEAD_PROPS));

    /*
     * Entities
     */
    // our own copy of the slime to make spawning a bit easier
    public static final RegistryObject<EntityType<SkySlimeEntity>> skySlimeEntity = ENTITIES.registerWithEgg("sky_slime", () ->
            EntityType.Builder.create(SkySlimeEntity::new, SpawnGroup.MONSTER)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(20)
                    .sized(2.04F, 2.04F)
                    .setCustomClientFactory((spawnEntity, world) -> TinkerWorld.skySlimeEntity.get().create(world)), 0x47eff5, 0xacfff4);
    public static final RegistryObject<EntityType<EnderSlimeEntity>> enderSlimeEntity = ENTITIES.registerWithEgg("ender_slime", () ->
            EntityType.Builder.create(EnderSlimeEntity::new, SpawnGroup.MONSTER)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(32)
                    .sized(2.04F, 2.04F)
                    .setCustomClientFactory((spawnEntity, world) -> TinkerWorld.enderSlimeEntity.get().create(world)), 0x6300B0, 0xD37CFF);
    public static final RegistryObject<EntityType<TerracubeEntity>> terracubeEntity = ENTITIES.registerWithEgg("terracube", () ->
            EntityType.Builder.create(TerracubeEntity::new, SpawnGroup.MONSTER)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(8)
                    .sized(2.04F, 2.04F)
                    .setCustomClientFactory((spawnEntity, world) -> TinkerWorld.terracubeEntity.get().create(world)), 0xAFB9D6, 0xA1A7B1);

    /*
     * Particles
     */
    public static final RegistryObject<DefaultParticleType> skySlimeParticle = PARTICLE_TYPES.register("sky_slime", () -> new DefaultParticleType(false));
    public static final RegistryObject<DefaultParticleType> enderSlimeParticle = PARTICLE_TYPES.register("ender_slime", () -> new DefaultParticleType(false));
    public static final RegistryObject<DefaultParticleType> terracubeParticle = PARTICLE_TYPES.register("terracube", () -> new DefaultParticleType(false));

    /*
     * Features
     */
    // small veins, standard distribution
    public static RegistryObject<ConfiguredFeature<OreFeatureConfig, Feature<OreFeatureConfig>>> configuredSmallCobaltOre = CONFIGURED_FEATURES.registerSupplier("cobalt_ore_small", () -> Feature.ORE, () -> new OreFeatureConfig(OreFeatures.NETHERRACK, cobaltOre.get().getDefaultState(), 4));
    public static RegistryObject<PlacedFeature> placedSmallCobaltOre = PLACED_FEATURES.register("cobalt_ore_small", configuredSmallCobaltOre, CountPlacementModifier.of(5), SquarePlacementModifier.of(), PlacedFeatures.EIGHT_ABOVE_AND_BELOW_RANGE, BiomePlacementModifier.of());
    // large veins, around y=16, up to 48
    public static RegistryObject<ConfiguredFeature<OreFeatureConfig, Feature<OreFeatureConfig>>> configuredLargeCobaltOre = CONFIGURED_FEATURES.registerSupplier("cobalt_ore_large", () -> Feature.ORE, () -> new OreFeatureConfig(OreFeatures.NETHERRACK, cobaltOre.get().getDefaultState(), 6));
    public static RegistryObject<PlacedFeature> placedLargeCobaltOre = PLACED_FEATURES.register("cobalt_ore_large", configuredLargeCobaltOre, CountPlacementModifier.of(3), SquarePlacementModifier.of(), HeightRangePlacementModifier.trapezoid(YOffset.fixed(8), YOffset.fixed(32)), BiomePlacementModifier.of());


    /*
     * Events
     */

    @SubscribeEvent
    void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(skySlimeEntity.get(), HostileEntity.createHostileAttributes().build());
        event.put(enderSlimeEntity.get(), HostileEntity.createHostileAttributes().build());
        event.put(terracubeEntity.get(), HostileEntity.createHostileAttributes().build());
    }

    /**
     * Sets all fire info for the given wood
     */
    private static void setWoodFireInfo(FireBlock fireBlock, WoodBlockObject wood) {
        // planks
        fireBlock.registerFlammableBlock(wood.get(), 5, 20);
        fireBlock.registerFlammableBlock(wood.getSlab(), 5, 20);
        fireBlock.registerFlammableBlock(wood.getStairs(), 5, 20);
        fireBlock.registerFlammableBlock(wood.getFence(), 5, 20);
        fireBlock.registerFlammableBlock(wood.getFenceGate(), 5, 20);
        // logs
        fireBlock.registerFlammableBlock(wood.getLog(), 5, 5);
        fireBlock.registerFlammableBlock(wood.getStrippedLog(), 5, 5);
        fireBlock.registerFlammableBlock(wood.getWood(), 5, 5);
        fireBlock.registerFlammableBlock(wood.getStrippedWood(), 5, 5);
    }

    @SubscribeEvent
    void registerSpawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(EntityType.SLIME, null, null, new SlimePlacementPredicate<>(TinkerTags.Blocks.EARTH_SLIME_SPAWN), Operation.OR);
        event.register(skySlimeEntity.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.SKY_SLIME_SPAWN), Operation.OR);
        event.register(enderSlimeEntity.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.ENDER_SLIME_SPAWN), Operation.OR);
        event.register(terracubeEntity.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, TerracubeEntity::canSpawnHere, Operation.OR);

    }

    @SubscribeEvent
    void commonSetup(final FMLCommonSetupEvent event) {
        // compostables
        event.enqueueWork(() -> {
            slimeLeaves.forEach((type, block) -> ComposterBlock.add(type.isNether() ? 0.85f : 0.35f, block));
            slimeSapling.forEach(block -> ComposterBlock.add(0.35f, block));
            slimeTallGrass.forEach(block -> ComposterBlock.add(0.35f, block));
            slimeFern.forEach(block -> ComposterBlock.add(0.65f, block));
            slimeGrassSeeds.forEach(block -> ComposterBlock.add(0.35F, block));
            ComposterBlock.add(0.5f, skySlimeVine);
            ComposterBlock.add(0.5f, enderSlimeVine);
            ComposterBlock.add(0.4f, enderbarkRoots);

            // head equipping
            DispenseItemBehavior dispenseArmor = new OptionalDispenseItemBehavior() {
                @Override
                private ItemStack execute(BlockSource source, ItemStack stack) {
                    this.setSuccess(ArmorItem.dispenseArmor(source, stack));
                    return stack;
                }
            };
            TinkerWorld.heads.forEach(head -> DispenserBlock.registerBehavior(head, dispenseArmor));
            // heads in firework stars
            TinkerWorld.heads.forEach(head -> FireworkStarRecipe.SHAPE_BY_ITEM.put(head.asItem(), FireworkRocketItem.Shape.CREEPER));
            // inject heads into the tile entity type
            event.enqueueWork(() -> {
                ImmutableSet.Builder<Block> builder = ImmutableSet.builder();
                builder.addAll(BlockEntityType.SKULL.validBlocks);
                TinkerWorld.heads.forEach(head -> builder.add(head));
                TinkerWorld.wallHeads.forEach(head -> builder.add(head));
                BlockEntityType.SKULL.validBlocks = builder.build();
            });
        });

        // flammability
        event.enqueueWork(() -> {
            FireBlock fireblock = (FireBlock) Blocks.FIRE;
            // plants
            BiConsumer<FoliageType, Block> plantFireInfo = (type, block) -> {
                if (!type.isNether()) {
                    fireblock.setFlammable(block, 30, 60);
                }
            };
            slimeLeaves.forEach(plantFireInfo);
            slimeTallGrass.forEach(plantFireInfo);
            slimeFern.forEach(plantFireInfo);
            // vines
            fireblock.setFlammable(skySlimeVine.get(), 15, 100);
            fireblock.setFlammable(enderSlimeVine.get(), 15, 100);
        });
    }

    @SubscribeEvent
    void gatherData(final GatherDataEvent event) {
        DataGenerator datagenerator = event.getGenerator();
        datagenerator.addProvider(event.includeServer(), new WorldRecipeProvider(datagenerator));
    }


    /* helpers */

    /**
     * Creates a skull block for the given head type
     */
    private static SkullBlock makeHead(TinkerHeadType type) {
        AbstractBlock.Settings props = AbstractBlock.Settings.create(Material.DECORATION).strength(1.0F);
        if (type == TinkerHeadType.PIGLIN || type == TinkerHeadType.PIGLIN_BRUTE || type == TinkerHeadType.ZOMBIFIED_PIGLIN) {
            return new PiglinHeadBlock(type, props);
        }
        return new SkullBlock(type, props);
    }

    /**
     * Creates a skull wall block for the given head type
     */
    private static WallSkullBlock makeWallHead(TinkerHeadType type) {
        AbstractBlock.Settings props = AbstractBlock.Settings.create(Material.DECORATION).strength(1.0F).lootFrom(() -> heads.get(type));
        if (type == TinkerHeadType.PIGLIN || type == TinkerHeadType.PIGLIN_BRUTE || type == TinkerHeadType.ZOMBIFIED_PIGLIN) {
            return new PiglinWallHeadBlock(type, props);
        }
        return new WallSkullBlock(type, props);
    }
}
