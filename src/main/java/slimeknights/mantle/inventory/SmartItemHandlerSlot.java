package slimeknights.mantle.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Forge still uses dumb vanilla logic for determining slot limits instead of their own method
 */
public class SmartItemHandlerSlot extends SlotItemHandler {
    public SmartItemHandlerSlot(SlottedStorage<ItemVariant> itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public int getMaxItemCount(@NotNull ItemStack stack) {
        return Math.min(stack.getMaxCount(), this.getItemHandler().getSlot(this.getSlotIndex()));
    }
}
