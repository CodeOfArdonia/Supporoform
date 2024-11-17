package slimeknights.tconstruct.world.worldgen.trees.feature;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.ModifiableTestableWorld;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.level.material.Material;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeTreeConfig;

import java.util.List;
import java.util.Set;

public class SlimeTreeFeature extends Feature<SlimeTreeConfig> {

    public SlimeTreeFeature(Codec<SlimeTreeConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<SlimeTreeConfig> context) {
        Set<BlockPos> trunkPos = Sets.newHashSet();
        Set<BlockPos> foliagePos = Sets.newHashSet();
        Set<BlockPos> leavesPos = Sets.newHashSet();

        BlockBox boundingBox = BlockBox.infinite();
        StructureWorldAccess level = context.getWorld();

        boolean placed = this.place(level, context.getRandom(), context.getOrigin(), trunkPos, foliagePos, boundingBox, context.getConfig());
        if (boundingBox.getMinX() <= boundingBox.getMaxX() && placed && !trunkPos.isEmpty()) {
            VoxelSet voxelshapepart = this.updateLeaves(level, boundingBox, trunkPos, leavesPos);
            StructureTemplate.updateCorner(level, 3, voxelshapepart, boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
            return true;
        }
        return false;
    }

    private boolean place(StructureWorldAccess level, Random rand, BlockPos positionIn, Set<BlockPos> trunkBlockPosSet, Set<BlockPos> foliagePositions, BlockBox boundingBoxIn, SlimeTreeConfig configIn) {
        // determine tree height
        int height = rand.nextInt(configIn.randomHeight) + configIn.baseHeight;
        if (configIn.canDoubleHeight && rand.nextInt(10) == 0) {
            height *= 2;
        }

//    BlockPos blockpos;
//    if (!configIn.forcePlacement) {
//      int oceanFloorHeight = generationReader.getHeight(Heightmap.Type.OCEAN_FLOOR, positionIn).getY();
//
//      blockpos = new BlockPos(positionIn.getX(), oceanFloorHeight, positionIn.getZ());
//    }
//    else {
//      blockpos = positionIn;
//    }

        if (positionIn.getY() >= level.getBottomY() + 1 && positionIn.getY() + height + 1 <= level.getTopY() && isSlimySoilAt(level, positionIn.down())) {
            this.setDirtAt(level, positionIn.down(), positionIn);
            this.placeTrunk(level, rand, height, positionIn, trunkBlockPosSet, boundingBoxIn, configIn);
            this.placeCanopy(level, rand, height, positionIn, trunkBlockPosSet, boundingBoxIn, configIn);
            return true;
        }
        return false;
    }

    protected void setDirtAt(StructureWorldAccess reader, BlockPos pos, BlockPos origin) {
        BlockState state = reader.getBlockState(pos);
        if (state.isIn(BlockTags.DIRT)) {
            reader.setBlockState(pos, Blocks.DIRT.getDefaultState(), 2);
        }
    }

    protected void placeTrunk(ModifiableTestableWorld worldIn, Random randomIn, int treeHeight, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn, SlimeTreeConfig treeFeatureConfigIn) {
        while (treeHeight > 0) {
            this.setLog(worldIn, randomIn, blockPos, blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);

            blockPos = blockPos.up();
            treeHeight--;
        }
    }

    protected void placeCanopy(ModifiableTestableWorld worldIn, Random randomIn, int treeHeight, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn, SlimeTreeConfig treeFeatureConfigIn) {
        blockPos = blockPos.up(treeHeight);
        for (int i = 0; i < 4; i++) {
            this.placeDiamondLayer(worldIn, randomIn, i + 1, blockPos.down(i), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
        }

        blockPos = blockPos.down(3);

        this.placeAir(worldIn, randomIn, blockPos.add(+4, 0, 0), blockPosSet, mutableBoundingBoxIn);
        this.placeAir(worldIn, randomIn, blockPos.add(-4, 0, 0), blockPosSet, mutableBoundingBoxIn);
        this.placeAir(worldIn, randomIn, blockPos.add(0, 0, +4), blockPosSet, mutableBoundingBoxIn);
        this.placeAir(worldIn, randomIn, blockPos.add(0, 0, -4), blockPosSet, mutableBoundingBoxIn);

        if (treeFeatureConfigIn.hasVines) {
            this.placeAir(worldIn, randomIn, blockPos.add(+1, 0, +1), blockPosSet, mutableBoundingBoxIn);
            this.placeAir(worldIn, randomIn, blockPos.add(+1, 0, -1), blockPosSet, mutableBoundingBoxIn);
            this.placeAir(worldIn, randomIn, blockPos.add(-1, 0, +1), blockPosSet, mutableBoundingBoxIn);
            this.placeAir(worldIn, randomIn, blockPos.add(-1, 0, -1), blockPosSet, mutableBoundingBoxIn);
        }

        //Drippers
        // stuck with only one block down because of leaf decay distance
        blockPos = blockPos.down();
        this.setLeaf(worldIn, randomIn, blockPos.add(+3, 0, 0), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
        this.setLeaf(worldIn, randomIn, blockPos.add(-3, 0, 0), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
        this.setLeaf(worldIn, randomIn, blockPos.add(0, 0, -3), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
        this.setLeaf(worldIn, randomIn, blockPos.add(0, 0, +3), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);

        if (!treeFeatureConfigIn.hasVines) {
            this.setLeaf(worldIn, randomIn, blockPos.add(+1, 0, +1), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
            this.setLeaf(worldIn, randomIn, blockPos.add(-3, 0, 0), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
            this.setLeaf(worldIn, randomIn, blockPos.add(-1, 0, +1), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
            this.setLeaf(worldIn, randomIn, blockPos.add(-1, 0, -1), blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
        }

        if (treeFeatureConfigIn.hasVines) {
            blockPos = blockPos.down();
            this.placeVine(worldIn, randomIn, blockPos.add(+3, 0, 0), blockPosSet, mutableBoundingBoxIn,
                    this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn).with(VineBlock.UP, true));

            this.placeVine(worldIn, randomIn, blockPos.add(-3, 0, 0), blockPosSet, mutableBoundingBoxIn,
                    this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn).with(VineBlock.UP, true));

            this.placeVine(worldIn, randomIn, blockPos.add(0, 0, -3), blockPosSet, mutableBoundingBoxIn,
                    this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn).with(VineBlock.UP, true));

            this.placeVine(worldIn, randomIn, blockPos.add(0, 0, +3), blockPosSet, mutableBoundingBoxIn,
                    this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn).with(VineBlock.UP, true));

            BlockState randomVine = this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn);
            this.placeVine(worldIn, randomIn, blockPos.add(+2, 1, +2), blockPosSet, mutableBoundingBoxIn,
                    randomVine.with(VineBlock.UP, true));
            this.placeVine(worldIn, randomIn, blockPos.add(+2, 0, +2), blockPosSet, mutableBoundingBoxIn,
                    randomVine);

            randomVine = this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn);
            this.placeVine(worldIn, randomIn, blockPos.add(+2, 1, -2), blockPosSet, mutableBoundingBoxIn,
                    randomVine.with(VineBlock.UP, true));
            this.placeVine(worldIn, randomIn, blockPos.add(+2, 0, -2), blockPosSet, mutableBoundingBoxIn,
                    randomVine);

            randomVine = this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn);
            this.placeVine(worldIn, randomIn, blockPos.add(-2, 1, +2), blockPosSet, mutableBoundingBoxIn,
                    randomVine.with(VineBlock.UP, true));
            this.placeVine(worldIn, randomIn, blockPos.add(-2, 0, +2), blockPosSet, mutableBoundingBoxIn,
                    randomVine);

            randomVine = this.getRandomizedVine(randomIn, blockPos, treeFeatureConfigIn);
            this.placeVine(worldIn, randomIn, blockPos.add(-2, 1, -2), blockPosSet, mutableBoundingBoxIn,
                    randomVine.with(VineBlock.UP, true));
            this.placeVine(worldIn, randomIn, blockPos.add(-2, 0, -2), blockPosSet, mutableBoundingBoxIn,
                    randomVine);
        }
    }

    private void placeDiamondLayer(ModifiableTestableWorld worldIn, Random randomIn, int range, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn, SlimeTreeConfig treeFeatureConfigIn) {
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                if (Math.abs(x) + Math.abs(z) <= range) {
                    BlockPos blockpos = blockPos.add(x, 0, z);
                    this.setLeaf(worldIn, randomIn, blockpos, blockPosSet, mutableBoundingBoxIn, treeFeatureConfigIn);
                }
            }
        }
    }

    protected boolean setLog(ModifiableTestableWorld worldIn, Random randomIn, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn, SlimeTreeConfig treeFeatureConfigIn) {
        if (!isAirOrLeavesAt(worldIn, blockPos)) {
            return false;
        } else {
            this.setBlockState(worldIn, blockPos, treeFeatureConfigIn.trunkProvider.get(randomIn, blockPos));
            //TODO mutableBoundingBoxIn.expand(new BoundingBox(blockPos, blockPos));
            blockPosSet.add(blockPos.toImmutable());
            return true;
        }
    }

    protected boolean placeAir(ModifiableTestableWorld worldIn, Random random, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn) {
        if (!isAirOrLeavesAt(worldIn, blockPos)) {
            return false;
        } else {
            this.setBlockState(worldIn, blockPos, Blocks.AIR.getDefaultState());
            //TODO mutableBoundingBoxIn.expand(new BoundingBox(blockPos, blockPos));
            blockPosSet.add(blockPos.toImmutable());
            return true;
        }
    }

    protected boolean setLeaf(ModifiableTestableWorld worldIn, Random random, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn, SlimeTreeConfig treeFeatureConfigIn) {
        if (!isAirOrLeavesAt(worldIn, blockPos)) {
            return false;
        } else {
            this.setBlockState(worldIn, blockPos, treeFeatureConfigIn.leavesProvider.get(random, blockPos));
            //TODO mutableBoundingBoxIn.expand(new BoundingBox(blockPos, blockPos));
            blockPosSet.add(blockPos.toImmutable());
            return true;
        }
    }

    protected boolean placeVine(ModifiableTestableWorld worldIn, Random random, BlockPos blockPos, Set<BlockPos> blockPosSet, BlockBox mutableBoundingBoxIn, BlockState vineState) {
        if (!isAirOrLeavesAt(worldIn, blockPos)) {
            return false;
        } else {
            this.setBlockState(worldIn, blockPos, vineState);
            //TODO mutableBoundingBoxIn.expand(new BoundingBox(blockPos, blockPos));
            blockPosSet.add(blockPos.toImmutable());
            return true;
        }
    }

    private BlockState getRandomizedVine(Random random, BlockPos blockPos, SlimeTreeConfig config) {
        BlockState state = config.vinesProvider.get(random, blockPos);

        BooleanProperty[] sides = new BooleanProperty[]{VineBlock.NORTH, VineBlock.EAST, VineBlock.SOUTH, VineBlock.WEST};

        for (BooleanProperty side : sides) {
            state = state.with(side, false);
        }

        for (int i = random.nextInt(3) + 1; i > 0; i--) {
            state = state.with(sides[random.nextInt(sides.length)], true);
        }

        return state;
    }

    public static boolean isEmptyOrLogAt(TestableWorld reader, BlockPos blockPos) {
        return isReplaceableAt(reader, blockPos) || reader.testBlockState(blockPos, state -> state.isIn(BlockTags.LOGS));
    }

    private static boolean isVineAt(TestableWorld reader, BlockPos blockPos) {
        return reader.testBlockState(blockPos, (state) -> state.isOf(Blocks.VINE));
    }

    private static boolean isWaterAt(TestableWorld reader, BlockPos blockPos) {
        return reader.testBlockState(blockPos, state -> state.isOf(Blocks.WATER));
    }

    public static boolean isAirOrLeavesAt(TestableWorld reader, BlockPos blockPos) {
        return reader.testBlockState(blockPos, state -> state.isAir() || state.isIn(BlockTags.LEAVES));
    }

    private static boolean isSlimySoilAt(TestableWorld reader, BlockPos blockPos) {
        return reader.testBlockState(blockPos, state -> state.isIn(TinkerTags.Blocks.SLIMY_SOIL));
    }

    private static boolean isTallPlantAt(TestableWorld reader, BlockPos blockPos) {
        return reader.testBlockState(blockPos, state -> state.getMaterial() == Material.REPLACEABLE_PLANT);
    }

    public static boolean isReplaceableAt(TestableWorld reader, BlockPos blockPos) {
        return isAirOrLeavesAt(reader, blockPos) || isTallPlantAt(reader, blockPos) || isWaterAt(reader, blockPos);
    }

    public static void setBlockStateAt(ModifiableWorld writer, BlockPos blockPos, BlockState state) {
        writer.setBlockState(blockPos, state, 19);
    }

    private VoxelSet updateLeaves(WorldAccess world, BlockBox boundingBox, Set<BlockPos> logs, Set<BlockPos> leaves) {
        List<Set<BlockPos>> distanceList = Lists.newArrayList();
        VoxelSet shapePart = new BitSetVoxelSet(boundingBox.getBlockCountX(), boundingBox.getBlockCountY(), boundingBox.getBlockCountZ());
        for (int j = 0; j < 6; ++j) {
            distanceList.add(Sets.newHashSet());
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        //for (BlockPos leavePos : Lists.newArrayList(leaves)) {
        //if (boundingBox.isInside(leavePos)) {
        //TODO shapePart.setFull(leavePos.getX() - boundingBox.minX(), leavePos.getY() - boundingBox.minY(), leavePos.getZ() - boundingBox.minZ(), true, true);
        //}
        //}

        for (BlockPos logPos : Lists.newArrayList(logs)) {
            //if (boundingBox.isInside(logPos)) {
            //TODO shapePart.setFull(logPos.getX() - boundingBox.minX(), logPos.getY() - boundingBox.minY(), logPos.getZ() - boundingBox.minZ(), true, true);
            //}
            for (Direction direction : Direction.values()) {
                mutable.set(logPos, direction);
                if (!logs.contains(mutable)) {
                    BlockState blockstate = world.getBlockState(mutable);
                    if (blockstate.contains(Properties.DISTANCE_1_7)) {
                        distanceList.get(0).add(mutable.toImmutable());
                        setBlockStateAt(world, mutable, blockstate.with(Properties.DISTANCE_1_7, 1));
                        //if (boundingBox.isInside(mutable)) {
                        //TODO shapePart.setFull(mutable.getX() - boundingBox.minX(), mutable.getY() - boundingBox.minY(), mutable.getZ() - boundingBox.minZ(), true, true);
                        //}
                    }
                }
            }
        }

        for (int distance = 1; distance < 6; ++distance) {
            Set<BlockPos> current = distanceList.get(distance - 1);
            Set<BlockPos> next = distanceList.get(distance);

            for (BlockPos pos : current) {
                //if (boundingBox.isInside(pos)) {
                //TODO shapePart.setFull(pos.getX() - boundingBox.minX(), pos.getY() - boundingBox.minY(), pos.getZ() - boundingBox.minZ(), true, true);
                //}

                for (Direction direction : Direction.values()) {
                    mutable.set(pos, direction);
                    if (!current.contains(mutable) && !next.contains(mutable)) {
                        BlockState state = world.getBlockState(mutable);
                        if (state.contains(Properties.DISTANCE_1_7)) {
                            int stateDistance = state.get(Properties.DISTANCE_1_7);
                            if (stateDistance > distance + 1) {
                                BlockState furtherState = state.with(Properties.DISTANCE_1_7, distance + 1);
                                setBlockStateAt(world, mutable, furtherState);
                                //if (boundingBox.isInside(mutable)) {
                                //TODO shapePart.setFull(mutable.getX() - boundingBox.minX(), mutable.getY() - boundingBox.minY(), mutable.getZ() - boundingBox.minZ(), true, true);
                                //}
                                next.add(mutable.toImmutable());
                            }
                        }
                    }
                }
            }
        }

        return shapePart;
    }
}
