package slimeknights.tconstruct.tools.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class ReadOnlySlot extends Slot {
    public ReadOnlySlot(Inventory inventoryIn, int index, int xPosition, int yPosition) {
        super(inventoryIn, index, xPosition, yPosition);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public void setStackNoCallbacks(ItemStack stack) {
    }

    @Override
    public ItemStack takeStack(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerIn) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
