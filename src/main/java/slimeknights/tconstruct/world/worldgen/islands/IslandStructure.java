package slimeknights.tconstruct.world.worldgen.islands;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.block.Block;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DataPool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.FoliageType;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Base logic for all island variants
 */
public class IslandStructure extends Structure {
    public static final Codec<IslandStructure> ENTRY_CODEC = RecordCodecBuilder.create(inst ->
            inst.group(configCodecBuilder(inst)).and(inst.group(
                            IslandPlacement.CODEC.fieldOf("placement").forGetter(s -> s.placement),
                            DataPool.createCodec(Identifier.CODEC).fieldOf("templates").forGetter(s -> s.templates),
                            DataPool.createCodec(ConfiguredFeature.REGISTRY_CODEC).fieldOf("trees").forGetter(s -> s.trees),
                            Registry.BLOCK.byNameCodec().optionalFieldOf("vines").forGetter(s -> s.vines),
                            DataPool.createCodec(Registry.BLOCK.byNameCodec()).fieldOf("grasses").forGetter(s -> s.grasses)))
                    .apply(inst, IslandStructure::new));

    @Getter
    private final IslandPlacement placement;
    private final DataPool<Identifier> templates;
    private final DataPool<RegistryEntry<ConfiguredFeature<?, ?>>> trees;
    private final Optional<Block> vines;
    @Getter
    private final DataPool<Block> grasses;

    public IslandStructure(Config settings, IslandPlacement placement, DataPool<Identifier> templates, DataPool<RegistryEntry<ConfiguredFeature<?, ?>>> trees, Optional<Block> vines, DataPool<Block> grasses) {
        super(settings);
        this.placement = placement;
        this.templates = templates;
        this.trees = trees;
        this.vines = vines;
        this.grasses = grasses;
    }

    @Override
    public StructureType<?> getType() {
        return TinkerStructures.island.get();
    }

    /**
     * Gets the vines for this island
     */
    @Nullable
    public Block getVines() {
        return vines.orElse(null);
    }

    @Override
    public Optional<StructurePosition> getStructurePosition(Context context) {
        // get height
        // biome check
//    BlockPos targetPos = context.chunkPos().getMiddleBlockPosition(height);
//    if (!context.validBiome().test(generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(targetPos.getX()), QuartPos.fromBlock(targetPos.getY()), QuartPos.fromBlock(targetPos.getZ()), context.randomState().sampler()))) {
//      return Optional.empty();
//    }

        // find variant
        return getStructurePosition(context, Type.WORLD_SURFACE, builder -> {
            this.generatePieces(builder, context);
        });
    }

    private void generatePieces(StructurePiecesCollector builder, Structure.Context context) {
        Random random = context.random();
        Optional<Identifier> template = templates.getDataOrEmpty(random);
        if (template.isPresent()) {
            BlockRotation rotation = BlockRotation.random(random);
            int height = placement.getHeight(context.chunkPos(), context.chunkGenerator(), context.world(), rotation, random, context.noiseConfig());
            BlockPos targetPos = context.chunkPos().getCenterAtY(height);
            BlockMirror mirror = Util.getRandom(BlockMirror.values(), random);
            builder.addPiece(new IslandPiece(context.structureTemplateManager(), this, template.get(), targetPos, trees.getDataOrEmpty(random).map(RegistryEntry::value).orElse(null), rotation, mirror));
        }
    }


    /* Builder */

    /**
     * Creates a builder for a sea based island
     */
    public static Builder seaBuilder() {
        return new Builder(IslandPlacement.SEA);
    }

    /**
     * Creates a builder for a sky based island
     */
    public static Builder skyBuilder() {
        return new Builder(IslandPlacement.SKY);
    }

    @SuppressWarnings("UnusedReturnValue")  // its a builder my dude
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private static final String[] SIZES = new String[]{"0x1x0", "2x2x4", "4x1x6", "8x1x11", "11x1x11"};

        private final IslandPlacement placement;
        private final DataPool.Builder<Identifier> templates = DataPool.builder();
        private final DataPool.Builder<RegistryEntry<ConfiguredFeature<?, ?>>> trees = DataPool.builder();
        private final DataPool.Builder<Block> grasses = DataPool.builder();
        @Nullable
        @Accessors(fluent = true)
        private Block vines;

        /**
         * Adds the given template to the builder
         */
        public Builder addTemplate(Identifier template, int weight) {
            this.templates.add(template, weight);
            return this;
        }

        /**
         * Adds the default 5 templates around the given prefix to the builder
         */
        public Builder addDefaultTemplates(Identifier prefix) {
            for (String size : SIZES) {
                addTemplate(LocationExtender.INSTANCE.suffix(prefix, size), 1);
            }
            return this;
        }

        /**
         * Adds a new tree to the builder with the given weight
         */
        public Builder addTree(RegistryEntry<? extends ConfiguredFeature<?, ?>> tree, int weight) {
            trees.add(RegistryEntry.hackyErase(tree), weight);
            return this;
        }

        /**
         * Adds a new tree to the builder with the given weight
         */
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        public Builder addTree(RegistryObject<? extends ConfiguredFeature<?, ?>> tree, int weight) {
            return addTree(tree.getHolder().get(), weight);
        }

        /**
         * Adds a new grass type to the builder with the given weight
         */
        public Builder vines(Block block) {
            this.vines = block;
            return this;
        }

        /**
         * Adds a new grass type to the builder with the given weight
         */
        public Builder vines(RegistryObject<? extends Block> block) {
            return vines(block.get());
        }

        /**
         * Adds a new grass type to the builder with the given weight
         */
        public Builder addGrass(Block block, int weight) {
            this.grasses.add(block, weight);
            return this;
        }

        /**
         * Adds a new grass type to the builder with the given weight
         */
        public Builder addGrass(RegistryObject<? extends Block> block, int weight) {
            return addGrass(block.get(), weight);
        }

        /**
         * Adds slimy grass of the given type to the builder
         */
        public Builder addSlimyGrass(FoliageType foliage) {
            addGrass(TinkerWorld.slimeTallGrass.get(foliage), 7);
            addGrass(TinkerWorld.slimeFern.get(foliage), 1);
            return this;
        }

        /**
         * Builds the final config
         */
        public IslandStructure build(Config settings) {
            return new IslandStructure(settings, placement, templates.build(), trees.build(), Optional.ofNullable(vines), grasses.build());
        }
    }
}
