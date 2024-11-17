package slimeknights.mantle.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Represents a GUI element INSIDE the graphics file.
 * The coordinates all refer to the coordinates inside the graphics!
 */
public class ElementScreen {
    // TODO: can this be final?
    public Identifier texture;
    public final int x;
    public final int y;
    public final int w;
    public final int h;

    public final int texW;
    public final int texH;

    public ElementScreen(int x, int y, int w, int h) {
        this(x, y, w, h, 256, 256);
    }

    public ElementScreen(int x, int y, int w, int h, int texW, int texH) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.texW = texW;
        this.texH = texH;
    }

    public ElementScreen(Identifier texture, int x, int y, int w, int h, int texW, int texH) {
        this(x, y, w, h, texW, texH);
        this.texture = texture;
    }

    /**
     * Creates a new element from this texture with the X, Y, width, and height
     */
    public ElementScreen move(int x, int y, int width, int height) {
        return new ElementScreen(this.texture, x, y, width, height, this.texW, this.texH);
    }

    /**
     * Creates a new element by offsetting this element by the given amount
     */
    public ElementScreen shift(int xd, int yd) {
        return this.move(this.x + xd, this.y + yd, this.w, this.h);
    }

    /**
     * Draws the element at the given x/y coordinates
     *
     * @param xPos X-Coordinate on the screen
     * @param yPos Y-Coordinate on the screen
     */
    public void draw(DrawContext graphics, int xPos, int yPos) {
        graphics.drawTexture(this.texture, xPos, yPos, this.x, this.y, this.w, this.h, this.texW, this.texH);
    }
}
