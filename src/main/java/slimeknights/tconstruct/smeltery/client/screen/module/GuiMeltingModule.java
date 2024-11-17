package slimeknights.tconstruct.smeltery.client.screen.module;

import lombok.AllArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@AllArgsConstructor
public class GuiMeltingModule {
    // progress bars
    private static final ScalableElementScreen PROGRESS_BAR = new ScalableElementScreen(176, 150, 3, 16, 256, 256);
    private static final ScalableElementScreen NO_HEAT_BAR = new ScalableElementScreen(179, 150, 3, 16, 256, 256);
    private static final ScalableElementScreen NO_SPACE_BAR = new ScalableElementScreen(182, 150, 3, 16, 256, 256);
    private static final ScalableElementScreen UNMELTABLE_BAR = new ScalableElementScreen(185, 150, 3, 16, 256, 256);

    // progress bar tooltips
    private static final Text TOOLTIP_NO_HEAT = Text.translatable(TConstruct.makeTranslationKey("gui", "melting.no_heat"));
    private static final Text TOOLTIP_NO_SPACE = Text.translatable(TConstruct.makeTranslationKey("gui", "melting.no_space"));
    private static final Text TOOLTIP_UNMELTABLE = Text.translatable(TConstruct.makeTranslationKey("gui", "melting.no_recipe"));

    private final HandledScreen<?> screen;
    private final MeltingModuleInventory inventory;
    private final IntSupplier temperature;
    private final Predicate<Slot> slotPredicate;


    /**
     * Draws the heat bars on each slot
     */
    public void drawHeatBars(DrawContext context) {
        int temperature = this.temperature.getAsInt();
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            Slot slot = this.screen.getScreenHandler().slots.get(i);
            if (slot.hasStack() && this.slotPredicate.test(slot)) {
                // determine the bar to draw and the progress
                ScalableElementScreen bar = PROGRESS_BAR;

                int index = slot.getSlotIndex();
                int currentTemp = this.inventory.getCurrentTime(index);
                int requiredTime = this.inventory.getRequiredTime(index);

                // no required time means unmeltable
                float progress = 1f;
                if (requiredTime == 0) {
                    bar = UNMELTABLE_BAR;
                } else if (this.inventory.getRequiredTemp(index) > temperature) {
                    bar = NO_HEAT_BAR;
                }
                // -1 error state if no space
                else if (currentTemp < 0) {
                    bar = NO_SPACE_BAR;
                    progress = 1f;
                }
                // scale back normal progress if too large
                else if (currentTemp <= requiredTime) {
                    progress = currentTemp / (float) requiredTime;
                }

                // draw the bar
                GuiUtil.drawProgressUp(context, bar, slot.x - 4, slot.y, progress);
            }
        }
    }

    /**
     * Draws the tooltip for the hovered hear slot
     *
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    public void drawHeatTooltips(DrawContext context, int mouseX, int mouseY) {
        int checkX = mouseX - this.screen.x;
        int checkY = mouseY - this.screen.y;
        int temperature = this.temperature.getAsInt();
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            Slot slot = this.screen.getScreenHandler().slots.get(i);
            // must have a stack
            if (slot.hasStack() && this.slotPredicate.test(slot)) {
                // mouse must be within the slot
                if (GuiUtil.isHovered(checkX, checkY, slot.x - 5, slot.y - 1, PROGRESS_BAR.w + 1, PROGRESS_BAR.h + 2)) {
                    int index = slot.getSlotIndex();
                    Text tooltip = null;

                    // NaN means 0 progress for 0 need, unmeltable
                    if (this.inventory.getRequiredTime(index) == 0) {
                        tooltip = TOOLTIP_UNMELTABLE;
                    }
                    // -1 error state if temperature is too low
                    else if (this.inventory.getRequiredTemp(slot.getSlotIndex()) > temperature) {
                        tooltip = TOOLTIP_NO_HEAT;
                    }
                    // 2x error state if no space
                    else if (this.inventory.getCurrentTime(index) < 0) {
                        tooltip = TOOLTIP_NO_SPACE;
                    }

                    // draw tooltip if relevant
                    if (tooltip != null) {
                        context.drawTooltip(MinecraftClient.getInstance().textRenderer, List.of(tooltip), Optional.empty(), mouseX, mouseY);
                    }

                    // cannot hover two slots, so done
                    break;
                }
            }
        }
    }
}
