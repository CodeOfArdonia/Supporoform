package slimeknights.tconstruct.tables.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

public class TinkersChestBlock extends ChestBlock {
    public TinkersChestBlock(Settings builder, BlockEntityFactory<? extends BlockEntity> be, boolean dropsItems) {
        super(builder, be, dropsItems);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        BlockEntityHelper.get(TinkersChestBlockEntity.class, world, pos).ifPresent(te -> {
            if (te.hasColor()) {
                ((DyeableItem) stack.getItem()).setColor(stack, te.getColor());
            }
        });
        return stack;
    }
}
