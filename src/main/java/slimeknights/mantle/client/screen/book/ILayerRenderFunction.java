package slimeknights.mantle.client.screen.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import slimeknights.mantle.client.screen.book.element.BookElement;

public interface ILayerRenderFunction {
    void draw(BookElement element, DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer);
}
