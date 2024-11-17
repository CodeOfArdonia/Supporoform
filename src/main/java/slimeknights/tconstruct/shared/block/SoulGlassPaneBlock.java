package slimeknights.tconstruct.shared.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class SoulGlassPaneBlock extends ClearGlassPaneBlock {
    public SoulGlassPaneBlock(Settings properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getSidesShape(BlockState pState, BlockView pReader, BlockPos pPos) {
        return super.getCollisionShape(pState, pReader, pPos, ShapeContext.absent());
    }
}
