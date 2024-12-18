package slimeknights.tconstruct.world;

import net.minecraft.block.PropaguleBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.HugeFungusFeatureConfig;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.feature.TreeFeatureConfig.Builder;
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize;
import net.minecraft.world.gen.foliage.RandomSpreadFoliagePlacer;
import net.minecraft.world.gen.root.RootPlacerType;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;
import net.minecraft.world.gen.stateprovider.RandomizedIntBlockStateProvider;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.gen.treedecorator.AttachedToLeavesTreeDecorator;
import net.minecraft.world.gen.treedecorator.TreeDecoratorType;
import net.minecraft.world.gen.trunk.UpwardsBranchingTrunkPlacer;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.Logger;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.block.SlimeVineBlock;
import slimeknights.tconstruct.world.block.SlimeVineBlock.VineStage;
import slimeknights.tconstruct.world.data.StructureRepalleter;
import slimeknights.tconstruct.world.data.WorldgenDatapackRegistryProvider;
import slimeknights.tconstruct.world.worldgen.islands.IslandPiece;
import slimeknights.tconstruct.world.worldgen.islands.IslandStructure;
import slimeknights.tconstruct.world.worldgen.trees.ExtraRootVariantPlacer;
import slimeknights.tconstruct.world.worldgen.trees.LeaveVineDecorator;
import slimeknights.tconstruct.world.worldgen.trees.SupplierBlockStateProvider;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeFungusConfig;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeTreeConfig;
import slimeknights.tconstruct.world.worldgen.trees.feature.SlimeFungusFeature;
import slimeknights.tconstruct.world.worldgen.trees.feature.SlimeTreeFeature;

import java.util.List;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Contains any logic relevant to structure generation, including trees and islands
 */
@SuppressWarnings("unused")
public final class TinkerStructures extends TinkerModule {
    static final Logger log = Util.getLogger("tinker_structures");
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, TConstruct.MOD_ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPE = DeferredRegister.create(Registry.STRUCTURE_TYPE_REGISTRY, TConstruct.MOD_ID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE = DeferredRegister.create(Registry.STRUCTURE_PIECE_REGISTRY, TConstruct.MOD_ID);
    private static final DeferredRegister<BlockStateProviderType<?>> BLOCK_STATE_PROVIDER_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_STATE_PROVIDER_TYPES, TConstruct.MOD_ID);
    private static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = DeferredRegister.create(Registry.TREE_DECORATOR_TYPE_REGISTRY, TConstruct.MOD_ID);
    private static final DeferredRegister<RootPlacerType<?>> ROOT_PLACERS = DeferredRegister.create(Registry.ROOT_PLACER_TYPE_REGISTRY, TConstruct.MOD_ID);


    public TinkerStructures() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        FEATURES.register(bus);
        STRUCTURE_TYPE.register(bus);
        STRUCTURE_PIECE.register(bus);
        BLOCK_STATE_PROVIDER_TYPES.register(bus);
        TREE_DECORATORS.register(bus);
        ROOT_PLACERS.register(bus);
    }


    /*
     * Misc
     */
    public static final RegistryObject<BlockStateProviderType<SupplierBlockStateProvider>> supplierBlockstateProvider = BLOCK_STATE_PROVIDER_TYPES.register("supplier_state_provider", () -> new BlockStateProviderType<>(SupplierBlockStateProvider.CODEC));
    public static final RegistryObject<TreeDecoratorType<LeaveVineDecorator>> leaveVineDecorator = TREE_DECORATORS.register("leave_vines", () -> new TreeDecoratorType<>(LeaveVineDecorator.CODEC));
    public static final RegistryObject<RootPlacerType<ExtraRootVariantPlacer>> extraRootVariantPlacer = ROOT_PLACERS.register("extra_root_variants", () -> new RootPlacerType<>(ExtraRootVariantPlacer.CODEC));

    /*
     * Features
     */
    /**
     * Overworld variant of slimy trees
     */
    public static final RegistryObject<SlimeTreeFeature> slimeTree = FEATURES.register("slime_tree", () -> new SlimeTreeFeature(SlimeTreeConfig.CODEC));
    /**
     * Nether variant of slimy trees
     */
    public static final RegistryObject<SlimeFungusFeature> slimeFungus = FEATURES.register("slime_fungus", () -> new SlimeFungusFeature(SlimeFungusConfig.CODEC));

    /**
     * Greenheart tree variant
     */
    public static final RegistryObject<ConfiguredFeature<SlimeTreeConfig, SlimeTreeFeature>> earthSlimeTree = CONFIGURED_FEATURES.registerStatic(
            "earth_slime_tree", slimeTree,
            new SlimeTreeConfig.Builder()
                    .planted()
                    .trunk(() -> TinkerWorld.greenheart.getLog().getDefaultState())
                    .leaves(() -> TinkerWorld.slimeLeaves.get(FoliageType.EARTH).getDefaultState())
                    .baseHeight(4).randomHeight(3)
                    .build());
    /**
     * Greenheart tree variant on islands
     */
    public static final RegistryObject<ConfiguredFeature<SlimeTreeConfig, SlimeTreeFeature>> earthSlimeIslandTree = CONFIGURED_FEATURES.registerStatic(
            "earth_slime_island_tree", slimeTree,
            new SlimeTreeConfig.Builder()
                    .trunk(() -> TinkerWorld.greenheart.getLog().getDefaultState())
                    .leaves(() -> TinkerWorld.slimeLeaves.get(FoliageType.EARTH).getDefaultState())
                    .baseHeight(4).randomHeight(3)
                    .build());

    /**
     * Skyroot tree variant
     */
    public static final RegistryObject<ConfiguredFeature<SlimeTreeConfig, SlimeTreeFeature>> skySlimeTree = CONFIGURED_FEATURES.registerStatic(
            "sky_slime_tree", slimeTree,
            new SlimeTreeConfig.Builder()
                    .planted().canDoubleHeight()
                    .trunk(() -> TinkerWorld.skyroot.getLog().getDefaultState())
                    .leaves(() -> TinkerWorld.slimeLeaves.get(FoliageType.SKY).getDefaultState())
                    .build());
    /**
     * Skyroot tree variant on islands
     */
    public static final RegistryObject<ConfiguredFeature<SlimeTreeConfig, SlimeTreeFeature>> skySlimeIslandTree = CONFIGURED_FEATURES.registerStatic(
            "sky_slime_island_tree", slimeTree,
            new SlimeTreeConfig.Builder()
                    .canDoubleHeight()
                    .trunk(() -> TinkerWorld.skyroot.getLog().getDefaultState())
                    .leaves(() -> TinkerWorld.slimeLeaves.get(FoliageType.SKY).getDefaultState())
                    .vines(() -> TinkerWorld.skySlimeVine.get().getDefaultState().with(SlimeVineBlock.STAGE, VineStage.MIDDLE))
                    .build());

    /**
     * Enderslime short tree variant
     */
    public static final RegistryObject<ConfiguredFeature<TreeFeatureConfig, Feature<TreeFeatureConfig>>> enderSlimeTree = CONFIGURED_FEATURES.registerSupplier(
            "ender_slime_tree", () -> Feature.TREE,
            () -> new Builder(BlockStateProvider.of(TinkerWorld.enderbark.getLog()),
                    new UpwardsBranchingTrunkPlacer(2, 1, 4, UniformIntProvider.create(1, 4), 0.5F, UniformIntProvider.create(0, 1), Registry.BLOCK.getOrCreateTag(TinkerTags.Blocks.ENDERBARK_LOGS_CAN_GROW_THROUGH)),
                    BlockStateProvider.of(TinkerWorld.slimeLeaves.get(FoliageType.ENDER)),
                    new RandomSpreadFoliagePlacer(ConstantIntProvider.create(3), ConstantIntProvider.create(0), ConstantIntProvider.create(2), 70),
                    ExtraRootVariantPlacer.builder()
                            .trunkOffset(UniformIntProvider.create(1, 3))
                            .rootBlock(TinkerWorld.enderbarkRoots.get())
                            .canGrowThroughTag(TinkerTags.Blocks.ENDERBARK_ROOTS_CAN_GROW_THROUGH)
                            .slimyRoots(TinkerWorld.slimyEnderbarkRoots)
                            .buildOptional(),
                    new TwoLayersFeatureSize(2, 0, 2))
                    .decorators(List.of(new LeaveVineDecorator(TinkerWorld.enderSlimeVine.get(), 0.125F), new AttachedToLeavesTreeDecorator(0.14F, 1, 0, new RandomizedIntBlockStateProvider(BlockStateProvider.of(TinkerWorld.slimeSapling.get(FoliageType.ENDER).getDefaultState().with(Properties.HANGING, true)), PropaguleBlock.AGE, UniformIntProvider.create(0, 4)), 2, List.of(Direction.DOWN))))
                    .ignoreVines()
                    .build());
    /**
     * Enderslime tall tree variant
     */
    public static final RegistryObject<ConfiguredFeature<TreeFeatureConfig, Feature<TreeFeatureConfig>>> enderSlimeTreeTall = CONFIGURED_FEATURES.registerSupplier(
            "ender_slime_tree_tall", () -> Feature.TREE,
            () -> new Builder(BlockStateProvider.of(TinkerWorld.enderbark.getLog()),
                    new UpwardsBranchingTrunkPlacer(4, 1, 9, UniformIntProvider.create(1, 6), 0.5F, UniformIntProvider.create(0, 1), Registry.BLOCK.getOrCreateTag(TinkerTags.Blocks.ENDERBARK_LOGS_CAN_GROW_THROUGH)),
                    BlockStateProvider.of(TinkerWorld.slimeLeaves.get(FoliageType.ENDER)),
                    new RandomSpreadFoliagePlacer(ConstantIntProvider.create(3), ConstantIntProvider.create(0), ConstantIntProvider.create(2), 70),
                    ExtraRootVariantPlacer.builder()
                            .trunkOffset(UniformIntProvider.create(3, 7))
                            .rootBlock(TinkerWorld.enderbarkRoots.get())
                            .canGrowThroughTag(TinkerTags.Blocks.ENDERBARK_ROOTS_CAN_GROW_THROUGH)
                            .slimyRoots(TinkerWorld.slimyEnderbarkRoots)
                            .buildOptional(),
                    new TwoLayersFeatureSize(3, 0, 2))
                    .decorators(List.of(new LeaveVineDecorator(TinkerWorld.enderSlimeVine.get(), 0.125F), new AttachedToLeavesTreeDecorator(0.14F, 1, 0, new RandomizedIntBlockStateProvider(BlockStateProvider.of(TinkerWorld.slimeSapling.get(FoliageType.ENDER).getDefaultState().with(Properties.HANGING, true)), PropaguleBlock.AGE, UniformIntProvider.create(0, 4)), 2, List.of(Direction.DOWN))))
                    .ignoreVines()
                    .build());
    /**
     * Bloodshroom tree variant
     */
    public static final RegistryObject<ConfiguredFeature<HugeFungusFeatureConfig, SlimeFungusFeature>> bloodSlimeFungus = CONFIGURED_FEATURES.registerSupplier(
            "blood_slime_fungus", slimeFungus,
            () -> new SlimeFungusConfig(
                    TinkerTags.Blocks.SLIMY_SOIL,
                    TinkerWorld.bloodshroom.getLog().defaultBlockState(),
                    TinkerWorld.slimeLeaves.get(FoliageType.BLOOD).defaultBlockState(),
                    TinkerWorld.congealedSlime.get(SlimeType.ICHOR).defaultBlockState(),
                    true));
    /**
     * Bloodshroom island tree variant
     */
    public static final RegistryObject<ConfiguredFeature<HugeFungusFeatureConfig, SlimeFungusFeature>> bloodSlimeIslandFungus = CONFIGURED_FEATURES.registerSupplier(
            "blood_slime_island_fungus", slimeFungus,
            () -> new SlimeFungusConfig(
                    TinkerTags.Blocks.SLIMY_NYLIUM,
                    TinkerWorld.bloodshroom.getLog().defaultBlockState(),
                    TinkerWorld.slimeLeaves.get(FoliageType.BLOOD).defaultBlockState(),
                    TinkerWorld.congealedSlime.get(SlimeType.ICHOR).defaultBlockState(),
                    false));
    /* Deprecated ichor tree */
    public static final RegistryObject<ConfiguredFeature<HugeFungusFeatureConfig, SlimeFungusFeature>> ichorSlimeFungus = CONFIGURED_FEATURES.registerSupplier(
            "ichor_slime_fungus", slimeFungus,
            () -> new SlimeFungusConfig(
                    TinkerTags.Blocks.SLIMY_SOIL,
                    TinkerWorld.bloodshroom.getLog().defaultBlockState(),
                    TinkerWorld.slimeLeaves.get(FoliageType.ICHOR).defaultBlockState(),
                    TinkerWorld.congealedSlime.get(SlimeType.ICHOR).defaultBlockState(),
                    false));

    /*
     * Structures
     */
    public static final RegistryObject<StructurePieceType> islandPiece = STRUCTURE_PIECE.register("island", () -> IslandPiece::new);
    public static final RegistryObject<StructureType<IslandStructure>> island = STRUCTURE_TYPE.register("island", () -> () -> IslandStructure.CODEC);
    // island keys, they are registered in JSON
    public static final RegistryKey<Structure> earthSlimeIsland = RegistryKey.of(Registry.STRUCTURE_REGISTRY, getResource("earth_slime_island"));
    public static final RegistryKey<Structure> skySlimeIsland = RegistryKey.of(Registry.STRUCTURE_REGISTRY, getResource("sky_slime_island"));
    public static final RegistryKey<Structure> clayIsland = RegistryKey.of(Registry.STRUCTURE_REGISTRY, getResource("clay_island"));
    public static final RegistryKey<Structure> bloodIsland = RegistryKey.of(Registry.STRUCTURE_REGISTRY, getResource("blood_island"));
    public static final RegistryKey<Structure> endSlimeIsland = RegistryKey.of(Registry.STRUCTURE_REGISTRY, getResource("end_slime_island"));

    @SubscribeEvent
    void gatherData(final GatherDataEvent event) {
        DataGenerator datagenerator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        boolean server = event.includeServer();
        datagenerator.addProvider(server, new StructureRepalleter(datagenerator, existingFileHelper));
        datagenerator.addProvider(server, new WorldgenDatapackRegistryProvider(datagenerator, existingFileHelper));
        //    datagenerator.addProvider(server, new StructureUpdater(datagenerator, existingFileHelper, TConstruct.MOD_ID, PackType.SERVER_DATA, "structures"));
        //    datagenerator.addProvider(event.includeClient(), new StructureUpdater(datagenerator, existingFileHelper, TConstruct.MOD_ID, PackType.CLIENT_RESOURCES, "book/structures"));
    }
}
