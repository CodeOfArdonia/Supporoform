package slimeknights.mantle.client.screen.book;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.book.data.BookData;

import static slimeknights.mantle.client.screen.book.Textures.TEX_BOOK;

public class ArrowButton extends ButtonWidget {

    public static final int WIDTH = 18;
    public static final int HEIGHT = 10;

    // Appearance
    @Nullable
    private final BookData bookData;
    public ArrowType arrowType;
    public int color;
    public int hoverColor;

    public ArrowButton(@Nullable BookData bookData, int x, int y, ArrowType arrowType, int color, int hoverColor, PressAction onPress) {
        super(x, y, arrowType.w, arrowType.h, Text.empty(), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);

        this.arrowType = arrowType;
        this.color = color;
        this.hoverColor = hoverColor;
        this.bookData = bookData;
    }

    public ArrowButton(int x, int y, ArrowType arrowType, int color, int hoverColor, PressAction onPress) {
        this(null, x, y, arrowType, color, hoverColor, onPress);
    }

    /**
     * Shared logic between public method and vanilla method
     */
    private void renderButton(DrawContext graphics, @Nullable BookData bookData) {
        Identifier texture;
        if (bookData != null) {
            texture = bookData.appearance.getBookTexture();
        } else {
            texture = TEX_BOOK;
        }

        int color = this.hovered ? this.hoverColor : this.color;

        float r = ((color >> 16) & 0xff) / 255.F;
        float g = ((color >> 8) & 0xff) / 255.F;
        float b = (color & 0xff) / 255.F;

        graphics.setShaderColor(r, g, b, 1f);
        graphics.drawTexture(texture, this.getX(), this.getY(), this.width, this.height, this.arrowType.x, this.arrowType.y, this.width, this.height, 512, 512);
        graphics.setShaderColor(1, 1, 1, 1);
//    this.renderBg(graphics, mouseX, mouseY, partialTicks);
    }

    /**
     * Public method to swap out book data on rendering
     */
    public void renderButton(DrawContext graphics, int mouseX, int mouseY, float pPartialTick, @Nullable BookData bookData) {
        this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        this.renderButton(graphics, bookData);
    }

    @Override
    protected void renderButton(DrawContext graphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderButton(graphics, this.bookData);
    }

    public enum ArrowType {
        NEXT(412, 0),
        PREV(412, 10),
        RIGHT(412, 20),
        LEFT(412, 30),
        BACK_UP(412, 40, 18, 18),
        UP(412, 58, 10, 18),
        DOWN(412 + 10, 58, 10, 18),
        REFRESH(412, 76, 18, 18);

        public final int x, y, w, h;

        ArrowType(int x, int y) {
            this(x, y, WIDTH, HEIGHT);
        }

        ArrowType(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
