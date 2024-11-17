package slimeknights.tconstruct.tables.menu.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;

/**
 * Slot for display of {@link LazyResultContainer}.
 */
@SuppressWarnings("WeakerAccess")
public class LazyResultSlot extends Slot {
    protected final LazyResultContainer inventory;
    protected int amountCrafted = 0;

    public LazyResultSlot(LazyResultContainer inventory, int xPosition, int yPosition) {
        super(inventory, 0, xPosition, yPosition);
        this.inventory = inventory;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack takeStack(int amount) {
        if (this.hasStack()) {
            this.amountCrafted += Math.min(amount, this.getStack().getCount());
        }

        return super.takeStack(amount);
    }

    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        this.inventory.craftResult(player, this.amountCrafted);
        this.amountCrafted = 0;
    }

    @Override
    protected void onCrafted(ItemStack stack, int amount) {
        this.amountCrafted += amount;
        this.onCrafted(stack);
    }

    @Override
    protected void onTake(int numItemsCrafted) {
        this.amountCrafted += numItemsCrafted;
    }
}
