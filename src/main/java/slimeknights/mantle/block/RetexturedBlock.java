package slimeknights.mantle.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.util.RetexturedHelper;

import java.util.List;

/**
 * Logic for a retexturable block. Use alongside {@link IRetexturedBlockEntity} and {@link RetexturedHelper}
 */
@SuppressWarnings("WeakerAccess")
public abstract class RetexturedBlock extends Block implements BlockEntityProvider {
    public RetexturedBlock(Settings properties) {
        super(properties);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        updateTextureBlock(world, pos, stack);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return getPickBlock(world, pos, state);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView pLevel, List<Text> tooltip, TooltipContext pFlag) {
        RetexturedHelper.addTooltip(stack, tooltip);
    }


    /* Utils */

    /**
     * Call in {@link Block#onPlaced(World, BlockPos, BlockState, LivingEntity, ItemStack)} to set the texture tag to the Tile Entity
     *
     * @param world World where the block was placed
     * @param pos   Block position
     * @param stack Item stack
     */
    public static void updateTextureBlock(World world, BlockPos pos, ItemStack stack) {
        if (stack.hasNbt()) {
            BlockEntityHelper.get(IRetexturedBlockEntity.class, world, pos).ifPresent(te -> te.updateTexture(RetexturedHelper.getTextureName(stack)));
        }
    }

    /**
     * Called in blocks to get the item stack for the current block
     *
     * @param world World
     * @param pos   Pos
     * @param state State
     * @return Pickblock stack with proper NBT
     */
    public static ItemStack getPickBlock(BlockView world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        ItemStack stack = new ItemStack(block);
        BlockEntityHelper.get(IRetexturedBlockEntity.class, world, pos).ifPresent(te -> RetexturedHelper.setTexture(stack, te.getTextureName()));
        return stack;
    }
}
