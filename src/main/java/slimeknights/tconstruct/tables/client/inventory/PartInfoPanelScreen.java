package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;

import java.util.ListIterator;

public class PartInfoPanelScreen extends InfoPanelScreen {
    private static final String COST_KEY = TConstruct.makeTranslationKey("gui", "part_builder.cost");
    private static final String MATERIAL_VALUE_KEY = TConstruct.makeTranslationKey("gui", "part_builder.material_value");

    private Text patternCost;
    private Text materialValue;

    public PartInfoPanelScreen(MultiModuleScreen parent, ScreenHandler container, PlayerInventory playerInventory, Text title) {
        super(parent, container, playerInventory, title);
        this.patternCost = Text.empty();
        this.materialValue = Text.empty();
    }

    /* Pattern cost */

    /**
     * Clears the pattern cost text
     */
    public void clearPatternCost() {
        this.patternCost = Text.empty();
        this.updateSliderParameters();
    }

    /**
     * Sets the pattern cost
     *
     * @param cost Pattern cost
     */
    public void setPatternCost(int cost) {
        this.patternCost = Text.translatable(COST_KEY, cost).formatted(Formatting.GOLD);
        this.updateSliderParameters();
    }

    /**
     * If true, has pattern cost text
     */
    private boolean hasPatternCost() {
        return this.patternCost != null && this.patternCost != Text.empty();
    }

    /* Material value */

    /**
     * Sets the material value
     *
     * @param value Value text
     */
    public void setMaterialValue(Text value) {
        this.materialValue = Text.translatable(MATERIAL_VALUE_KEY, value).styled(style -> style.withColor(TextColor.fromRgb(0x7fffff)));
        this.updateSliderParameters();
    }

    /**
     * Clears the material value
     */
    public void clearMaterialValue() {
        this.materialValue = Text.empty();
        this.updateSliderParameters();
    }

    /**
     * If true, has material value text
     */
    private boolean hasMaterialValue() {
        return this.materialValue != null && this.materialValue != Text.empty();
    }

    @Override
    public int calcNeededHeight() {
        int neededHeight = 0;

        if (!this.hasInitialized()) {
            return height;
        }

        int scaledFontHeight = this.getScaledFontHeight();
        if (this.hasCaption()) {
            neededHeight += scaledFontHeight + 3;
        }

        if (this.hasPatternCost()) {
            neededHeight += scaledFontHeight + 3;
        }

        if (this.hasMaterialValue()) {
            neededHeight += scaledFontHeight + 3;
        }

        neededHeight += (scaledFontHeight + 0.5f) * this.getTotalLines().size();

        return neededHeight;
    }

    @Override
    protected float getTooltipStart(float y) {
        y = super.getTooltipStart(y);
        int scaledFontHeight = this.getScaledFontHeight();
        if (this.hasPatternCost()) {
            y += scaledFontHeight + 3;
        }
        if (this.hasMaterialValue()) {
            y += scaledFontHeight + 3;
        }
        return y;
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        RenderUtils.setup(BACKGROUND_IMAGE);

        this.border.draw(context);
        BACKGROUND.drawScaled(context, this.x + 4, this.y + 4, this.backgroundWidth - 8, this.backgroundHeight - 8);

        int y = 5 + this.y;
        int x = 5 + this.x;
        int color = 0xfff0f0f0;

        // info ? in the top right corner
        if (this.hasTooltips()) {
            context.drawText(this.textRenderer, "?", guiRight() - this.border.w - this.textRenderer.getWidth("?") / 2, this.y + 5, 0xff5f5f5f, false);
        }

        int scaledFontHeight = this.getScaledFontHeight();
        if (this.hasCaption()) {
            int x2 = this.backgroundWidth / 2;
            x2 -= this.textRenderer.getWidth(this.caption) / 2;

            context.drawTextWithShadow(this.textRenderer, this.caption.asOrderedText(), this.x + x2, y, color);
            y += scaledFontHeight + 3;
        }

        // Draw pattern cost
        if (this.hasPatternCost()) {
            int x2 = this.backgroundWidth / 2;
            x2 -= this.textRenderer.getWidth(this.patternCost) / 2;

            context.drawTextWithShadow(this.textRenderer, this.patternCost.asOrderedText(), this.x + x2, y, color);
            y += scaledFontHeight + 3;
        }

        // Draw material value
        if (this.hasMaterialValue()) {
            int x2 = this.backgroundWidth / 2;
            x2 -= this.textRenderer.getWidth(this.materialValue) / 2;

            context.drawTextWithShadow(this.textRenderer, this.materialValue.asOrderedText(), this.x + x2, y, color);
            y += scaledFontHeight + 3;
        }

        if (this.text == null || this.text.isEmpty()) {
            // no text to draw
            return;
        }

        float textHeight = textRenderer.fontHeight + 0.5f;
        float lowerBound = (this.y + this.backgroundHeight - 5) / this.textScale;
        //RenderSystem.scalef(this.textScale, this.textScale, 1.0f);
        context.getMatrices().push();
        context.getMatrices().scale(this.textScale, this.textScale, 1.0f);
        x /= this.textScale;
        y /= this.textScale;

        // render shown lines
        ListIterator<OrderedText> iter = this.getTotalLines().listIterator(this.slider.getValue());
        while (iter.hasNext()) {
            if (y + textHeight - 0.5f > lowerBound) {
                break;
            }

            OrderedText line = iter.next();
            context.drawTextWithShadow(this.textRenderer, line, x, y, color);
            y += textHeight;
        }

        context.getMatrices().pop();
        //RenderSystem.scalef(1f / textScale, 1f / textScale, 1.0f);

        //this.minecraft.getTextureManager().bind(BACKGROUND_IMAGE);
        RenderUtils.setup(BACKGROUND_IMAGE);
        this.slider.update(mouseX, mouseY);
        this.slider.draw(context);
    }
}
