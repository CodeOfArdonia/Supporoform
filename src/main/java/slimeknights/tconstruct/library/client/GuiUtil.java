package slimeknights.tconstruct.library.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuiUtil {
    /**
     * Draws the background of a container
     *
     * @param context    Matrix context
     * @param screen     Parent screen
     * @param background Background location
     */
    public static void drawBackground(DrawContext context, HandledScreen<?> screen, Identifier background) {
        RenderUtils.setup(background);
        context.fill(screen.x, screen.y, 0, 0, screen.backgroundWidth, screen.backgroundHeight);
    }

    /**
     * Checks if the given area is hovered
     *
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param x      Tank X position
     * @param y      Tank Y position
     * @param width  Tank width
     * @param height Tank height
     * @return True if the area is hovered
     */
    public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    /**
     * Checks if the given tank area is hovered
     *
     * @param mouseX   Mouse X position
     * @param mouseY   Mouse Y position
     * @param amount   Current tank amount
     * @param capacity Tank capacity
     * @param x        Tank X position
     * @param y        Tank Y position
     * @param width    Tank width
     * @param height   Tank height
     * @return True if the tank is hovered, false otherwise
     */
    public static boolean isTankHovered(int mouseX, int mouseY, int amount, int capacity, int x, int y, int width, int height) {
        // check X position first, its easier
        if (mouseX < x || mouseX > x + width || mouseY > y + height) {
            return false;
        }
        // next, try height
        int topHeight = height - (height * amount / capacity);
        return mouseY > y + topHeight;
    }

    /**
     * Renders a fluid tank with a partial fluid level
     *
     * @param screen   Parent screen
     * @param stack    Fluid stack
     * @param capacity Tank capacity, determines height
     * @param x        Tank X position
     * @param y        Tank Y position
     * @param width    Tank width
     * @param height   Tank height
     * @param depth    Tank depth
     */
    public static void renderFluidTank(DrawContext context, HandledScreen<?> screen, FluidStack stack, int capacity, int x, int y, int width, int height, int depth) {
        renderFluidTank(context, screen, stack, stack.getAmount(), capacity, x, y, width, height, depth);
    }

    /**
     * Renders a fluid tank with a partial fluid level and an amount override
     *
     * @param screen   Parent screen
     * @param stack    Fluid stack
     * @param capacity Tank capacity, determines height
     * @param x        Tank X position
     * @param y        Tank Y position
     * @param width    Tank width
     * @param height   Tank height
     * @param depth    Tank depth
     */
    public static void renderFluidTank(DrawContext context, HandledScreen<?> screen, FluidStack stack, long amount, int capacity, int x, int y, int width, int height, int depth) {
        if (!stack.isEmpty() && capacity > 0) {
            int maxY = y + height;
            long fluidHeight = Math.min(height * amount / capacity, height);
            renderTiledFluid(context, screen, stack, x, maxY - fluidHeight, width, fluidHeight, depth);
        }
    }

    /**
     * Colors and renders a fluid sprite
     *
     * @param context Matrix instance
     * @param screen   Parent screen
     * @param stack    Fluid stack
     * @param x        Fluid X
     * @param y        Fluid Y
     * @param width    Fluid width
     * @param height   Fluid height
     * @param depth    Fluid depth
     */
    public static void renderTiledFluid(DrawContext context, HandledScreen<?> screen, FluidStack stack, int x, long y, int width, long height, int depth) {
        if (!stack.isEmpty()) {
            IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(stack.getFluid());
            Sprite fluidSprite = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(clientFluid.getStillTexture(stack));
            RenderUtils.setColorRGBA(clientFluid.getTintColor(stack));
            renderTiledTextureAtlas(context, screen, fluidSprite, x, y, width, height, depth, stack.getFluid().getFluidType().isLighterThanAir());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    /**
     * Renders a texture atlas sprite tiled over the given area
     *
     * @param context   Matrix instance
     * @param screen     Parent screen
     * @param sprite     Sprite to render
     * @param x          X position to render
     * @param y          Y position to render
     * @param width      Render width
     * @param height     Render height
     * @param depth      Render depth
     * @param upsideDown If true, flips the sprite
     */
    public static void renderTiledTextureAtlas(DrawContext context, HandledScreen<?> screen, Sprite sprite, int x, int y, int width, int height, int depth, boolean upsideDown) {
        // start drawing sprites
        RenderUtils.bindTexture(sprite.getAtlasId());
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        // tile vertically
        float u1 = sprite.getMinU();
        float v1 = sprite.getMinV();
        int spriteHeight = sprite.getHeight();
        int spriteWidth = sprite.getWidth();
        int startX = x + screen.x;
        int startY = y + screen.y;
        do {
            int renderHeight = Math.min(spriteHeight, height);
            height -= renderHeight;
            float v2 = sprite.getFrameV((16f * renderHeight) / spriteHeight);

            // we need to draw the quads per width too
            int x2 = startX;
            int widthLeft = width;
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            // tile horizontally
            do {
                int renderWidth = Math.min(spriteWidth, widthLeft);
                widthLeft -= renderWidth;

                float u2 = sprite.getFrameU((16f * renderWidth) / spriteWidth);
                if (upsideDown) {
                    // FIXME: I think this causes tiling errors, look into it
                    buildSquare(matrix, builder, x2, x2 + renderWidth, startY, startY + renderHeight, depth, u1, u2, v2, v1);
                } else {
                    buildSquare(matrix, builder, x2, x2 + renderWidth, startY, startY + renderHeight, depth, u1, u2, v1, v2);
                }
                x2 += renderWidth;
            } while (widthLeft > 0);

            startY += renderHeight;
        } while (height > 0);

        // finish drawing sprites
        BufferRenderer.drawWithGlobalProgram(builder.end());
        // RenderSystem.enableAlphaTest();
        RenderSystem.enableDepthTest();
    }

    /**
     * Adds a square of texture to a buffer builder
     *
     * @param builder Builder instance
     * @param x1      X start
     * @param x2      X end
     * @param y1      Y start
     * @param y2      Y end
     * @param z       Depth
     * @param u1      Texture U start
     * @param u2      Texture U end
     * @param v1      Texture V start
     * @param v2      Texture V end
     */
    private static void buildSquare(Matrix4f matrix, BufferBuilder builder, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2) {
        builder.vertex(matrix, x1, y2, z).texture(u1, v2).next();
        builder.vertex(matrix, x2, y2, z).texture(u2, v2).next();
        builder.vertex(matrix, x2, y1, z).texture(u2, v1).next();
        builder.vertex(matrix, x1, y1, z).texture(u1, v1).next();
    }

    /**
     * Draws an upwards progress bar
     *
     * @param element  Element to draw
     * @param x        X position to start
     * @param y        Y position to start
     * @param progress Progress between 0 and 1
     */
    public static void drawProgressUp(DrawContext context, ElementScreen element, int x, int y, float progress) {
        int height;
        if (progress > 1) {
            height = element.h;
        } else if (progress < 0) {
            height = 0;
        } else {
            // add an extra 0.5 so it rounds instead of flooring
            height = (int) (progress * element.h + 0.5);
        }
        // amount to offset element by for the height
        int deltaY = element.h - height;
        context.blit(context, x, y + deltaY, element.x, element.y + deltaY, element.w, height, element.texW, element.texH);
    }

    /**
     * Renders a highlight overlay for the given area
     *
     * @param context Matrix instance
     * @param x       Element X position
     * @param y       Element Y position
     * @param width   Element width
     * @param height  Element height
     */
    public static void renderHighlight(DrawContext context, int x, int y, int width, int height) {
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        context.fill(x, y, x + width, y + height, 0x80FFFFFF);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }

    /**
     * Renders a pattern at the given location
     */
    public static void renderPattern(DrawContext context, Pattern pattern, int x, int y) {
        Sprite sprite = MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).getSprite(pattern.getTexture());
        context.drawSprite(x, y, 100, 16, 16, sprite);
    }
}
