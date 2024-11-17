package slimeknights.tconstruct.tables.menu;

import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import slimeknights.mantle.util.sync.LambdaDataSlot;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.ModifierWorktableBlockEntity;
import slimeknights.tconstruct.tables.menu.slot.ArmorSlot;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModifierWorktableContainerMenu extends TabbedContainerMenu<ModifierWorktableBlockEntity> {
    // slots
    @Getter
    private final List<Slot> inputSlots;
    @Getter
    private final LazyResultSlot outputSlot;

    public ModifierWorktableContainerMenu(int windowIdIn, PlayerInventory inv, @Nullable ModifierWorktableBlockEntity tile) {
        super(TinkerTables.modifierWorktableContainer.get(), windowIdIn, inv, tile);

        // unfortunately, nothing works with no tile
        if (tile != null) {
            // send the player the current recipe, as we only sync to open containers
//      tile.syncRecipe(inv.player);
            // slots
            this.addSlot(this.outputSlot = new LazyResultSlot(tile.getCraftingResult(), 125, 33));
            // inputs
            inputSlots = new ArrayList<>();
            inputSlots.add(this.addSlot(new WorktableSlot(this, tile, ModifierWorktableBlockEntity.TINKER_SLOT, 8, 15)));
            for (int index = 0; index < tile.size() - 1; index++) {
                inputSlots.add(this.addSlot(new WorktableSlot(this, tile, index + ModifierWorktableBlockEntity.INPUT_START, 8, 35 + 18 * index)));
            }

            // listen for the button to change in the tile
            this.addProperty(new LambdaDataSlot(-1, tile::getSelectedIndex, i -> {
                tile.selectModifier(i);
                this.updateScreen();
            }));
            // update for the first time
            this.updateScreen();
        } else {
            this.inputSlots = Collections.emptyList();
            this.outputSlot = null;
        }

        // add armor and offhand slots, for convenience
        for (ArmorSlotType slotType : ArmorSlotType.values()) {
            int index = slotType.getIndex();
            this.addSlot(new ArmorSlot(inv, slotType.getEquipmentSlot(), 152, 8 + (3 - index) * 18));
        }
        this.addSlot(new Slot(inv, 40, 132, 62).setBackground(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT));

        // other inventories
        this.addChestSideInventory();
        this.addInventorySlots();
    }

    public ModifierWorktableContainerMenu(int id, PlayerInventory inv, PacketByteBuf buf) {
        this(id, inv, getTileEntityFromBuf(buf, ModifierWorktableBlockEntity.class));
    }

    @Override
    public void onContentChanged(Inventory inventoryIn) {

    }

    /**
     * Called when a pattern button is pressed
     */
    @Override
    public boolean onButtonClick(PlayerEntity playerIn, int id) {
        // no letting ghosts choose modifiers
        if (playerIn.isSpectator()) {
            return false;
        }
        if (id >= 0 && tile != null) {
            tile.selectModifier(id);
        }
        return true;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slotIn) {
        return slotIn != this.outputSlot && super.canInsertIntoSlot(stack, slotIn);
    }

    /**
     * Slot to update recipe on change
     */
    private static class WorktableSlot extends Slot {

        private final TabbedContainerMenu<?> menu;
        private final ModifierWorktableBlockEntity tile;

        public WorktableSlot(TabbedContainerMenu<?> menu, ModifierWorktableBlockEntity tile, int index, int xPosition, int yPosition) {
            super(tile, index, xPosition, yPosition);
            this.menu = menu;
            this.tile = tile;
        }

        @Override
        public void markDirty() {
            tile.onSlotChanged(id);
            super.markDirty();
            menu.updateScreen();
        }
    }
}
