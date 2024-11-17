package slimeknights.mantle.client.screen.book.element;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import slimeknights.mantle.client.book.action.StringActionProcessor;
import slimeknights.mantle.client.book.data.element.TextComponentData;
import slimeknights.mantle.client.screen.book.TextComponentDataRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TextComponentElement extends SizedBookElement {

    public TextComponentData[] text;
    private final List<Text> tooltip = new ArrayList<Text>();

    private transient String lastAction = "";

    public TextComponentElement(int x, int y, int width, int height, String text) {
        this(x, y, width, height, Text.literal(text));
    }

    public TextComponentElement(int x, int y, int width, int height, Text text) {
        this(x, y, width, height, new TextComponentData(text));
    }

    public TextComponentElement(int x, int y, int width, int height, Collection<TextComponentData> text) {
        this(x, y, width, height, text.toArray(new TextComponentData[0]));
    }

    public TextComponentElement(int x, int y, int width, int height, TextComponentData... text) {
        super(x, y, width, height);

        this.text = text;
    }

    @Override
    public void draw(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        this.lastAction = TextComponentDataRenderer.drawText(graphics, this.x, this.y, this.width, this.height, this.text, mouseX, mouseY, fontRenderer, this.tooltip);
    }

    @Override
    public void drawOverlay(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        if (this.tooltip.size() > 0) {
            this.drawTooltip(graphics, this.tooltip, mouseX, mouseY, fontRenderer);
            this.tooltip.clear();
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && !this.lastAction.isEmpty()) {
            StringActionProcessor.process(this.lastAction, this.parent);
        }
    }
}
