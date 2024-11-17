package slimeknights.tconstruct.smeltery.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiMeltingModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiSmelteryTank;
import slimeknights.tconstruct.smeltery.client.screen.module.HeatingStructureSideInventoryScreen;
import slimeknights.tconstruct.smeltery.menu.HeatingStructureContainerMenu;

import java.util.Objects;

public class HeatingStructureScreen extends MultiModuleScreen<HeatingStructureContainerMenu> implements IScreenWithFluidTank {
    public static final Identifier BACKGROUND = TConstruct.getResource("textures/gui/smeltery.png");
    private static final ElementScreen SCALA = new ElementScreen(176, 76, 52, 52, 256, 256);

    private final HeatingStructureSideInventoryScreen sideInventory;
    private final HeatingStructureBlockEntity te;
    private final GuiSmelteryTank tank;
    public final GuiMeltingModule melting;
    private final GuiFuelModule fuel;

    public HeatingStructureScreen(HeatingStructureContainerMenu container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);

        HeatingStructureBlockEntity te = container.getTile();
        if (te != null) {
            this.te = te;
            this.tank = new GuiSmelteryTank(this, te.getTank(), 8, 16, SCALA.w, SCALA.h, Objects.requireNonNull(Registries.BLOCK_ENTITY_TYPE.getId(te.getType())));
            int slots = te.getMeltingInventory().getSlots();
            this.sideInventory = new HeatingStructureSideInventoryScreen(this, container.getSideInventory(), playerInventory, slots, HeatingStructureContainerMenu.calcColumns(slots));
            addModule(sideInventory);
            FuelModule fuelModule = te.getFuelModule();
            this.melting = new GuiMeltingModule(this, te.getMeltingInventory(), fuelModule::getTemperature, sideInventory::shouldDrawSlot);
            this.fuel = new GuiFuelModule(this, fuelModule, 71, 32, 12, 36, 70, 15, false);
        } else {
            this.te = null;
            this.tank = null;
            this.melting = null;
            this.fuel = null;
            this.sideInventory = null;
        }
    }


    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        // if the smeltery becomes invalid or the slot size changes, kill the UI
        if (te == null || !te.getCachedState().get(ControllerBlock.IN_STRUCTURE)
                || te.getMeltingInventory().getSlots() != sideInventory.getSlotCount()) {
            this.close();
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        // draw stuff with background
        GuiUtil.drawBackground(context, this, BACKGROUND);
        // fuel
        if (fuel != null) {
            fuel.draw(context);
        }

        // draw other components
        super.drawBackground(context, partialTicks, mouseX, mouseY);

        // render fluids
        if (tank != null) tank.renderFluids(context);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        assert client != null;
        RenderUtils.setup(BACKGROUND);
        SCALA.draw(context, 8, 16);

        // highlight hovered fluids
        if (tank != null) tank.renderHighlight(context, mouseX, mouseY);
        if (fuel != null) fuel.renderHighlight(context, mouseX - this.x, mouseY - this.y);

        // while this might make sense to draw in the side inventory logic, slots are rendered by the parent screen it seems
        // so we get the most accurate offset rendering it here, as we offset the foreground of submodules but they don't draw their own slots
        // I hate the whole multimodule system right now
        if (melting != null) melting.drawHeatBars(context);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        // fluid tooltips
        if (tank != null) tank.drawTooltip(context, mouseX, mouseY);
        if (fuel != null) {
            boolean hasTank = false;
            if (te.getStructure() != null) {
                hasTank = te.getStructure().hasTanks();
            }
            fuel.addTooltip(context, mouseX, mouseY, hasTank);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && tank != null) {
            tank.handleClick((int) mouseX - cornerX, (int) mouseY - cornerY);
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Nullable
    @Override
    public Object getIngredientUnderMouse(double mouseX, double mouseY) {
        Object ingredient = null;

        int checkX = (int) mouseX - cornerX;
        int checkY = (int) mouseY - cornerY;

        // try fuel first, its faster
        if (this.fuel != null) ingredient = this.fuel.getIngredient(checkX, checkY);
        // then try tank
        if (this.tank != null && ingredient == null) ingredient = this.tank.getIngredient(checkX, checkY);

        return ingredient;
    }
}
