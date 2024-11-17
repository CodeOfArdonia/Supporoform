package slimeknights.tconstruct.smeltery.client.screen.module;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule.FuelInfo;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * GUI component handling the fuel module
 */
@RequiredArgsConstructor
public class GuiFuelModule {
    private static final ScalableElementScreen FIRE = new ScalableElementScreen(176, 136, 14, 14, 256, 256);

    // tooltips
    private static final String TOOLTIP_TEMPERATURE = TConstruct.makeTranslationKey("gui", "melting.fuel.temperature");
    private static final List<Text> TOOLTIP_NO_TANK = Collections.singletonList(Text.translatable(TConstruct.makeTranslationKey("gui", "melting.fuel.no_tank")));
    private static final List<Text> TOOLTIP_NO_FUEL = Collections.singletonList(Text.translatable(TConstruct.makeTranslationKey("gui", "melting.fuel.empty")));
    private static final Text TOOLTIP_INVALID_FUEL = Text.translatable(TConstruct.makeTranslationKey("gui", "melting.fuel.invalid")).formatted(Formatting.RED);
    private static final Text TOOLTIP_SOLID_FUEL = Text.translatable(TConstruct.makeTranslationKey("gui", "melting.fuel.solid"));

    private final HandledScreen<?> screen;
    private final FuelModule fuelModule;
    /**
     * location to draw the tank
     */
    private final int x, y, width, height;
    /**
     * location to draw the fire
     */
    private final int fireX, fireY;
    /**
     * If true, UI has a fuel slot
     */
    private final boolean hasFuelSlot;

    private FuelInfo fuelInfo = FuelInfo.EMPTY;

    /**
     * Checks if the fuel tank is hovered
     *
     * @param checkX X position to check
     * @param checkY Y position to check
     * @return True if hovered
     */
    private boolean isHovered(int checkX, int checkY) {
        return GuiUtil.isHovered(checkX, checkY, x - 1, y - 1, width + 2, height + 2);
    }

    /**
     * Draws the fuel at the correct location
     *
     * @param context Matrix stack instance
     */
    public void draw(DrawContext context) {
        // draw fire
        int fuel = fuelModule.getFuel();
        int fuelQuality = fuelModule.getFuelQuality();
        if (fuel > 0 && fuelQuality > 0) {
            FIRE.drawScaledYUp(context, fireX + screen.x, fireY + screen.y, 14 * fuel / fuelQuality);
        }

        // draw tank second, it changes the image
        // store fuel info into a field for other methods, this one updates most often
        if (!hasFuelSlot) {
            fuelInfo = fuelModule.getFuelInfo();
            if (!fuelInfo.isEmpty()) {
                GuiUtil.renderFluidTank(context, screen, fuelInfo.getFluid(), fuelInfo.getTotalAmount(), fuelInfo.getCapacity(), x, y, width, height, 100);
            }
        }
    }

    /**
     * Highlights the hovered fuel
     *
     * @param context Matrix stack instance
     * @param checkX   Top corner relative mouse X
     * @param checkY   Top corner relative mouse Y
     */
    public void renderHighlight(DrawContext context, int checkX, int checkY) {
        if (isHovered(checkX, checkY)) {
            // if there is a fuel slot, render highlight lower
            if (hasFuelSlot) {
                if (checkY > y + 18) {
                    GuiUtil.renderHighlight(context, x, y + 18, width, height - 18);
                }
            } else {
                // full fluid highlight
                GuiUtil.renderHighlight(context, x, y, width, height);
            }
        }
    }

    /**
     * Adds the tooltip for the fuel
     *
     * @param context Matrix stack instance
     * @param mouseX   Mouse X position
     * @param mouseY   Mouse Y position
     */
    public void addTooltip(DrawContext context, int mouseX, int mouseY, boolean hasTank) {
        int checkX = mouseX - screen.x;
        int checkY = mouseY - screen.y;

        if (isHovered(checkX, checkY)) {
            List<Text> tooltip;
            // if an item or we have a fuel slot, do item tooltip
            if (hasFuelSlot || fuelInfo.isItem()) {
                // if there is a fuel slot, start below the fuel slot
                if (!hasFuelSlot || checkY > y + 18) {
                    if (hasTank) {
                        // no invalid fuel, we assume the slot is validated (hasFuelSlot is only true for the heater which validates)
                        int temperature = fuelModule.getTemperature();
                        if (temperature > 0) {
                            tooltip = Arrays.asList(TOOLTIP_SOLID_FUEL, Text.translatable(TOOLTIP_TEMPERATURE, temperature).formatted(Formatting.GRAY, Formatting.ITALIC));
                        } else {
                            tooltip = TOOLTIP_NO_FUEL;
                        }
                    } else {
                        tooltip = TOOLTIP_NO_TANK;
                    }
                } else {
                    tooltip = Collections.emptyList();
                }
            } else if (!fuelInfo.isEmpty()) {
                FluidStack fluid = fuelInfo.getFluid();
                tooltip = FluidTooltipHandler.getFluidTooltip(fluid, fuelInfo.getTotalAmount());
                int temperature = fuelInfo.getTemperature();
                if (temperature > 0) {
                    tooltip.add(1, Text.translatable(TOOLTIP_TEMPERATURE, temperature).formatted(Formatting.GRAY, Formatting.ITALIC));
                } else {
                    tooltip.add(1, TOOLTIP_INVALID_FUEL);
                }
            } else {
                tooltip = hasTank ? TOOLTIP_NO_FUEL : TOOLTIP_NO_TANK;
            }

            screen.renderComponentTooltip(context, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Gets the fluid stack under the mouse
     *
     * @param checkX Mouse X position
     * @param checkY Mouse Y position
     * @return Fluid stack under mouse
     */
    @Nullable
    public FluidStack getIngredient(int checkX, int checkY) {
        if (!hasFuelSlot && isHovered(checkX, checkY) && !this.fuelInfo.isEmpty()) {
            return this.fuelInfo.getFluid();
        }
        return null;
    }
}
