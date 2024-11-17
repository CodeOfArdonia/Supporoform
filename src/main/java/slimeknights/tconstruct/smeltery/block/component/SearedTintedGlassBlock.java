package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class SearedTintedGlassBlock extends SearedGlassBlock {
    public SearedTintedGlassBlock(Settings properties) {
        super(properties);
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return world.getMaxLightLevel();
    }
}
