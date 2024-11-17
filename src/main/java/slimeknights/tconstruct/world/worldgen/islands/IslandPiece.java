package slimeknights.tconstruct.world.worldgen.islands;

import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.block.SlimeVineBlock;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import java.util.Optional;

public class IslandPiece extends SimpleStructurePiece {
    @Nullable
    private final ConfiguredFeature<?, ?> tree;
    private final IslandStructure structure;
    private int numberOfTreesPlaced;
    private ChunkGenerator chunkGenerator;

    public IslandPiece(StructureTemplateManager manager, IslandStructure structure, Identifier templateName, BlockPos templatePos, @Nullable ConfiguredFeature<?, ?> tree, BlockRotation rotation, BlockMirror mirror) {
        super(TinkerStructures.islandPiece.get(), 0, manager, templateName, templateName.toString(), makeSettings(rotation, mirror), templatePos);
        this.structure = structure;
        this.numberOfTreesPlaced = 0;
        this.tree = tree;
    }

    public IslandPiece(StructureContext context, NbtCompound nbt) {
        super(TinkerStructures.islandPiece.get(), nbt, context.structureTemplateManager(), id -> makeSettings(Rotation.valueOf(nbt.getString("Rot")), Mirror.valueOf(nbt.getString("Mi"))));
        DynamicRegistryManager access = context.registryManager();
        if (find(access.get(Registry.STRUCTURE_REGISTRY), nbt.getString("Structure")) instanceof IslandStructure island) {
            this.structure = island;
        } else {
            this.structure = null;
        }
        this.tree = find(access.get(Registry.CONFIGURED_FEATURE_REGISTRY), nbt.getString("Tree"));
        this.numberOfTreesPlaced = nbt.getInt("NumberOfTreesPlaced");
    }

    private static StructurePlacementData makeSettings(BlockRotation rotation, BlockMirror mirror) {
        return new StructurePlacementData().setIgnoreEntities(true).addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS).setRotation(rotation).setMirror(mirror);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound tag) {
        super.writeNbt(context, tag);
        DynamicRegistryManager access = context.registryManager();
        Identifier structure = access.get(Registry.STRUCTURE_REGISTRY).getKey(this.structure);
        if (structure != null) {
            tag.putString("Structure", structure.toString());
        }
        tag.putString("Rot", this.placementData.getRotation().name());
        tag.putString("Mi", this.placementData.getMirror().name());
        tag.putInt("NumberOfTreesPlaced", this.numberOfTreesPlaced);
        if (this.tree != null) {
            Identifier key = access.get(Registry.CONFIGURED_FEATURE_REGISTRY).getKey(this.tree);
            if (key != null) {
                tag.putString("Tree", key.toString());
            }
        }
    }

    @Override
    protected void handleMetadata(String function, BlockPos pos, ServerWorldAccess level, Random rand, BlockBox sbb) {
        switch (function) {
            case "tconstruct:slime_vine" -> {
                Block vines = this.structure.getVines();
                if (vines != null && rand.nextBoolean()) {
                    placeVine(level, pos, rand, vines.getDefaultState());
                }
            }
            case "tconstruct:slime_tree" -> {
                if (this.tree != null && this.numberOfTreesPlaced < 3 && rand.nextBoolean() && level instanceof StructureWorldAccess worldgenLevel) {
                    if (this.tree.generate(worldgenLevel, this.chunkGenerator, rand, pos)) {
                        this.numberOfTreesPlaced++;
                    }
                }
            }
            case "tconstruct:slime_tall_grass" -> {
                if (rand.nextBoolean()) {
                    Optional<Block> plant = this.structure.getGrasses().getRandomValue(rand);
                    if (plant.isPresent()) {
                        Block block = plant.get();
                        BlockState state = block.getDefaultState();
                        if (block instanceof PlantBlock bush && bush.canPlaceAt(state, level, pos)) {
                            level.setBlockState(pos, state, 2);
                        }
                    }
                }
            }
        }
    }

    private static void placeVine(WorldAccess worldIn, BlockPos pos, Random random, BlockState vineToPlace) {
        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && SlimeVineBlock.shouldConnectTo(worldIn, pos.offset(direction), direction)) {
                worldIn.setBlockState(pos, vineToPlace.with(SlimeVineBlock.getFacingProperty(direction), Boolean.TRUE), 2);
            }
        }

        // grow the vine a few times to start
        BlockPos vinePos = pos;
        for (int size = random.nextInt(8); size >= 0; size--) {
            BlockState state = worldIn.getBlockState(vinePos);
            if (state.getBlock() instanceof SlimeVineBlock vine) {
                vine.grow(worldIn, random, vinePos, state);
                vinePos = vinePos.down();
            } else {
                break;
            }
        }
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor manager, ChunkGenerator generator, Random rand, BlockBox bounds, ChunkPos chunk, BlockPos pos) {
        this.chunkGenerator = generator;

        // TODO: previously sea islands canceled if not enough water, but that leads to undesirable behavior as the structure "exists" but has no pieces
        // find if there is another place that can handle the check or ditch it
//    if (true || this.structure.getPlacement().isPositionValid(world, this.templatePosition, generator)) {
        super.generate(world, manager, generator, rand, bounds, chunk, pos);
//    }
    }


    /* Registry helpers, perhaps put somewhere better? */

    /**
     * Gets a registry, or falls back to builtin
     */
    private static <T> Registry<T> getRegistry(RegistryKey<? extends Registry<T>> registryKey, Registry<T> builtIn, StructureContext context) {
        Optional<? extends Registry<T>> registry = context.registryManager().getOptional(registryKey);
        if (registry.isPresent()) {
            return registry.get();
        } else {
            return builtIn;
        }
    }

    /**
     * Finds a registry object
     */
    @Nullable
    private static <T> T find(Registry<T> registry, String key) {
        Identifier id = Identifier.tryParse(key);
        if (id != null) {
            return registry.get(id);
        }
        return null;
    }
}
