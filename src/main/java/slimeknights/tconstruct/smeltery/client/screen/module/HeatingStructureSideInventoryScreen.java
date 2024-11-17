package slimeknights.tconstruct.smeltery.client.screen.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.tables.client.inventory.module.SideInventoryScreen;
import slimeknights.tconstruct.tables.menu.module.SideInventoryContainer;

public class HeatingStructureSideInventoryScreen extends SideInventoryScreen<HeatingStructureScreen, SideInventoryContainer<? extends HeatingStructureBlockEntity>> {
    public static final Identifier SLOT_LOCATION = HeatingStructureScreen.BACKGROUND;

    // TODO: read from a proper place
    public HeatingStructureSideInventoryScreen(HeatingStructureScreen parent, SideInventoryContainer<? extends HeatingStructureBlockEntity> container, PlayerInventory playerInventory, int slotCount, int columns) {
        super(parent, container, playerInventory, Text.empty(), slotCount, columns, false, true);
        slot = new ScalableElementScreen(0, 166, 22, 18, 256, 256);
        slotEmpty = new ScalableElementScreen(22, 166, 22, 18, 256, 256);
        yOffset = 0;
    }

    @Override
    protected boolean shouldDrawName() {
        return false;
    }

    @Override
    protected void updateSlots() {
        // adjust for the heat bar
        xOffset += 4;
        super.updateSlots();
        xOffset -= 4;
    }

    @Override
    protected int drawSlots(MatrixStack matrices, int xPos, int yPos) {
        RenderSystem.setShaderTexture(0, SLOT_LOCATION);
        int ret = super.drawSlots(matrices, xPos, yPos);
        RenderSystem.setShaderTexture(0, GENERIC_INVENTORY);
        return ret;
    }

    @Override
    public void renderLabels(MatrixStack matrices, int mouseX, int mouseY) {
        super.renderLabels(matrices, mouseX, mouseY);
    }

    @Override
    public void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);
        if (parent.melting != null) {
            parent.melting.drawHeatTooltips(context, mouseX, mouseY);
        }
    }
}
