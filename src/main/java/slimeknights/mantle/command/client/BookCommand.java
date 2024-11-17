package slimeknights.mantle.command.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.command.MantleCommand;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A command for different book
 */
public class BookCommand {
    private static final String BOOK_NOT_FOUND = "command.mantle.book_test.not_found";

    private static final String EXPORT_SUCCESS = "command.mantle.book.export.success";
    private static final String EXPORT_FAIL = "command.mantle.book.export.error_generic";
    private static final String EXPORT_FAIL_IO = "command.mantle.book.export.error_io";

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(source -> source.hasPermissionLevel(MantleCommand.PERMISSION_GAME_COMMANDS) && source.getEntity() instanceof AbstractClientPlayerEntity)
                .then(CommandManager.literal("open")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(MantleClientCommand.REGISTERED_BOOKS)
                                .executes(BookCommand::openBook)))
                .then(CommandManager.literal("export_images")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(MantleClientCommand.REGISTERED_BOOKS)
                                .then(CommandManager.argument("scale", IntegerArgumentType.integer(1, 16))
                                        .executes(BookCommand::exportImagesWithScale))
                                .executes(BookCommand::exportImages)));
    }

    /**
     * Opens the specified book
     *
     * @param context Command context
     * @return Integer return
     */
    private static int openBook(CommandContext<ServerCommandSource> context) {
        Identifier book = IdentifierArgumentType.getIdentifier(context, "id");

        BookData bookData = BookLoader.getBook(book);
        if (bookData != null) {
            bookData.openGui(Text.literal("Book"), "", null, null);
        } else {
            bookNotFound(book);
            return 1;
        }

        return 0;
    }

    /**
     * Renders all images in the book to files at specified scale
     *
     * @param context Command context
     * @return Integer return
     */
    private static int exportImagesWithScale(CommandContext<ServerCommandSource> context) {
        Identifier book = IdentifierArgumentType.getIdentifier(context, "id");
        int scale = context.getArgument("scale", Integer.class);

        return doExportImages(book, scale);
    }

    /**
     * Renders all images in the book to files
     *
     * @param context Command context
     * @return Integer return
     */
    private static int exportImages(CommandContext<ServerCommandSource> context) {
        Identifier book = IdentifierArgumentType.getIdentifier(context, "id");

        return doExportImages(book, 1);
    }

    /**
     * Renders all images in the book to files
     *
     * @param book  Book to export
     * @param scale Scale to export at
     * @return Integer return
     */
    private static int doExportImages(Identifier book, int scale) {
        BookData bookData = BookLoader.getBook(book);

        Path gameDirectory = MinecraftClient.getInstance().runDirectory.toPath();
        Path screenshotDir = Paths.get(gameDirectory.toString(), ScreenshotRecorder.SCREENSHOTS_DIRECTORY, "mantle_book", book.getNamespace(), book.getPath());

        if (bookData != null) {
            if (!screenshotDir.toFile().mkdirs() && !screenshotDir.toFile().exists()) {
                throw new CommandException(Text.translatable(EXPORT_FAIL_IO));
            }

            int width = BookScreen.PAGE_WIDTH_UNSCALED * 2 * scale;
            int height = BookScreen.PAGE_HEIGHT_UNSCALED * scale;
            float zFar = 1000.0F + 10000.0F * 3;

            bookData.load();
            BookScreen screen = new BookScreen(Text.literal("Book"), bookData, "", null, null);
            screen.init(MinecraftClient.getInstance(), width / scale, height / scale);
            screen.drawArrows = false;
            screen.mouseInput = false;

            Matrix4f matrix = (new Matrix4f()).setOrtho(0.0F, width, height, 0.0F, 1000.0F, zFar);
            RenderSystem.setProjectionMatrix(matrix, VertexSorter.BY_Z);

            MatrixStack stack = RenderSystem.getModelViewStack();
            stack.push();
            stack.loadIdentity();
            stack.translate(0, 0, 1000F - zFar);
            stack.scale(scale, scale, 1);
            RenderSystem.applyModelViewMatrix();
            DiffuseLighting.enableGuiDepthLighting();

            Framebuffer target = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
            target.enableStencil();

            try {
                VertexConsumerProvider.Immediate buffer = VertexConsumerProvider.immediate(new BufferBuilder(2048));
                target.beginWrite(true);

                DrawContext gui = new DrawContext(MinecraftClient.getInstance(), buffer);

                do {
                    RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

                    screen.tick();

                    RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                            GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);

                    gui.getMatrices().push();
                    screen.render(gui, 0, 0, 0);
                    gui.draw();
                    gui.getMatrices().pop();

                    try (NativeImage image = takeScreenshot(target)) {
                        int page = screen.getPage_();
                        String pageFormat = page < 0 ? "cover" : "page_" + page;
                        Path path = Paths.get(screenshotDir.toString(), pageFormat + ".png");

                        if (page == -1) { // the cover is half the width
                            try (NativeImage scaled = new NativeImage(image.getFormat(), width / 2, height, false)) {
                                image.copyRect(scaled, image.getWidth() / 2 - width / 4, 0, 0, 0,
                                        width / 2, height, false, false);
                                scaled.writeTo(path);
                            } catch (Exception e) {
                                Mantle.logger.error("Failed to save screenshot", e);
                                throw new CommandException(Text.translatable(EXPORT_FAIL));
                            }
                        } else {
                            image.writeTo(path);
                        }
                    } catch (Exception e) {
                        Mantle.logger.error("Failed to save screenshot", e);
                        throw new CommandException(Text.translatable(EXPORT_FAIL));
                    }
                } while (screen.nextPage());
            } finally {
                stack.pop();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.defaultBlendFunc();
                target.endWrite();
                target.delete();
            }
        } else {
            bookNotFound(book);
            return 1;
        }

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            Text fileComponent = Text.literal(screenshotDir.toString()).formatted(Formatting.UNDERLINE)
                    .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshotDir.toAbsolutePath().toString())));
            player.sendMessage(Text.translatable(EXPORT_SUCCESS, fileComponent), false);
        }
        return 0;
    }

    /**
     * Duplicate of Screenshot#takeScreenshot, but with transparency
     */
    private static NativeImage takeScreenshot(Framebuffer pFramebuffer) {
        int i = pFramebuffer.textureWidth;
        int j = pFramebuffer.textureHeight;
        NativeImage nativeimage = new NativeImage(i, j, false);
        RenderSystem.bindTexture(pFramebuffer.getColorAttachment());
        nativeimage.loadFromTextureImage(0, false);
        nativeimage.mirrorVertically();
        return nativeimage;
    }

    public static void bookNotFound(Identifier book) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(Text.translatable(BOOK_NOT_FOUND, book).formatted(Formatting.RED), false);
        }
    }
}
