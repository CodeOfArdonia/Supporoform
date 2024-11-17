package slimeknights.tconstruct.shared.block;

import slimeknights.tconstruct.common.TinkerTags;

import static net.minecraft.state.property.Properties.DOWN;
import static net.minecraft.state.property.Properties.EAST;
import static net.minecraft.state.property.Properties.NORTH;
import static net.minecraft.state.property.Properties.SOUTH;
import static net.minecraft.state.property.Properties.UP;
import static net.minecraft.state.property.Properties.WEST;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class PlatformBlock extends Block implements Waterloggable {
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    private static final BooleanProperty[] DIRECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    /**
     * Makes an index in the shapes map for the given set of booleans
     */
    private static int makeShapeIndex(boolean up, boolean down, boolean north, boolean east, boolean south, boolean west) {
        return (down ? 0b000001 : 0)
                | (up ? 0b000010 : 0)
                | (north ? 0b000100 : 0)
                | (south ? 0b001000 : 0)
                | (west ? 0b010000 : 0)
                | (east ? 0b100000 : 0);
    }

    static {
        // base boxes
        VoxelShape neither = VoxelShapes.union(Block.createCuboidShape(0, 0, 0, 2, 16, 2), Block.createCuboidShape(14, 0, 0, 16, 16, 2),
                Block.createCuboidShape(0, 0, 14, 2, 16, 16), Block.createCuboidShape(14, 0, 14, 16, 16, 16));
        VoxelShape bottom = VoxelShapes.union(neither, Block.createCuboidShape(0, 0, 0, 16, 2, 16));
        VoxelShape topPlate = Block.createCuboidShape(0, 14, 0, 16, 16, 16);
        VoxelShape top = VoxelShapes.union(neither, topPlate);
        VoxelShape both = VoxelShapes.union(bottom, topPlate);

        // start building map for all orientations
        boolean[] bools = {false, true};
        VoxelShape northPlate = Block.createCuboidShape(0, 2, 0, 16, 14, 1);
        VoxelShape southPlate = Block.createCuboidShape(0, 2, 15, 16, 14, 16);
        VoxelShape westPlate = Block.createCuboidShape(0, 2, 0, 1, 14, 16);
        VoxelShape eastPlate = Block.createCuboidShape(15, 2, 0, 16, 14, 16);
        for (boolean north : bools) {
            for (boolean east : bools) {
                for (boolean south : bools) {
                    for (boolean west : bools) {
                        for (boolean up : bools) {
                            for (boolean down : bools) {
                                // base shape
                                VoxelShape shape;
                                if (up) {
                                    shape = down ? neither : bottom;
                                } else {
                                    shape = down ? top : both;
                                }
                                if (north) shape = VoxelShapes.combine(shape, northPlate, BooleanBiFunction.OR);
                                if (south) shape = VoxelShapes.combine(shape, southPlate, BooleanBiFunction.OR);
                                if (west) shape = VoxelShapes.combine(shape, westPlate, BooleanBiFunction.OR);
                                if (east) shape = VoxelShapes.combine(shape, eastPlate, BooleanBiFunction.OR);
                                // add to map
                                int index = makeShapeIndex(up, down, north, east, south, west);
                                SHAPES[index] = shape.simplify();
                            }
                        }
                    }
                }
            }
        }
    }

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public PlatformBlock(Settings props) {
        super(props);
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(DIRECTIONS);
        builder.add(WATERLOGGED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView level, BlockPos pos, ShapeContext context) {
        return SHAPES[makeShapeIndex(state.get(UP), state.get(DOWN), state.get(NORTH), state.get(EAST), state.get(SOUTH), state.get(WEST))];
    }

    @Override
    public VoxelShape getSidesShape(BlockState pState, BlockView pReader, BlockPos pPos) {
        return VoxelShapes.fullCube();
    }

    /**
     * Checks if the block has the given facing property
     */
    private static boolean facingConnected(Direction facing, BlockState state, DirectionProperty property) {
        return !state.contains(property) || state.get(property) == facing;
    }

    /**
     * Checks if the block should connect to the given side
     */
    private static boolean connected(Direction direction, BlockState state) {
        if (!state.isIn(TinkerTags.Blocks.PLATFORM_CONNECTIONS)) {
            return false;
        }
        // if the block has a side property, use that
        BooleanProperty sideProp = DIRECTIONS[direction.getOpposite().getId()];
        if (state.contains(sideProp)) {
            return state.get(sideProp);
        }
        // if there is a face property and it is not wall, not connected
        if (state.contains(Properties.WALL_MOUNT_LOCATION) && state.get(Properties.WALL_MOUNT_LOCATION) != WallMountLocation.WALL) {
            return false;
        }
        // try relevant facing properties, if any are present must be facing this
        return facingConnected(direction, state, Properties.HORIZONTAL_FACING)
                && facingConnected(direction, state, Properties.FACING)
                && facingConnected(direction, state, Properties.HOPPER_FACING);
    }

    /**
     * Returns true if this platform connects vertically to the block
     */
    protected boolean verticalConnect(BlockState state) {
        return state.isOf(this);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        World level = context.getWorld();
        BlockPos below = pos.down();
        BlockState belowState = level.getBlockState(below);
        BlockState state = this.getDefaultState()
                .with(WATERLOGGED, level.getFluidState(pos).getFluid() == Fluids.WATER)
                .with(UP, verticalConnect(level.getBlockState(pos.up())))
                .with(DOWN, verticalConnect(belowState) || belowState.isSideSolidFullSquare(level, below, Direction.UP));
        for (Direction direction : Type.HORIZONTAL) {
            state = state.with(DIRECTIONS[direction.getId()], connected(direction, level.getBlockState(pos.offset(direction))));
        }
        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess level, BlockPos selfPos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            level.scheduleFluidTick(selfPos, Fluids.WATER, Fluids.WATER.getTickRate(level));
        }

        if (direction == Direction.UP) {
            return state.with(UP, verticalConnect(neighbor));
        } else if (direction == Direction.DOWN) {
            return state.with(DOWN, verticalConnect(neighbor) || neighbor.isSideSolidFullSquare(level, neighborPos, Direction.UP));
        }
        return state.with(DIRECTIONS[direction.getId()], connected(direction, neighbor));
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(pState);
    }
}
