package slimeknights.mantle.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.entity.INameableMenuProvider;
import slimeknights.mantle.inventory.BaseContainerMenu;

/**
 * Base class for blocks with an inventory
 */
@SuppressWarnings("WeakerAccess")
public abstract class InventoryBlock extends Block implements BlockEntityProvider {

    protected InventoryBlock(Settings builder) {
        super(builder);
    }

    /**
     * Called when the block is activated to open the UI. Override to return false for blocks with no inventory
     *
     * @param player Player instance
     * @param world  World instance
     * @param pos    Block position
     * @return true if the GUI opened, false if not
     */
    protected boolean openGui(PlayerEntity player, World world, BlockPos pos) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory container = this.createScreenHandlerFactory(world.getBlockState(pos), world, pos);
            if (container != null && player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.openHandledScreen(container);
                if (player.currentScreenHandler instanceof BaseContainerMenu<?> menu) {
                    menu.syncOnOpen(serverPlayer);
                }
            }
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult rayTraceResult) {
        if (player.bypassesLandingEffects()) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            return this.openGui(player, world, pos) ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }


    /* Naming */

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(worldIn, pos, state, placer, stack);

        // set custom name from named stack
        if (stack.hasCustomName()) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof INameableMenuProvider provider) {
                provider.setCustomName(stack.getName());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    @Nullable
    @Deprecated
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World worldIn, BlockPos pos) {
        BlockEntity be = worldIn.getBlockEntity(pos);
        return be instanceof NamedScreenHandlerFactory ? (NamedScreenHandlerFactory) be : null;
    }


    /* Inventory handling */

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity te = worldIn.getBlockEntity(pos);
            if (te != null) {
                te.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(inventory -> this.dropInventoryItems(state, worldIn, pos, inventory));
                worldIn.updateComparators(pos, this);
            }
        }

        super.onStateReplaced(state, worldIn, pos, newState, isMoving);
    }

    /**
     * Called when the block is replaced to drop contained items.
     *
     * @param state     Block state
     * @param worldIn   Tile world
     * @param pos       Tile position
     * @param inventory Item handler
     */
    protected void dropInventoryItems(BlockState state, World worldIn, BlockPos pos, IItemHandler inventory) {
        dropInventoryItems(worldIn, pos, inventory);
    }

    /**
     * Drops all items from the given inventory in world
     *
     * @param world     World instance
     * @param pos       Position to drop
     * @param inventory Inventory instance
     */
    public static void dropInventoryItems(World world, BlockPos pos, IItemHandler inventory) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        for (int i = 0; i < inventory.getSlots(); ++i) {
            ItemScatterer.spawn(world, x, y, z, inventory.getStackInSlot(i));
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public boolean onSyncedBlockEvent(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.onSyncedBlockEvent(state, worldIn, pos, id, param);
        BlockEntity be = worldIn.getBlockEntity(pos);
        return be != null && be.onSyncedBlockEvent(id, param);
    }
}
