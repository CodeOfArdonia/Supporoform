package slimeknights.tconstruct.tables.item;

import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Dyeable chest block
 */
public class TinkersChestBlockItem extends BlockItem implements DyeableItem {
    public TinkersChestBlockItem(Block blockIn, Settings builder) {
        super(blockIn, builder);
    }

    @Override
    public int getColor(ItemStack stack) {
        NbtCompound tag = stack.getSubNbt("display");
        return tag != null && tag.contains("color", NbtElement.NUMBER_TYPE) ? tag.getInt("color") : TinkersChestBlockEntity.DEFAULT_COLOR;
    }

    @Override
    protected boolean postPlacement(BlockPos pos, World worldIn, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        boolean result = super.postPlacement(pos, worldIn, player, stack, state);
        if (this.hasColor(stack)) {
            int color = this.getColor(stack);
            BlockEntityHelper.get(TinkersChestBlockEntity.class, worldIn, pos).ifPresent(te -> te.setColor(color));
        }
        return result;
    }
}
