package slimeknights.tconstruct.tables.menu.module;

import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;

import org.jetbrains.annotations.Nullable;

public class SideInventoryContainer<TILE extends BlockEntity> extends BaseContainerMenu<TILE> {

    @Getter
    private final int columns;
    @Getter
    private final int slotCount;
    protected final LazyOptional<IItemHandler> itemHandler;

    public SideInventoryContainer(ScreenHandlerType<?> containerType, int windowId, PlayerInventory inv, @Nullable TILE tile, int x, int y, int columns) {
        this(containerType, windowId, inv, tile, null, x, y, columns);
    }

    public SideInventoryContainer(ScreenHandlerType<?> containerType, int windowId, PlayerInventory inv, @Nullable TILE tile, @Nullable Direction inventoryDirection, int x, int y, int columns) {
        super(containerType, windowId, inv, tile);

        // must have a TE
        if (tile == null) {
            this.itemHandler = LazyOptional.of(() -> EmptyHandler.INSTANCE);
        } else {
            this.itemHandler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, inventoryDirection);
        }

        // slot properties
        IItemHandler handler = this.itemHandler.orElse(EmptyHandler.INSTANCE);
        this.slotCount = handler.getSlots();
        this.columns = columns;
        int rows = this.slotCount / columns;
        if (this.slotCount % columns != 0) {
            rows++;
        }

        // add slots
        int index = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (index >= this.slotCount) {
                    break;
                }

                this.addSlot(this.createSlot(handler, index, x + c * 18, y + r * 18));
                index++;
            }
        }
    }

    /**
     * Creates a slot for this inventory
     *
     * @param itemHandler Item handler
     * @param index       Slot index
     * @param x           Slot X position
     * @param y           Slot Y position
     * @return Inventory slot
     */
    protected Slot createSlot(IItemHandler itemHandler, int index, int x, int y) {
        return new SmartItemHandlerSlot(itemHandler, index, x, y);
    }
}
