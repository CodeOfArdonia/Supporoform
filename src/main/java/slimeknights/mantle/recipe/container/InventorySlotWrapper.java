package slimeknights.mantle.recipe.container;

import lombok.AllArgsConstructor;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Implementation of {@link ISingleStackContainer} to wrap another {@link IInventory}
 */
@AllArgsConstructor
public class InventorySlotWrapper implements ISingleStackContainer {
    private final Inventory parent;
    private final int index;

    @Override
    public ItemStack getStack() {
        return this.parent.getStack(this.index);
    }
}
