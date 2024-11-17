package slimeknights.tconstruct.tables.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.RetexturedBlock;

public abstract class RetexturedTableBlock extends TabbedTableBlock {
    public RetexturedTableBlock(Settings builder) {
        super(builder);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        RetexturedBlock.updateTextureBlock(world, pos, stack);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return RetexturedBlock.getPickBlock(world, pos, state);
    }
}
