package slimeknights.tconstruct.tables.menu;

import lombok.Getter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.menu.slot.ArmorSlot;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;
import slimeknights.tconstruct.tables.menu.slot.TinkerStationSlot;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TinkerStationContainerMenu extends TabbedContainerMenu<TinkerStationBlockEntity> {
    @Getter
    private final List<Slot> inputSlots;
    private final LazyResultSlot resultSlot;

    /**
     * Standard constructor
     *
     * @param id   Window ID
     * @param inv  Player inventory
     * @param tile Relevant tile entity
     */
    public TinkerStationContainerMenu(int id, PlayerInventory inv, @Nullable TinkerStationBlockEntity tile) {
        super(TinkerTables.tinkerStationContainer.get(), id, inv, tile);

        // unfortunately, nothing works with no tile
        if (tile != null) {
            tile.setItemName("");
            // send the player the current recipe, as we only sync to open containers
            tile.syncRecipe(inv.player);

            this.inputSlots = new ArrayList<>();
            this.addSlot(new TinkerStationSlot(tile, TinkerStationBlockEntity.TINKER_SLOT, 0, 0));

            for (int index = 0; index < tile.size() - 1; index++) {
                this.inputSlots.add(this.addSlot(new TinkerStationSlot(tile, index + TinkerStationBlockEntity.INPUT_SLOT, 0, 0)));
            }

            // add result slot, will fetch result cache
            this.addSlot(this.resultSlot = new LazyResultSlot(tile.getCraftingResult(), 114, 38));
            // set initial slot filters and activations
            this.setToolSelection(StationSlotLayoutLoader.getInstance().get(Registries.BLOCK.getId(tile.getCachedState().getBlock())));
        } else {
            // requirement for final variable
            this.resultSlot = null;
            this.inputSlots = Collections.emptyList();
        }

        // add armor and offhand slots, for convenience
        for (ArmorSlotType slotType : ArmorSlotType.values()) {
            int index = slotType.getIndex();
            this.addSlot(new ArmorSlot(inv, slotType.getEquipmentSlot(), 152, 16 + (3 - index) * 18));
        }
        this.addSlot(new Slot(inv, 40, 132, 70).setBackground(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT));

        this.addInventorySlots();
    }

    /**
     * Factory constructor
     *
     * @param id  Window ID
     * @param inv Player inventory
     * @param buf Buffer for fetching tile
     */
    public TinkerStationContainerMenu(int id, PlayerInventory inv, PacketByteBuf buf) {
        this(id, inv, getTileEntityFromBuf(buf, TinkerStationBlockEntity.class));
    }

    @Override
    protected int getInventoryYOffset() {
        return 92;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot != this.resultSlot && super.canInsertIntoSlot(stack, slot);
    }

    /**
     * Updates the active slots from the screen
     *
     * @param layout New layout
     */
    public void setToolSelection(StationSlotLayout layout) {
        assert this.tile != null;
        int maxSize = this.tile.size();
        for (int i = 0; i < maxSize; i++) {
            Slot slot = this.slots.get(i);
            if (slot instanceof TinkerStationSlot slotToolPart) {
                // activate or deactivate the slots, sets the filters
                LayoutSlot layoutSlot = layout.getSlot(i);
                if (layoutSlot.isHidden()) {
                    slotToolPart.deactivate();
                } else {
                    slotToolPart.activate(layoutSlot);
                }
            }
        }
    }

    public ItemStack getResult() {
        return this.resultSlot.getStack();
    }
}
