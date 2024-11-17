package slimeknights.mantle.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import slimeknights.mantle.util.BlockEntityHelper;

/**
 * Book item that can be placed on lecterns
 */
public abstract class LecternBookItem extends TooltipItem implements ILecternBookItem {
    public LecternBookItem(Settings properties) {
        super(properties);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World level = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isOf(Blocks.LECTERN)) {
            if (LecternBlock.putBookIfAbsent(context.getPlayer(), level, pos, state, context.getStack())) {
                return ActionResult.success(level.isClient);
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Event handler to control the lectern GUI
     */
    public static void interactWithBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getLevel();
        // client side has no access to the book, so just skip
        if (world.isClient() || event.getEntity().isShiftKeyDown()) {
            return;
        }
        // must be a lectern, and have the TE
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.LECTERN)) {
            BlockEntityHelper.get(LecternBlockEntity.class, world, pos)
                    .ifPresent(te -> {
                        ItemStack book = te.getBook();
                        if (!book.isEmpty() && book.getItem() instanceof ILecternBookItem
                                && ((ILecternBookItem) book.getItem()).openLecternScreen(world, pos, event.getEntity(), book)) {
                            event.setCanceled(true);
                        }
                    });
        }
    }

}
