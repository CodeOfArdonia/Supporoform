package slimeknights.tconstruct.world.block;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import slimeknights.tconstruct.common.TinkerTags;

public class SlimeLeavesBlock extends LeavesBlock {
    @Getter
    private final FoliageType foliageType;

    public SlimeLeavesBlock(Settings properties, FoliageType foliageType) {
        super(properties);
        this.foliageType = foliageType;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn, BlockPos currentPos, BlockPos facingPos) {
        int i = getDistance(facingState) + 1;
        if (i != 1 || stateIn.get(DISTANCE) != i) {
            worldIn.scheduleBlockTick(currentPos, this, 1);
        }

        return stateIn;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        worldIn.setBlockState(pos, updateDistanceFromLogs(state, worldIn, pos), 3);
    }

    private static BlockState updateDistanceFromLogs(BlockState state, WorldAccess world, BlockPos pos) {
        int i = 7;

        BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
        for (Direction direction : Direction.values()) {
            mutableBlockPos.set(pos).move(direction);
            i = Math.min(i, getDistance(world.getBlockState(mutableBlockPos)) + 1);
            if (i == 1) {
                break;
            }
        }

        return state.with(DISTANCE, i);
    }

    private static int getDistance(BlockState neighbor) {
        if (neighbor.isIn(TinkerTags.Blocks.SLIMY_LOGS)) {
            return 0;
        } else {
            return neighbor.getBlock() instanceof SlimeLeavesBlock ? neighbor.get(DISTANCE) : 7;
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return updateDistanceFromLogs(this.getDefaultState().with(PERSISTENT, Boolean.TRUE), context.getWorld(), context.getBlockPos());
    }

// TODO: needed?
//  @Override
//  public boolean canBeReplacedByLeaves(BlockState state, LevelReader world, BlockPos pos) {
//    return this.isAir(state, world, pos) || state.is(BlockTags.LEAVES) || state.is(TinkerTags.Blocks.SLIMY_LEAVES);
//  }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.foliageType != FoliageType.ICHOR) {
            super.fillItemCategory(group, items);
        }
    }
}
