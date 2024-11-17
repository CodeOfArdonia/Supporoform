package slimeknights.tconstruct.smeltery.block.controller;

import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MelterBlock extends TinyMultiblockControllerBlock {
    public MelterBlock(Settings props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new MelterBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World pLevel, BlockState pState, BlockEntityType<T> check) {
        return pLevel.isClient ? null : BlockEntityHelper.castTicker(check, TinkerSmeltery.melter.get(), MelterBlockEntity.SERVER_TICKER);
    }


    /*
     * Display
     */

    @Deprecated
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView worldIn, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView reader, BlockPos pos) {
        return true;
    }


    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random rand) {
        if (state.get(ACTIVE)) {
            double x = pos.getX() + 0.5D;
            double y = (double) pos.getY() + (rand.nextFloat() * 6F) / 16F;
            double z = pos.getZ() + 0.5D;
            double frontOffset = 0.52D;
            double sideOffset = rand.nextDouble() * 0.6D - 0.3D;
            spawnFireParticles(world, state, x, y, z, frontOffset, sideOffset);
        }
    }
}
