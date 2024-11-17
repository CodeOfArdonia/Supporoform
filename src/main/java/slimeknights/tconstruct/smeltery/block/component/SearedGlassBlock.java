package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public class SearedGlassBlock extends SearedBlock {

    public SearedGlassBlock(Settings properties) {
        super(properties, false);
    }

    @Deprecated
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView worldIn, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView reader, BlockPos pos) {
        return true;
    }

    @Deprecated
    @Override
    public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getBlock() == this || super.isSideInvisible(state, adjacentBlockState, side);
    }
}
