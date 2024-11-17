package slimeknights.tconstruct.smeltery.block.component;

import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

public class SearedStairsBlock extends StairsBlock implements BlockEntityProvider {

    public SearedStairsBlock(Supplier<BlockState> state, Settings properties) {
        super(state, properties);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new SmelteryComponentBlockEntity(pPos, pState);
    }

    @Override
    @Deprecated
    public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!newState.isOf(this)) {
            BlockEntityHelper.get(SmelteryComponentBlockEntity.class, worldIn, pos).ifPresent(te -> te.notifyMasterOfChange(pos, newState));
        }
        super.onStateReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        SmelteryComponentBlockEntity.updateNeighbors(world, pos, state);
    }

    @Override
    @Deprecated
    public boolean onSyncedBlockEvent(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.onSyncedBlockEvent(state, worldIn, pos, id, param);

        BlockEntity tileentity = worldIn.getBlockEntity(pos);

        return tileentity != null && tileentity.onSyncedBlockEvent(id, param);
    }
}
