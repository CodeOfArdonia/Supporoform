package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.block.entity.component.DuctBlockEntity;

/**
 * Item handler for the duct
 */
public class DuctItemHandler extends SingleItemHandler<DuctBlockEntity> {

    public DuctItemHandler(DuctBlockEntity parent) {
        super(parent, 1);
    }

    /**
     * Sets the stack in this duct
     *
     * @param newStack New stack
     */
    @Override
    public void setStack(ItemStack newStack) {
        World world = parent.getWorld();
        boolean hasChange = world != null && !ItemStack.areEqual(getStack(), newStack);
        super.setStack(newStack);
        if (hasChange) {
            if (!world.isClient) {
                BlockPos pos = parent.getPos();
                TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
            } else {
                parent.updateFluid();
            }
        }
    }

    @Override
    protected boolean isItemValid(ItemStack stack) {
        // the item or its container must be in the tag
        if (!stack.isIn(TinkerTags.Items.DUCT_CONTAINERS)) {
            ItemStack container = stack.getRecipeRemainder();
            if (container.isEmpty() || !container.isIn(TinkerTags.Items.DUCT_CONTAINERS)) {
                return false;
            }
        }
        // the item must contain fluid (no empty cans or buckets)
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .filter(cap -> !cap.getFluidInTank(0).isEmpty())
                .isPresent();
    }

    /**
     * Gets the fluid filter for this duct
     *
     * @return Fluid filter
     */
    public FluidStack getFluid() {
        ItemStack stack = getStack();
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return FluidUtil.getFluidHandler(stack)
                .map(handler -> handler.getFluidInTank(0))
                .orElse(FluidStack.EMPTY);
    }
}
