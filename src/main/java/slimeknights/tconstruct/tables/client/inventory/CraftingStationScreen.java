package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;

public class CraftingStationScreen extends BaseTabbedScreen<CraftingStationBlockEntity, CraftingStationContainerMenu> {
    private static final Identifier CRAFTING_TABLE_GUI_TEXTURES = new Identifier("textures/gui/container/crafting_table.png");

    public CraftingStationScreen(CraftingStationContainerMenu container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);
        addChestSideInventory(playerInventory);
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        this.drawBackground(context, CRAFTING_TABLE_GUI_TEXTURES);
        super.drawBackground(context, partialTicks, mouseX, mouseY);
    }
}
