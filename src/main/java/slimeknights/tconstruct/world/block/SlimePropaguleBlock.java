package slimeknights.tconstruct.world.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.Objects;

import static net.minecraft.block.PropaguleBlock.AGE;
import static net.minecraft.state.property.Properties.HANGING;
import static net.minecraft.state.property.Properties.WATERLOGGED;

/**
 * Recreation of {@link net.minecraft.block.PropaguleBlock} to swap out the tree grower.
 */
public class SlimePropaguleBlock extends SlimeSaplingBlock {
    public SlimePropaguleBlock(SaplingGenerator treeIn, FoliageType foliageType, Settings properties) {
        super(treeIn, foliageType, properties);
        this.setDefaultState(this.stateManager.getDefaultState().with(STAGE, 0).with(AGE, 0).with(WATERLOGGED, false).with(HANGING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(STAGE).add(AGE).add(WATERLOGGED).add(HANGING);
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext pContext) {
        FluidState fluidstate = pContext.getWorld().getFluidState(pContext.getBlockPos());
        boolean flag = fluidstate.getFluid() == Fluids.WATER;
        return Objects.requireNonNull(super.getPlacementState(pContext))
                .with(WATERLOGGED, flag)
                .with(AGE, 4);
    }


    @Override
    public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
        Vec3d vec3 = pState.getModelOffset(pLevel, pPos);
        VoxelShape voxelshape;
        if (pState.get(HANGING)) {
            voxelshape = SHAPE_PER_AGE[pState.get(AGE)];
        } else {
            voxelshape = SHAPE;
        }

        return voxelshape.offset(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView level, BlockPos pos) {
        return state.get(HANGING)
                ? level.getBlockState(pos.up()).isIn(TinkerTags.Blocks.SLIMY_LEAVES)
                : super.canPlaceAt(state, level, pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pState.get(WATERLOGGED)) {
            pLevel.scheduleFluidTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickRate(pLevel));
        }
        return pFacing == Direction.UP && !pState.canPlaceAt(pLevel, pCurrentPos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.get(WATERLOGGED)
                ? Fluids.WATER.getStill(false)
                : super.getFluidState(pState);
    }

    @Override
    public void randomTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
        if (!pState.get(HANGING)) {
            if (pRandom.nextInt(7) == 0) {
                this.generate(pLevel, pPos, pState, pRandom);
            }

        } else {
            if (pState.get(AGE) != 4) {
                pLevel.setBlockState(pPos, pState.cycle(AGE), 2);
            }

        }
    }

    @Override
    public boolean isValidBonemealTarget(BlockView pLevel, BlockPos pPos, BlockState state, boolean pIsClient) {
        return !state.get(HANGING) || state.get(AGE) != 4;
    }

    @Override
    public boolean canGrow(World pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
        return pState.get(HANGING)
                ? pState.get(AGE) != 4
                : super.canGrow(pLevel, pRandom, pPos, pState);
    }

    @Override
    public void grow(ServerWorld pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
        if (pState.get(HANGING) && pState.get(AGE) != 4) {
            pLevel.setBlockState(pPos, pState.cycle(AGE), 2);
        } else {
            super.grow(pLevel, pRandom, pPos, pState);
        }
    }
}
