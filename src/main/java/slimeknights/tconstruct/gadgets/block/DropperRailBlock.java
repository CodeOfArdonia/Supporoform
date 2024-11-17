package slimeknights.tconstruct.gadgets.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.RailBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.Hopper;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.mantle.inventory.EmptyItemHandler;

public class DropperRailBlock extends RailBlock {

    public DropperRailBlock(Settings properties) {
        super(properties);
    }

    @Override
    public void onMinecartPass(BlockState state, World world, BlockPos pos, AbstractMinecartEntity cart) {
        if (!cart.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).isPresent() || !(cart instanceof Hopper)) {
            return;
        }
        BlockEntity tileEntity = world.getBlockEntity(pos.down());
        if (tileEntity == null || !tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).isPresent()) {
            return;
        }

        // todo: fix this optional usage
        IItemHandler itemHandlerCart = cart.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(EmptyItemHandler.INSTANCE);
        IItemHandler itemHandlerTE = tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(EmptyItemHandler.INSTANCE);

        for (int i = 0; i < itemHandlerCart.getSlots(); i++) {
            ItemStack itemStack = itemHandlerCart.extractItem(i, 1, true);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (ItemHandlerHelper.insertItem(itemHandlerTE, itemStack, true).isEmpty()) {
                itemStack = itemHandlerCart.extractItem(i, 1, false);
                ItemHandlerHelper.insertItem(itemHandlerTE, itemStack, false);
                break;
            }
        }
    }

}
