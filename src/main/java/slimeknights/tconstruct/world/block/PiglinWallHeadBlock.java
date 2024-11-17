package slimeknights.tconstruct.world.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SkullBlock.SkullType;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.Map;

public class PiglinWallHeadBlock extends WallSkullBlock {
    private static final Map<Direction, VoxelShape> PIGLIN_SHAPES = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, VoxelShapes.union(Block.createCuboidShape(3, 4, 8, 13, 12, 16), Block.createCuboidShape(5, 4, 7, 11, 6, 8), Block.createCuboidShape(6, 6, 7, 10, 8, 8)),
            Direction.SOUTH, VoxelShapes.union(Block.createCuboidShape(3, 4, 0, 13, 12, 8), Block.createCuboidShape(5, 4, 8, 11, 6, 9), Block.createCuboidShape(6, 6, 8, 10, 8, 9)),
            Direction.EAST, VoxelShapes.union(Block.createCuboidShape(0, 4, 3, 8, 12, 13), Block.createCuboidShape(8, 4, 5, 9, 6, 11), Block.createCuboidShape(8, 6, 6, 9, 8, 10)),
            Direction.WEST, VoxelShapes.union(Block.createCuboidShape(8, 4, 3, 16, 12, 13), Block.createCuboidShape(7, 4, 5, 8, 6, 11), Block.createCuboidShape(7, 6, 6, 8, 8, 10))));

    public PiglinWallHeadBlock(SkullType type, Settings properties) {
        super(type, properties);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return PIGLIN_SHAPES.get(state.get(FACING));
    }
}
