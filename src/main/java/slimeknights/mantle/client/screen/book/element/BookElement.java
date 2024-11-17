package slimeknights.mantle.client.screen.book.element;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;
import slimeknights.mantle.client.screen.book.BookScreen;

import java.util.List;
import java.util.Optional;

public abstract class BookElement {

    public BookScreen parent;

    protected MinecraftClient mc = MinecraftClient.getInstance();
    protected TextureManager renderEngine = this.mc.getTextureManager();

    public int x, y;

    public BookElement(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void draw(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer);

    public void drawOverlay(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
    }

    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {

    }

    public void mouseReleased(double mouseX, double mouseY, int clickedMouseButton) {

    }

    public void mouseDragged(double clickX, double clickY, double mx, double my, double lastX, double lastY, int button) {

    }

    public void renderToolTip(DrawContext graphics, TextRenderer fontRenderer, ItemStack stack, int x, int y) {
        List<Text> list = stack.getTooltip(this.mc.player, this.mc.options.advancedItemTooltips ? TooltipContext.Default.ADVANCED : TooltipContext.Default.BASIC);

        TextRenderer font = IClientItemExtensions.of(stack).getFont(stack, FontContext.TOOLTIP);
        if (font == null) {
            font = fontRenderer;
        }

        this.drawTooltip(graphics, list, x, y, font);
    }

    public void drawTooltip(DrawContext graphics, List<Text> textLines, int x, int y, TextRenderer font) {
        // GuiUtils.drawHoveringText(matrixStack, textLines, x, y, this.parent.width, this.parent.height, -1, font);
        // GuiUtils.drawHoveringText(matrixStack, textLines, x, y, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT, BookScreen.PAGE_WIDTH, font);
        int oldWidth = this.parent.width;
        int oldHeight = this.parent.height;
        this.parent.width = BookScreen.PAGE_WIDTH;
        this.parent.height = BookScreen.PAGE_HEIGHT;
        graphics.drawTooltip(font, textLines, Optional.empty(), x, y);
        this.parent.width = oldWidth;
        this.parent.height = oldHeight;
    }
}
