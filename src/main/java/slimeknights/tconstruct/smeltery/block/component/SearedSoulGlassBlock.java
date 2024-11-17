package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class SearedSoulGlassBlock extends SearedGlassBlock {
    public SearedSoulGlassBlock(Settings properties) {
        super(properties);
    }

    @Override
    public VoxelShape getSidesShape(BlockState pState, BlockView pReader, BlockPos pPos) {
        return VoxelShapes.fullCube();
    }

    @Override
    public boolean canPathfindThrough(BlockState pState, BlockView pLevel, BlockPos pPos, NavigationType pType) {
        return false;
    }
}
