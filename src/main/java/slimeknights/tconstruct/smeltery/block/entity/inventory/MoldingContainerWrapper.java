package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.recipe.molding.IMoldingContainer;

/**
 * Wrapper around an item handler for the sake of use as a molding inventory
 */
@RequiredArgsConstructor
public class MoldingContainerWrapper implements IMoldingContainer {
    private final IItemHandler handler;
    private final int slot;

    @Getter
    @Setter
    private ItemStack pattern = ItemStack.EMPTY;

    @Override
    public ItemStack getMaterial() {
        return this.handler.getStackInSlot(this.slot);
    }
}
