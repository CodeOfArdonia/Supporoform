package slimeknights.tconstruct.shared.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.GlassBlock;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class SoulGlassBlock extends GlassBlock {
    public SoulGlassBlock(Settings properties) {
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
