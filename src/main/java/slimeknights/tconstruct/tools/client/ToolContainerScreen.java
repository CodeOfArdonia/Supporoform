package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.InventoryModifierHook;
import slimeknights.tconstruct.library.tools.layout.Patterns;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.menu.ToolContainerMenu;

import java.util.List;
import java.util.function.Function;

import static slimeknights.tconstruct.tools.menu.ToolContainerMenu.REPEAT_BACKGROUND_START;
import static slimeknights.tconstruct.tools.menu.ToolContainerMenu.SLOT_SIZE;

/**
 * Screen for a tool inventory
 */
public class ToolContainerScreen extends HandledScreen<ToolContainerMenu> {
    /**
     * The ResourceLocation containing the chest GUI texture.
     */
    private static final Identifier TEXTURE = TConstruct.getResource("textures/gui/tool.png");

    /**
     * Max number of rows in the repeat slots background
     */
    private static final int REPEAT_BACKGROUND_ROWS = 6;
    /**
     * Start location of the player inventory
     */
    private static final int PLAYER_INVENTORY_START = REPEAT_BACKGROUND_START + (REPEAT_BACKGROUND_ROWS * SLOT_SIZE);
    /**
     * Height of the player inventory texture
     */
    private static final int PLAYER_INVENTORY_HEIGHT = 97;
    /**
     * Start Y location of the slot start element
     */
    private static final int SLOTS_START = 256 - SLOT_SIZE;
    /**
     * Selected slot texture X position
     */
    private static final int SELECTED_X = 176;

    /**
     * Total number of slots in the inventory
     */
    private final int slots;
    /**
     * Number of rows in this inventory
     */
    private final int inventoryRows;
    /**
     * Number of slots in the final row
     */
    private final int slotsInLastRow;
    /**
     * Tool instance being rendered
     */
    private final IToolStackView tool;

    public ToolContainerScreen(ToolContainerMenu menu, PlayerInventory inv, Text title) {
        super(menu, inv, title);
        int slots = menu.getItemHandler().getSlots();
        if (menu.isShowOffhand()) {
            slots++;
        }
        int inventoryRows = slots / 9;
        int slotsInLastRow = slots % 9;
        if (slotsInLastRow == 0) {
            slotsInLastRow = 9;
        } else {
            inventoryRows++;
        }
        this.slots = slots;
        this.inventoryRows = inventoryRows;
        this.slotsInLastRow = slotsInLastRow;
        this.backgroundHeight = 114 + this.inventoryRows * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
        this.tool = ToolStack.from(menu.getStack());
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int index, SlotActionType type) {
        // disallow swapping the tool slot
        if (type == SlotActionType.SWAP) {
            EquipmentSlot toolSlot = this.handler.getSlotType();
            if (toolSlot == EquipmentSlot.MAINHAND && index == this.handler.getSelectedHotbarSlot() || toolSlot == EquipmentSlot.OFFHAND && index == PlayerInventory.OFF_HAND_SLOT) {
                return;
            }
        }
        super.onMouseClick(slot, slotId, index, type);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int x, int y) {
        RenderUtils.setup(TEXTURE);
        int xStart = (this.width - this.backgroundWidth) / 2;
        int yStart = (this.height - this.backgroundHeight) / 2;

        int yOffset; // after ifs, will be the height of the final element
        if (this.inventoryRows <= REPEAT_BACKGROUND_ROWS) {
            yOffset = this.inventoryRows * SLOT_SIZE + REPEAT_BACKGROUND_START;
            context.fill(xStart, yStart, 0, 0, this.backgroundWidth, yOffset);
        } else {
            // draw top area with first 6 rows
            yOffset = REPEAT_BACKGROUND_ROWS * SLOT_SIZE + REPEAT_BACKGROUND_START;
            context.fill(xStart, yStart, 0, 0, this.backgroundWidth, yOffset);

            // draw each next group of 6
            int remaining = this.inventoryRows - REPEAT_BACKGROUND_ROWS;
            int height = REPEAT_BACKGROUND_ROWS * SLOT_SIZE;
            for (; remaining > REPEAT_BACKGROUND_ROWS; remaining -= REPEAT_BACKGROUND_ROWS) {
                context.fill(xStart, yStart + yOffset, 0, REPEAT_BACKGROUND_START, this.backgroundWidth, height);
                yOffset += height;
            }

            // draw final set of up to 6
            height = remaining * SLOT_SIZE;
            context.fill(xStart, yStart + yOffset, 0, REPEAT_BACKGROUND_START, this.backgroundWidth, height);
            yOffset += height;
        }
        // draw the player inventory background
        context.fill(xStart, yStart + yOffset, 0, PLAYER_INVENTORY_START, this.backgroundWidth, PLAYER_INVENTORY_HEIGHT);

        // draw slot background
        int rowLeft = xStart + 7;
        int rowStart = yStart + REPEAT_BACKGROUND_START - SLOT_SIZE;
        for (int i = 1; i < this.inventoryRows; i++) {
            context.fill(rowLeft, rowStart + i * SLOT_SIZE, 0, SLOTS_START, 9 * SLOT_SIZE, SLOT_SIZE);
        }
        // last row may not have all slots
        context.fill(rowLeft, rowStart + this.inventoryRows * SLOT_SIZE, 0, SLOTS_START, this.slotsInLastRow * SLOT_SIZE, SLOT_SIZE);

        // draw a background on the selected slot index
        int selectedSlot = this.handler.getSelectedHotbarSlot();
        if (selectedSlot != -1) {
            int slotIndex = this.slots - 1;
            if (selectedSlot != 10) {
                slotIndex += 28 + selectedSlot;
            }
            if (slotIndex < this.handler.slots.size()) {
                Slot slot = this.handler.getSlot(slotIndex);
                context.fill(xStart + slot.x - 2, yStart + slot.y - 2, SELECTED_X, 0, SLOT_SIZE + 2, SLOT_SIZE + 2);
            }
        }


        // prepare pattern drawing
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        assert this.client != null;
        Function<Identifier, Sprite> spriteGetter = this.client.getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

        // draw slot patterns for all empty slots
        int start = 0;
        int maxSlots = this.handler.slots.size();

        List<ModifierEntry> modifiers = this.tool.getModifierList();
        modifiers:
        for (int modIndex = modifiers.size() - 1; modIndex >= 0; modIndex--) {
            ModifierEntry entry = modifiers.get(modIndex);
            InventoryModifierHook inventory = entry.getHook(ToolInventoryCapability.HOOK);
            int size = inventory.getSlots(this.tool, entry);
            for (int i = 0; i < size; i++) {
                if (start + i >= maxSlots) {
                    break modifiers;
                }
                Slot slot = this.handler.getSlot(start + i);
                Pattern pattern = inventory.getPattern(this.tool, entry, i, slot.hasStack());
                if (pattern != null) {
                    Sprite sprite = spriteGetter.apply(pattern.getTexture());
                    context.drawSprite(xStart + slot.x, yStart + slot.y, 100, 16, 16, sprite);
                }
            }
            start += size;
        }

        // offhand icon
        if (this.handler.isShowOffhand()) {
            Slot slot = this.handler.getSlot(this.slots - 1);
            if (!slot.hasStack()) {
                Sprite sprite = spriteGetter.apply(Patterns.SHIELD.getTexture());
                context.drawSprite(xStart + slot.x, yStart + slot.y, 100, 16, 16, sprite);
            }
        }
    }
}
