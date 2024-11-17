package slimeknights.tconstruct.smeltery.block.controller;

import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SmelteryControllerBlock extends HeatingControllerBlock {
    public SmelteryControllerBlock(Settings properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new SmelteryBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World pLevel, BlockState pState, BlockEntityType<T> type) {
        return HeatingStructureBlockEntity.getTicker(pLevel, type, TinkerSmeltery.smeltery.get());
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(worldIn, pos, state, placer, stack);
        // check structure
        BlockEntityHelper.get(SmelteryBlockEntity.class, worldIn, pos).ifPresent(SmelteryBlockEntity::updateStructure);
    }

    @Override
    @Deprecated
    public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!newState.isOf(this)) {
            BlockEntityHelper.get(SmelteryBlockEntity.class, worldIn, pos).ifPresent(SmelteryBlockEntity::invalidateStructure);
        }
        super.onStateReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random rand) {
        if (state.get(ACTIVE)) {
            double x = pos.getX() + 0.5D;
            double y = (double) pos.getY() + (rand.nextFloat() * 6F + 2F) / 16F;
            double z = pos.getZ() + 0.5D;
            double frontOffset = 0.52D;
            double sideOffset = rand.nextDouble() * 0.6D - 0.3D;
            spawnFireParticles(world, state, x, y, z, frontOffset, sideOffset);
        }
    }


    /* No rotation if in a structure  */

    @Deprecated
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        if (state.get(IN_STRUCTURE)) {
            return state;
        }
        return super.rotate(state, rotation);
    }

    @Deprecated
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        if (state.get(IN_STRUCTURE)) {
            return state;
        }
        return super.mirror(state, mirror);
    }
}
