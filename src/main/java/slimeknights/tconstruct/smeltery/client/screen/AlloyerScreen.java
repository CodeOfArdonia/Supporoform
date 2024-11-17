package slimeknights.tconstruct.smeltery.client.screen;

import net.minecraft.client.gui.DrawContext;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.smeltery.block.entity.controller.AlloyerBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.MixerAlloyTank;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiTankModule;
import slimeknights.tconstruct.smeltery.menu.AlloyerContainerMenu;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AlloyerScreen extends HandledScreen<AlloyerContainerMenu> implements IScreenWithFluidTank {
    private static final int[] INPUT_TANK_START_X = {54, 22, 38, 70, 6};
    private static final Identifier BACKGROUND = TConstruct.getResource("textures/gui/alloyer.png");
    private static final ElementScreen SCALA = new ElementScreen(176, 0, 34, 52, 256, 256);
    private static final ElementScreen FUEL_SLOT = new ElementScreen(176, 52, 18, 36, 256, 256);
    private static final ElementScreen FUEL_TANK = new ElementScreen(194, 52, 14, 38, 256, 256);
    private static final ElementScreen INPUT_TANK = new ElementScreen(208, 52, 16, 54, 256, 256);

    private final GuiFuelModule fuel;
    private final GuiTankModule outputTank;
    private GuiTankModule[] inputTanks = new GuiTankModule[0];

    public AlloyerScreen(AlloyerContainerMenu container, PlayerInventory inv, Text name) {
        super(container, inv, name);
        AlloyerBlockEntity te = container.getTile();
        if (te != null) {
            FuelModule fuelModule = te.getFuelModule();
            fuel = new GuiFuelModule(this, fuelModule, 153, 32, 12, 36, 152, 15, container.isHasFuelSlot());
            outputTank = new GuiTankModule(this, te.getTank(), 114, 16, 34, 52, AlloyerContainerMenu.TOOLTIP_FORMAT);
            updateTanks();
        } else {
            fuel = null;
            outputTank = null;
        }
    }

    /**
     * Updates the tanks from the tile entity
     */
    private void updateTanks() {
        AlloyerBlockEntity te = handler.getTile();
        if (te != null) {
            MixerAlloyTank alloyTank = te.getAlloyTank();
            int numTanks = alloyTank.getTanks();
            GuiTankModule[] tanks = new GuiTankModule[numTanks];
            int max = Math.min(numTanks, 5); // only support 5 tanks, any more is impossible
            for (int i = 0; i < max; i++) {
                tanks[i] = new GuiTankModule(this, alloyTank.getFluidHandler(i), INPUT_TANK_START_X[i], 16, 14, 52, AlloyerContainerMenu.TOOLTIP_FORMAT);
            }
            this.inputTanks = tanks;
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        // if the input count changes, update
        AlloyerBlockEntity te = handler.getTile();
        if (te != null && te.getAlloyTank().getTanks() != inputTanks.length) {
            this.updateTanks();
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

        // fluids
        if (outputTank != null) outputTank.draw(context);

        // draw tank backgrounds first, then draw tank contents, less binding
        RenderUtils.setup(BACKGROUND);
        for (GuiTankModule tankModule : inputTanks) {
            INPUT_TANK.draw(context, tankModule.getX() - 1 + this.x, tankModule.getY() - 1 + this.y);
        }

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

        // draw tank contents last, reduces bind calls
        for (GuiTankModule tankModule : inputTanks) {
            tankModule.draw(context);
        }
    }



    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        int checkX = mouseX - this.x;
        int checkY = mouseY - this.y;

        // highlight hovered tank
        if (outputTank != null) outputTank.highlightHoveredFluid(context, checkX, checkY);
        for (GuiTankModule tankModule : inputTanks) {
            tankModule.highlightHoveredFluid(context, checkX, checkY);
        }

        // highlight hovered fuel
        if (fuel != null) fuel.renderHighlight(context, checkX, checkY);

        // scala
        assert client != null;
        RenderUtils.setup(BACKGROUND);
        SCALA.draw(context, 114, 16);
    }

    @Override
    public void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        // tank tooltip
        if (outputTank != null) outputTank.renderTooltip(context, mouseX, mouseY);

        for (GuiTankModule tankModule : inputTanks) {
            tankModule.renderTooltip(context, mouseX, mouseY);
        }

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
        if (this.fuel != null) {
            ingredient = this.fuel.getIngredient(checkX, checkY);
        }

        // next output tank
        if (this.outputTank != null && ingredient == null) {
            ingredient = this.outputTank.getIngreientUnderMouse(checkX, checkY);
        }

        // finally input tanks
        if (ingredient == null) {
            for (GuiTankModule tankModule : this.inputTanks) {
                ingredient = tankModule.getIngreientUnderMouse(checkX, checkY);
                if (ingredient != null) {
                    return ingredient;
                }
            }
        }

        return ingredient;
    }
}
