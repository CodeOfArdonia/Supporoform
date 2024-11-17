package slimeknights.tconstruct.tables.menu.slot;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

/**
 * Class for common logic with tinker station input slots
 */
public class TinkerStationSlot extends Slot {
    private final LazyResultContainer craftResult;
    private LayoutSlot layout = null;

    public TinkerStationSlot(TinkerStationBlockEntity tile, int index, int xPosition, int yPosition) {
        super(tile, index, xPosition, yPosition);
        this.craftResult = tile.getCraftingResult();
    }

    /**
     * If true, this slot is inactive
     */
    public boolean isDormant() {
        return this.layout == null;
    }

    /**
     * Activates this slot
     */
    public void activate(LayoutSlot layout) {
        this.layout = layout;
    }

    /**
     * Deactivates this slot
     */
    public void deactivate() {
        this.layout = null;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        // dormant slots don't take any items, they can only be taken out of
        return stack.isEmpty() || (this.layout != null && this.layout.isValid(stack));
    }

    @Override
    public void markDirty() {
        this.craftResult.clear();
        super.markDirty();
    }
}
