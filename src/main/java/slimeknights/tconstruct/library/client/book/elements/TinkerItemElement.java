package slimeknights.tconstruct.library.client.book.elements;

import net.minecraft.client.gui.DrawContext;
import slimeknights.mantle.client.screen.book.element.ItemElement;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collection;

/**
 * Same as ElementItem, but uses the vanilla fontrenderer if none other is given
 */
public class TinkerItemElement extends ItemElement {

    public boolean noTooltip = false;

    public TinkerItemElement(ItemStack item) {
        this(0, 0, 1, item);
    }

    public TinkerItemElement(int x, int y, float scale, Item item) {
        super(x, y, scale, item);
    }

    public TinkerItemElement(int x, int y, float scale, Block item) {
        super(x, y, scale, item);
    }

    public TinkerItemElement(int x, int y, float scale, ItemStack item) {
        super(x, y, scale, item);
    }

    public TinkerItemElement(int x, int y, float scale, Collection<ItemStack> itemCycle) {
        super(x, y, scale, itemCycle);
    }

    public TinkerItemElement(int x, int y, float scale, Collection<ItemStack> itemCycle, String action) {
        super(x, y, scale, itemCycle, action);
    }

    public TinkerItemElement(int x, int y, float scale, ItemStack... itemCycle) {
        super(x, y, scale, itemCycle);
    }

    public TinkerItemElement(int x, int y, float scale, ItemStack[] itemCycle, @Nullable String action) {
        super(x, y, scale, itemCycle, action);
    }

    @Override
    public void drawOverlay(DrawContext context, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        if (this.noTooltip) {
            return;
        }

        if (this.tooltip == null) {
            fontRenderer = mc.textRenderer;
        }

        super.drawOverlay(context, mouseX, mouseY, partialTicks, fontRenderer);
    }
}
