package slimeknights.tconstruct.library.client.book.elements;

import net.minecraft.client.gui.DrawContext;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.ArrowButton;
import slimeknights.mantle.client.screen.book.element.ArrowElement;
import slimeknights.mantle.client.screen.book.element.BookElement;
import slimeknights.tconstruct.library.client.book.content.ContentModifier;

import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class CycleRecipeElement extends ArrowElement {

    public CycleRecipeElement(int x, int y, ArrowButton.ArrowType arrowType, int arrowColor, int arrowColorHover, ContentModifier modifier, BookData book, ArrayList<BookElement> list) {
        super(x, y, arrowType, arrowColor, arrowColorHover, (button) -> modifier.nextRecipe(book, list));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.button != null && this.isHovered(mouseX, mouseY)) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            this.button.onPress();
        }
    }

    public void playDownSound(SoundManager handler) {
        handler.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void drawOverlay(DrawContext context, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        if (this.isHovered(mouseX, mouseY)) {
            this.drawTooltip(context, Collections.singletonList(Text.translatable("gui.tconstruct.manual.cycle.recipes")), mouseX, mouseY, fontRenderer);
        }
    }
}
