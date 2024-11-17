package slimeknights.mantle.client.screen.book.element;

import net.minecraft.block.Block;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.book.action.StringActionProcessor;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class ItemElement extends SizedBookElement {

    public static final int ITEM_SIZE_HARDCODED = 16;
    public static final long ITEM_SWITCH_TIME = 3000000000L; // 3 seconds

    public DefaultedList<ItemStack> itemCycle;
    public float scale;
    @Nullable
    public String action;
    public List<Text> tooltip;

    public long lastTime;
    public int currentItem = 0;

    public ItemElement(int x, int y, float scale, Item item) {
        this(x, y, scale, new ItemStack(item));
    }

    public ItemElement(int x, int y, float scale, Block item) {
        this(x, y, scale, new ItemStack(item));
    }

    public ItemElement(int x, int y, float scale, ItemStack item) {
        this(x, y, scale, new ItemStack[]{item});
    }

    public ItemElement(int x, int y, float scale, Collection<ItemStack> itemCycle) {
        this(x, y, scale, itemCycle.toArray(new ItemStack[0]));
    }

    public ItemElement(int x, int y, float scale, Collection<ItemStack> itemCycle, String action) {
        this(x, y, scale, itemCycle.toArray(new ItemStack[0]), action);
    }

    public ItemElement(int x, int y, float scale, ItemStack... itemCycle) {
        this(x, y, scale, itemCycle, null);
    }

    public ItemElement(int x, int y, float scale, ItemStack[] itemCycle, @Nullable String action) {
        super(x, y, MathHelper.floor(ITEM_SIZE_HARDCODED * scale), MathHelper.floor(ITEM_SIZE_HARDCODED * scale));

        this.lastTime = Util.getMeasuringTimeNano();

        DefaultedList<ItemStack> nonNullStacks = DefaultedList.ofSize(itemCycle.length, ItemStack.EMPTY);
        for (int i = 0; i < itemCycle.length; i++) {
            if (!itemCycle[i].isEmpty()) {
                nonNullStacks.set(i, itemCycle[i].copy());
            }
        }

        this.itemCycle = nonNullStacks;
        this.scale = scale;
        this.action = action;
    }

    @Override
    public void draw(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        long nano = Util.getMeasuringTimeNano();

        if (nano > this.lastTime + ITEM_SWITCH_TIME) {
            this.lastTime = nano;
            this.currentItem++;

            if (this.currentItem >= this.itemCycle.size()) {
                this.currentItem = 0;
            }
        }

        if (this.currentItem < this.itemCycle.size()) {
            // Lighting.turnBackOn(); TODO: still needed?

            MatrixStack matrices = graphics.getMatrices();
            matrices.push();
            matrices.translate(this.x, this.y, 0);
            matrices.scale(this.scale, this.scale, 1.0F);

            ItemStack stack = this.itemCycle.get(this.currentItem);
            graphics.drawItem(stack, 0, 0);
            TextRenderer font = IClientItemExtensions.of(stack).getFont(stack, FontContext.TOOLTIP);
            if (font == null) font = this.mc.textRenderer;
            graphics.drawItemInSlot(font, stack, 0, 0);

            matrices.pop();
        }
    }

    @Override
    public void drawOverlay(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        if (this.isHovered(mouseX, mouseY) && this.currentItem < this.itemCycle.size()) {
            if (this.tooltip != null) {
                this.drawTooltip(graphics, this.tooltip, mouseX, mouseY, fontRenderer);
            } else {
                this.renderToolTip(graphics, fontRenderer, this.itemCycle.get(this.currentItem), mouseX, mouseY);
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovered(mouseX, mouseY) && this.currentItem < this.itemCycle.size()) {
            if (!StringHelper.isEmpty(this.action)) {
                StringActionProcessor.process(this.action, this.parent);
            }
        }
    }
}
