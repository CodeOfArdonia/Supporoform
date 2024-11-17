package slimeknights.tconstruct.world.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SkullBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class PiglinHeadBlock extends SkullBlock {
    protected static final VoxelShape PIGLIN_SHAPE = Block.createCuboidShape(3, 0, 3, 13, 8, 13);

    public PiglinHeadBlock(SkullType type, Settings properties) {
        super(type, properties);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return PIGLIN_SHAPE;
    }
}
