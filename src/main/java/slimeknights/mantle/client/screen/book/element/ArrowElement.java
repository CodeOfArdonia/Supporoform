package slimeknights.mantle.client.screen.book.element;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import slimeknights.mantle.client.screen.book.ArrowButton;

public class ArrowElement extends ButtonElement {

    protected final ArrowButton button;

    public ArrowElement(int x, int y, ArrowButton.ArrowType arrowType, int arrowColor, int arrowColorHover, PressAction iPressable) {
        super(x, y, arrowType.w, arrowType.h);
        // pass in book data during draw
        this.button = new ArrowButton(null, x, y, arrowType, arrowColor, arrowColorHover, iPressable);
    }

    @Override
    public void draw(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        this.button.renderButton(graphics, mouseX, mouseY, partialTicks, this.parent.book);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.button != null && this.isHovered(mouseX, mouseY)) {
            this.button.onPress();
        }
    }

}
