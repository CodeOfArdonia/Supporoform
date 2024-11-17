package slimeknights.mantle.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

// a sub-gui. Mostly the same as a separate ContainerScreen, but doesn't do the calls that affect the game as if this were the only gui
public abstract class ModuleScreen<P extends MultiModuleScreen<?>, C extends ScreenHandler> extends HandledScreen<C> {

    protected final P parent;

    // left or right of the parent
    protected final boolean right;
    // top or bottom of the parent
    protected final boolean bottom;

    public int yOffset = 0;
    public int xOffset = 0;

    public ModuleScreen(P parent, C container, PlayerInventory playerInventory, Text title, boolean right, boolean bottom) {
        super(container, playerInventory, title);

        this.parent = parent;
        this.right = right;
        this.bottom = bottom;
    }

    public int guiRight() {
        return this.x + this.backgroundWidth;
    }

    public int guiBottom() {
        return this.y + this.backgroundHeight;
    }

    public Rect2i getArea() {
        return new Rect2i(this.x, this.y, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void init() {
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
    }

    public void updatePosition(int parentX, int parentY, int parentSizeX, int parentSizeY) {
        if (this.right) {
            this.x = parentX + parentSizeX;
        } else {
            this.x = parentX - this.backgroundWidth;
        }

        if (this.bottom) {
            this.y = parentY + parentSizeY - this.backgroundHeight;
        } else {
            this.y = parentY;
        }

        this.x += this.xOffset;
        this.y += this.yOffset;
    }

    public boolean shouldDrawSlot(Slot slot) {
        return true;
    }

    public boolean isMouseInModule(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.guiRight() && mouseY >= this.y && mouseY < this.guiBottom();
    }

    public boolean isMouseOverFullSlot(double mouseX, double mouseY) {
        for (Slot slot : this.handler.slots) {
            if (this.parent.isPointOverSlot(slot, mouseX, mouseY) && slot.hasStack()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Callback to draw background elements
     */
    public void handleDrawGuiContainerBackgroundLayer(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
        this.drawBackground(graphics, partialTicks, mouseX, mouseY);
    }

    /**
     * Callback to draw foreground elements
     */
    public void handleDrawGuiContainerForegroundLayer(DrawContext graphics, int mouseX, int mouseY) {
        this.drawForeground(graphics, mouseX, mouseY);
    }

    /**
     * Callback to draw hovering tooltips
     */
    public void handleRenderHoveredTooltip(DrawContext graphics, int mouseX, int mouseY) {
        this.drawMouseoverTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Custom mouse click handling.
     *
     * @return True to prevent the main container handling the mouseclick
     */
    public boolean handleMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Custom mouse click handling.
     *
     * @return True to prevent the main container handling the mouseclick
     */
    public boolean handleMouseClickMove(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick) {
        return false;
    }

    /**
     * Custom mouse click handling.
     *
     * @return True to prevent the main container handling the mouseclick
     */
    public boolean handleMouseReleased(double mouseX, double mouseY, int state) {
        return false;
    }

    /**
     * Custom mouse scrolled handling.
     *
     * @return True to prevent the main container handling the mouseclick
     */
    public boolean handleMouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }
}
