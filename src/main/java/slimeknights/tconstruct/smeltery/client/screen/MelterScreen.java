package slimeknights.tconstruct.smeltery.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiMeltingModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiTankModule;
import slimeknights.tconstruct.smeltery.menu.MelterContainerMenu;

public class MelterScreen extends HandledScreen<MelterContainerMenu> implements IScreenWithFluidTank {
    private static final Identifier BACKGROUND = TConstruct.getResource("textures/gui/melter.png");
    private static final ElementScreen SCALA = new ElementScreen(176, 0, 52, 52, 256, 256);
    private static final ElementScreen FUEL_SLOT = new ElementScreen(176, 52, 18, 36, 256, 256);
    private static final ElementScreen FUEL_TANK = new ElementScreen(194, 52, 14, 38, 256, 256);

    private final GuiMeltingModule melting;
    private final GuiFuelModule fuel;
    private final GuiTankModule tank;

    public MelterScreen(MelterContainerMenu container, PlayerInventory inv, Text name) {
        super(container, inv, name);
        MelterBlockEntity te = container.getTile();
        if (te != null) {
            FuelModule fuelModule = te.getFuelModule();
            melting = new GuiMeltingModule(this, te.getMeltingInventory(), fuelModule::getTemperature, slot -> true);
            fuel = new GuiFuelModule(this, fuelModule, 153, 32, 12, 36, 152, 15, container.isHasFuelSlot());
            tank = new GuiTankModule(this, te.getTank(), 90, 16, 52, 52, MelterContainerMenu.TOOLTIP_FORMAT);
        } else {
            melting = null;
            fuel = null;
            tank = null;
        }
    }

    @Override
    public void render(DrawContext context, int x, int y, float partialTicks) {
        this.renderBackground(context);
        super.render(context, x, y, partialTicks);
        this.drawMouseoverTooltip(context, x, y);
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        GuiUtil.drawBackground(context, this, BACKGROUND);

        // fuel
        if (fuel != null) {
            // draw the correct background for the fuel type
            if (handler.isHasFuelSlot()) {
                FUEL_SLOT.draw(context, x + 150, y + 31);
            } else {
                FUEL_TANK.draw(context, x + 152, y + 31);
            }
            fuel.draw(context);
        }

        // fluids
        if (tank != null) tank.draw(context);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        int checkX = mouseX - this.x;
        int checkY = mouseY - this.y;

        // highlight hovered tank
        if (tank != null) tank.highlightHoveredFluid(context, checkX, checkY);
        // highlight hovered fuel
        if (fuel != null) fuel.renderHighlight(context, checkX, checkY);

        // scala
        RenderUtils.setup(BACKGROUND);
        SCALA.draw(context, 90, 16);

        // heat bars
        if (melting != null) {
            melting.drawHeatBars(context);
        }
    }

    @Override
    public void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        // tank tooltip
        if (tank != null) tank.renderTooltip(context, mouseX, mouseY);

        // heat tooltips
        if (melting != null) melting.drawHeatTooltips(context, mouseX, mouseY);

        // fuel tooltip
        if (fuel != null) fuel.addTooltip(context, mouseX, mouseY, true);
    }

    @Nullable
    @Override
    public Object getIngredientUnderMouse(double mouseX, double mouseY) {
        Object ingredient = null;
        int checkX = (int) mouseX - x;
        int checkY = (int) mouseY - y;

        // try fuel first, its faster
        if (this.fuel != null)
            ingredient = this.fuel.getIngredient(checkX, checkY);

        if (this.tank != null && ingredient == null)
            ingredient = this.tank.getIngreientUnderMouse(checkX, checkY);

        return ingredient;
    }
}
