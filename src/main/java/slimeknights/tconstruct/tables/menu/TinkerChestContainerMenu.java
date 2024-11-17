package slimeknights.tconstruct.tables.menu;

import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.chest.AbstractChestBlockEntity;
import slimeknights.tconstruct.tables.menu.module.SideInventoryContainer;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;

public class TinkerChestContainerMenu extends TabbedContainerMenu<AbstractChestBlockEntity> {
    protected SideInventoryContainer<AbstractChestBlockEntity> inventory;

    public TinkerChestContainerMenu(int id, PlayerInventory inv, @Nullable AbstractChestBlockEntity tileEntity) {
        super(TinkerTables.tinkerChestContainer.get(), id, inv, tileEntity);
        // columns don't matter since they get set by gui
        if (this.tile != null) {
            this.inventory = new DynamicChestInventory(TinkerTables.tinkerChestContainer.get(), this.syncId, inv, this.tile, 8, 18, 8);
            this.addSubContainer(this.inventory, true);
        }
        this.addInventorySlots();
    }

    public TinkerChestContainerMenu(int id, PlayerInventory inv, PacketByteBuf buf) {
        this(id, inv, getTileEntityFromBuf(buf, AbstractChestBlockEntity.class));
    }

    /**
     * Resizable inventory
     */
    public static class DynamicChestInventory extends SideInventoryContainer<AbstractChestBlockEntity> {
        public DynamicChestInventory(ScreenHandlerType<?> containerType, int windowId, PlayerInventory inv, AbstractChestBlockEntity tile, int x, int y, int columns) {
            super(containerType, windowId, inv, tile, x, y, columns);
        }
    }
}
