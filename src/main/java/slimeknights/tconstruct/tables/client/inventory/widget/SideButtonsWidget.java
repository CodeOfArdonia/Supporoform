package slimeknights.tconstruct.tables.client.inventory.widget;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.Rect2i;
import org.apache.commons.compress.utils.Lists;
import slimeknights.mantle.client.screen.MultiModuleScreen;

import java.util.List;

public class SideButtonsWidget<T extends ButtonWidget> implements Drawable, Element {

    private static final int SPACING = 4;

    protected final MultiModuleScreen<?> parent;

    @Getter
    private final int leftPos;
    private final int topPos;
    @Getter
    private final int imageWidth;
    private final int imageHeight;

    private final int columns;
    protected final List<T> buttons = Lists.newArrayList();
    private ButtonWidget clickedButton;
    private boolean focused = false;

    public SideButtonsWidget(MultiModuleScreen<?> parent, int leftPos, int topPos, int columns, int rows, int buttonWidth, int buttonHeight) {
        this.parent = parent;

        this.leftPos = leftPos;
        this.topPos = topPos;
        this.columns = columns;

        this.imageWidth = size(columns, buttonWidth);
        this.imageHeight = size(rows, buttonHeight);
    }

    public int guiRight() {
        return this.leftPos + this.imageWidth;
    }

    public int guiBottom() {
        return this.topPos + this.imageHeight;
    }

    public Rect2i getArea() {
        return new Rect2i(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    public void setButtonPositions() {
        for (int i = 0; i < this.buttons.size(); i++) {
            T button = this.buttons.get(i);
            int x = (i % this.columns) * (button.getWidth() + SPACING);
            int y = (i / this.columns) * (button.getHeight() + SPACING);
            button.setPosition(this.leftPos + x, this.topPos + y);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.leftPos <= mouseX && mouseX < this.guiRight() && this.topPos <= mouseY && mouseY < this.guiBottom();
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            for (T button : this.buttons) {
                if (button.mouseClicked(mouseX, mouseY, mouseButton)) {
                    this.clickedButton = button;
                    return true;
                }
            }
        }

        return false;
    }

    public boolean handleMouseReleased(double mouseX, double mouseY, int state) {
        if (this.clickedButton != null) {
            this.clickedButton.mouseReleased(mouseX, mouseY, state);
            this.clickedButton = null;
            return true;
        }

        return false;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        for (T button : this.buttons) {
            button.render(context, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Calculates the number of rows of buttons when fitting the given number of buttons within the given number of columns.
     */
    public static int rowsForCount(int columns, int count) {
        return (count - 1) / columns + 1;
    }

    /**
     * Calculates the width or height of this widget given the width or height of a button and the number of buttons along the same axis.
     */
    public static int size(int buttonCount, int buttonSize) {
        return buttonSize * buttonCount + SPACING * (buttonCount - 1);
    }
}
