package slimeknights.tconstruct.tables.menu.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.inventory.CustomResultSlot;
import slimeknights.mantle.inventory.IContainerCraftingCustom;

/**
 * Same as {@link CustomResultSlot}, but does not require an crafting inventory
 */
public class CraftingResultSlot extends net.minecraft.screen.slot.CraftingResultSlot {
    private final IContainerCraftingCustom callback;

    @SuppressWarnings("ConstantConditions")
    public CraftingResultSlot(IContainerCraftingCustom callback, PlayerEntity player, Inventory inv, int index, int x, int y) {
        // pass in null for CraftingInventory
        super(player, null, inv, index, x, y);
        this.callback = callback;
    }

    /* Methods that reference CraftingInventory */

    @Override
    protected void onCrafted(ItemStack stack) {
        if (this.amount > 0) {
            stack.onCraft(this.player.getWorld(), this.player, this.amount);
            ForgeEventFactory.firePlayerCraftingEvent(this.player, stack, this.inventory);
        }
        this.amount = 0;
    }

    @Override
    public void onTakeItem(PlayerEntity playerIn, @NotNull ItemStack stack) {
        this.onCrafted(stack);
        this.callback.onCrafting(playerIn, stack, this.inventory);
    }
}
