package slimeknights.mantle.client.screen.book.element;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class TooltipElement extends SizedBookElement {

    private final List<Text> tooltips;

    public TooltipElement(List<Text> tooltip, int x, int y, int width, int height) {
        super(x, y, width, height);

        this.tooltips = tooltip;
    }

    @Override
    public void draw(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
    }

    @Override
    public void drawOverlay(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        if (this.isHovered(mouseX, mouseY)) {
            graphics.drawTooltip(fontRenderer, this.tooltips, Optional.empty(), mouseX, mouseY);
        }
    }
}
