package slimeknights.mantle.client.screen;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens.Provider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Generic container screen that simply draws the given background
 *
 * @param <T> Container type
 */
@SuppressWarnings("WeakerAccess")
public class BackgroundContainerScreen<T extends ScreenHandler> extends HandledScreen<T> {
    /**
     * Background drawn for this screen
     */
    protected final Identifier background;

    /**
     * Creates a new screen instance
     *
     * @param container  Container class
     * @param inventory  Player inventory
     * @param name       Container name
     * @param background Container background
     */
    public BackgroundContainerScreen(T container, PlayerInventory inventory, Text name, int height, Identifier background) {
        super(container, inventory, name);
        this.background = background;
        this.backgroundHeight = height;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.drawTexture(this.background, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @RequiredArgsConstructor(staticName = "of")
    public static class Factory<T extends ScreenHandler> implements Provider<T, BackgroundContainerScreen<T>> {
        private final Identifier background;
        private final int height;

        /**
         * Creates a factory from the container name
         *
         * @param height Screen height
         * @param name   Name of this container
         */
        public static <T extends ScreenHandler> Factory<T> ofName(int height, Identifier name) {
            return of(new Identifier(name.getNamespace(), String.format("textures/gui/%s.png", name.getPath())), height);
        }

        @Override
        public BackgroundContainerScreen<T> create(T menu, PlayerInventory inventory, Text title) {
            return new BackgroundContainerScreen<>(menu, inventory, title, this.height, this.background);
        }
    }
}
