package slimeknights.tconstruct.library.client.book.elements;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import slimeknights.mantle.client.book.action.StringActionProcessor;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.element.TextData;
import slimeknights.mantle.client.screen.book.element.SizedBookElement;

public class PageIconLinkElement extends SizedBookElement {

    public PageData pageData;
    public SizedBookElement displayElement;
    public TextData link;
    public String action;
    public Text name;

    public PageIconLinkElement(int x, int y, SizedBookElement displayElement, Text name, PageData pageData) {
        this(x, y, displayElement.width, displayElement.height, displayElement, name, pageData);
    }

    public PageIconLinkElement(int x, int y, int w, int h, SizedBookElement displayElement, Text name, PageData pageData) {
        super(x, y, w, h);
        this.displayElement = displayElement;
        this.pageData = pageData;

        this.action = "mantle:go-to-page-rtn " + pageData.parent.name + "." + pageData.name;

        this.name = name;
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        boolean hover = this.isHovered(mouseX, mouseY);
        RenderSystem.setShaderColor(1F, 1F, 1F, hover ? 1F : 0.5F);

        if (this.isHovered(mouseX, mouseY)) {
            context.fill(this.x, this.y, this.x + this.width, this.y + this.height, this.parent.book.appearance.hoverColor | (0x77 << 24));
        }

        this.displayElement.draw(context, mouseX, mouseY, partialTicks, fontRenderer);
    }

    @Override
    public void drawOverlay(DrawContext context, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        if (this.name != null && !this.name.getString().isEmpty() && this.isHovered(mouseX, mouseY)) {
            this.drawTooltip(context, ImmutableList.of(this.name), mouseX, mouseY, fontRenderer);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.isHovered(mouseX, mouseY)) {
            StringActionProcessor.process(this.action, this.parent);
        }
    }
}
