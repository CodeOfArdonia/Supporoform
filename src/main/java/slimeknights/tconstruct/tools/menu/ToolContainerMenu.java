package slimeknights.tconstruct.tools.menu;

import lombok.Getter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerTools;

/**
 * Container for a tool inventory
 */
public class ToolContainerMenu extends ScreenHandler {
    /**
     * Size of a single slot
     */
    public static final int SLOT_SIZE = 18;
    /**
     * Y start of the repeat slots background
     */
    public static final int REPEAT_BACKGROUND_START = 17;

    /**
     * Stack containing the tool being rendered
     */
    @Getter
    private final ItemStack stack;
    /**
     * Item handler being rendered
     */
    @Getter
    private final IItemHandler itemHandler;
    private final PlayerEntity player;
    @Getter
    private final EquipmentSlot slotType;
    @Getter
    private final int selectedHotbarSlot;
    @Getter
    private final boolean showOffhand;
    /**
     * Index of the first player inventory slot
     */
    private final int playerInventoryStart;

    public ToolContainerMenu(int id, PlayerInventory playerInventory, ItemStack stack, IItemHandlerModifiable itemHandler, EquipmentSlot slotType) {
        this(TinkerTools.toolContainer.get(), id, playerInventory, stack, itemHandler, slotType);
    }

    /**
     * Creates a new instance of this container on the client side
     */
    public static ToolContainerMenu forClient(int id, PlayerInventory inventory, PacketByteBuf buffer) {
        EquipmentSlot slotType = buffer.readEnumConstant(EquipmentSlot.class);
        ItemStack stack = inventory.player.getEquippedStack(slotType);
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).filter(cap -> cap instanceof IItemHandlerModifiable).orElse(EmptyItemHandler.INSTANCE);
        return new ToolContainerMenu(TinkerTools.toolContainer.get(), id, inventory, stack, handler, slotType);
    }

    protected ToolContainerMenu(@Nullable ScreenHandlerType<?> type, int id, PlayerInventory playerInventory, ItemStack stack, IItemHandler handler, EquipmentSlot slotType) {
        super(type, id);
        this.stack = stack;
        this.itemHandler = handler;
        this.player = playerInventory.player;
        this.slotType = slotType;

        // add tool slots
        int slots = this.itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            this.addSlot(new ToolContainerSlot(this.itemHandler, i, 8 + (i % 9) * SLOT_SIZE, (REPEAT_BACKGROUND_START + 1) + (i / 9) * SLOT_SIZE));
        }
        // add offhand if requested
        this.showOffhand = ModifierUtil.checkVolatileFlag(stack, ToolInventoryCapability.INCLUDE_OFFHAND);
        if (this.showOffhand) {
            int x = 8 + (slots % 9) * SLOT_SIZE;
            int y = (REPEAT_BACKGROUND_START + 1) + (slots / 9) * SLOT_SIZE;
            if (slotType == EquipmentSlot.OFFHAND) {
                this.addSlot(new ReadOnlySlot(playerInventory, 40, x, y));
            } else {
                this.addSlot(new Slot(playerInventory, 40, x, y));
            }
            slots++;
        }

        this.playerInventoryStart = this.slots.size();

        // add player slots
        int playerY = 32 + SLOT_SIZE * ((slots + 8) / 9);
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, playerY + r * 18));
            }
        }
        int hotbarStart = playerY + 58;
        this.selectedHotbarSlot = slotType == EquipmentSlot.MAINHAND ? playerInventory.selectedSlot : (slotType == EquipmentSlot.OFFHAND ? 10 : -1);
        for (int c = 0; c < 9; ++c) {
            if (c == this.selectedHotbarSlot) {
                this.addSlot(new ReadOnlySlot(playerInventory, c, 8 + c * 18, hotbarStart));
            } else {
                this.addSlot(new Slot(playerInventory, c, 8 + c * 18, hotbarStart));
            }
        }
    }

    @Override
    public boolean canUse(PlayerEntity playerIn) {
        // if the stack ever leaves the slot, close the menu, as we have no way to recover then and dupes are likely
        return this.player == playerIn && !this.stack.isEmpty() && this.player.getEquippedStack(this.slotType) == this.stack;
    }

    @Override
    public ItemStack quickMove(PlayerEntity playerIn, int index) {
        if (this.playerInventoryStart < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            int end = this.slots.size();
            if (index < this.playerInventoryStart) {
                if (!this.insertItem(slotStack, this.playerInventoryStart, end, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(slotStack, 0, this.playerInventoryStart, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.setStackNoCallbacks(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }

    private static class ToolContainerSlot extends SmartItemHandlerSlot {

        private final int index;

        public ToolContainerSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.index = index;
        }

        @Override
        public void setStack(@NotNull ItemStack stack) {
            // using set as an indicator it changed, so no need to call setChanged anymore here
            ((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(this.index, stack);
        }

        @Override
        public void markDirty() {
            // no proper setChanged method on item handler, so just set the existing stack
            this.setStack(getStack());
        }
    }
}
