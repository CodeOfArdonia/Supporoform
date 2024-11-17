package slimeknights.mantle.client.screen.book;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.screen.book.element.BookElement;

import java.util.*;
import java.util.function.Consumer;

public class BookScreen extends Screen {

    public static boolean debug = false;

    public static final int TEX_SIZE = 512;

    public static final int PAGE_MARGIN = 8;

    public static final int PAGE_PADDING_TOP = 4;
    public static final int PAGE_PADDING_BOT = 4;
    public static final int PAGE_PADDING_LEFT = 8;
    public static final int PAGE_PADDING_RIGHT = 0;

    public static final float PAGE_SCALE = 1f;
    public static final int PAGE_WIDTH_UNSCALED = 206;
    public static final int PAGE_HEIGHT_UNSCALED = 200;

    // For best results, make sure both PAGE_WIDTH_UNSCALED - (PAGE_PADDING + PAGE_MARGIN) * 2 and PAGE_HEIGHT_UNSCALED - (PAGE_PADDING + PAGE_MARGIN) * 2 divide evenly into PAGE_SCALE (without remainder)
    public static final int PAGE_WIDTH = (int) ((PAGE_WIDTH_UNSCALED - (PAGE_PADDING_LEFT + PAGE_PADDING_RIGHT + PAGE_MARGIN + PAGE_MARGIN)) / PAGE_SCALE);
    public static final int PAGE_HEIGHT = (int) ((PAGE_HEIGHT_UNSCALED - (PAGE_PADDING_TOP + PAGE_PADDING_BOT + PAGE_MARGIN + PAGE_MARGIN)) / PAGE_SCALE);

    // Used for the book to image exporter to disable arrows and mouse input
    public boolean drawArrows = true;
    public boolean mouseInput = true;

    private ArrowButton previousArrow, nextArrow, backArrow, indexArrow;

    public final BookData book;
    @Nullable
    private final Consumer<String> pageUpdater;
    @Nullable
    private final Consumer<?> bookPickup;

    private int page = -1;
    private int oldPage = -2;
    private final ArrayList<BookElement> leftElements = new ArrayList<>();
    private final ArrayList<BookElement> rightElements = new ArrayList<>();

    public AdvancementCache advancementCache;

    private double[] lastClick;
    private double[] lastDrag;

    // TODO: maybe make this a list with ability to add custom layers
    private static final ILayerRenderFunction[] LAYERS = {
            // Main layer
            BookElement::draw,
            // Overlay layer
            BookElement::drawOverlay
    };

    public BookScreen(Text title, BookData book, String page, @Nullable Consumer<String> pageUpdater, @Nullable Consumer<?> bookPickup) {
        super(title);
        this.book = book;
        this.pageUpdater = pageUpdater;
        this.bookPickup = bookPickup;

        this.client = MinecraftClient.getInstance();
        this.textRenderer = this.client.textRenderer;

        this.advancementCache = new AdvancementCache();
        if (this.client.player != null) {
            this.client.player.networkHandler.getAdvancementHandler().setListener(this.advancementCache);
        }
        this.openPage(book.findPageNumber(page, this.advancementCache));
    }

    public TextRenderer getFontRenderer() {
        TextRenderer fontRenderer = this.book.fontRenderer;
        if (fontRenderer == null) {
            fontRenderer = Objects.requireNonNull(this.client).textRenderer;
        }

        return fontRenderer;
    }

    private Vector3f splitRGB(int color) {
        float r = ColorHelper.Argb.getRed(color) / 255.F;
        float g = ColorHelper.Argb.getGreen(color) / 255.F;
        float b = ColorHelper.Argb.getBlue(color) / 255.F;

        return new Vector3f(r, g, b);
    }

    private Vector4f splitRGBA(int color) {
        float r = ColorHelper.Argb.getRed(color) / 255.F;
        float g = ColorHelper.Argb.getGreen(color) / 255.F;
        float b = ColorHelper.Argb.getBlue(color) / 255.F;
        float a = ColorHelper.Argb.getAlpha(color) / 255.F;

        return new Vector4f(r, g, b, a);
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.client == null) {
            return;
        }

        TextRenderer fontRenderer = this.getFontRenderer();

        if (debug) {
            graphics.fill(0, 0, fontRenderer.getWidth("DEBUG") + 4, fontRenderer.fontHeight + 4, 0x55000000);
            graphics.drawText(this.textRenderer, "DEBUG", 2, 2, 0xFFFFFFFF, false);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        // RenderSystem.enableAlphaTest(); TODO: still needed?
        RenderSystem.enableBlend();

        Vector3f coverColor = this.splitRGB(this.book.appearance.coverColor);

        if (this.page == -1) {
            this.renderCover(graphics, coverColor);
        } else {
            MatrixStack matrixStack = graphics.getMatrices();
            // TODO: can we create copies of the guiGraphics?
            // Jank way to copy last matrix in matrix stack, as no proper way is provided
//      PoseStack leftMatrix = new PoseStack();
//      leftMatrix.last().pose().mul(matrixStack.last().pose());
//      leftMatrix.last().normal().mul(matrixStack.last().normal());
//
//      PoseStack rightMatrix = new PoseStack();
//      rightMatrix.last().pose().mul(matrixStack.last().pose());
//      rightMatrix.last().normal().mul(matrixStack.last().normal());
//
//      drawerTransform(leftMatrix, false);
//      drawerTransform(rightMatrix, true);
//
//      leftMatrix.scale(PAGE_SCALE, PAGE_SCALE, 1F);
//      rightMatrix.scale(PAGE_SCALE, PAGE_SCALE, 1F);

            boolean renderLeft = this.shouldRenderPage(this.page, false);
            boolean renderRight = this.shouldRenderPage(this.page, true);

            this.renderUnderLayer(graphics, coverColor);

            if (renderLeft) {
                this.renderPageBackground(graphics, false);
            }

            if (renderRight) {
                this.renderPageBackground(graphics, true);
            }

            int leftMX = this.getMouseX(false);
            int rightMX = this.getMouseX(true);
            int mY = this.getMouseY();

            // TODO: can we draw the left all at once then the right all at once to reduce number of matrix operations?
            for (ILayerRenderFunction layer : LAYERS) {
                if (renderLeft) {
                    matrixStack.push();
                    this.drawerTransform(matrixStack, false);
                    matrixStack.scale(PAGE_SCALE, PAGE_SCALE, 1F);
                    this.renderPageLayer(graphics, leftMX, mY, partialTicks, this.leftElements, layer);
                    matrixStack.pop();
                }

                if (renderRight) {
                    matrixStack.push();
                    this.drawerTransform(matrixStack, true);
                    matrixStack.scale(PAGE_SCALE, PAGE_SCALE, 1F);
                    this.renderPageLayer(graphics, rightMX, mY, partialTicks, this.rightElements, layer);
                    matrixStack.pop();
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private boolean shouldRenderPage(int pageNum, boolean rightSide) {
        if (!rightSide) {
            return pageNum != 0;
        }

        int fullPageCount = this.book.getFullPageCount(this.advancementCache);
        return this.page < fullPageCount - 1 || this.book.getPageCount(this.advancementCache) % 2 != 0;
    }

    private void renderCover(DrawContext graphics, Vector3f coverColor) {
        TextRenderer fontRenderer = this.getFontRenderer();

        Identifier cover = this.book.appearance.getCoverTexture();

        int centerX = this.width / 2 - PAGE_WIDTH_UNSCALED / 2;
        int centerY = this.height / 2 - PAGE_HEIGHT_UNSCALED / 2;

        RenderSystem.setShaderColor(coverColor.x(), coverColor.y(), coverColor.z(), 1.0f);
        graphics.drawTexture(cover, centerX, centerY, 0, 0, PAGE_WIDTH_UNSCALED, PAGE_HEIGHT_UNSCALED, TEX_SIZE, TEX_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        MatrixStack matrixStack = graphics.getMatrices();
        if (!this.book.appearance.title.isEmpty()) {
            graphics.drawTexture(cover, centerX, centerY, 0, PAGE_HEIGHT_UNSCALED, PAGE_WIDTH_UNSCALED, PAGE_HEIGHT_UNSCALED, TEX_SIZE, TEX_SIZE);

            matrixStack.push();

            int width = fontRenderer.getWidth(this.book.appearance.title);
            float scale = MathHelper.clamp((float) PAGE_WIDTH / width, 0F, 2.5F);

            matrixStack.scale(scale, scale, 1F);

            graphics.drawText(this.textRenderer, this.book.appearance.title, (int) ((this.width / 2F) / scale + 3 - width / 2F), (int) ((this.height / 2F - fontRenderer.fontHeight / 2F) / scale - 4), this.book.appearance.getCoverTextColor(), false);
            matrixStack.pop();
        }

        if (!this.book.appearance.subtitle.isEmpty()) {
            matrixStack.push();

            int width = fontRenderer.getWidth(this.book.appearance.subtitle);
            float scale = MathHelper.clamp((float) PAGE_WIDTH / width, 0F, 1.5F);

            matrixStack.scale(scale, scale, 1F);
            graphics.drawText(this.textRenderer, this.book.appearance.subtitle, (int) ((this.width / 2F) / scale + 7 - width / 2F), (int) ((this.height / 2F + 100 - fontRenderer.fontHeight * 2) / scale), this.book.appearance.getCoverTextColor(), false);
            matrixStack.pop();
        }
    }

    private void renderUnderLayer(DrawContext graphics, Vector3f coverColor) {
        graphics.setShaderColor(coverColor.x(), coverColor.y(), coverColor.z(), 1f);
        graphics.drawTexture(this.book.appearance.getBookTexture(), this.width / 2 - PAGE_WIDTH_UNSCALED, this.height / 2 - PAGE_HEIGHT_UNSCALED / 2, 0, 0, PAGE_WIDTH_UNSCALED * 2, PAGE_HEIGHT_UNSCALED, TEX_SIZE, TEX_SIZE);
        graphics.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderPageBackground(DrawContext graphics, boolean rightSide) {
        Vector3f pageTint = this.splitRGB(this.book.appearance.getPageTint());
        graphics.setShaderColor(pageTint.x(), pageTint.y(), pageTint.z(), 1f);
        if (!rightSide) {
            graphics.drawTexture(this.book.appearance.getBookTexture(), this.width / 2 - PAGE_WIDTH_UNSCALED, this.height / 2 - PAGE_HEIGHT_UNSCALED / 2, 0, PAGE_HEIGHT_UNSCALED, PAGE_WIDTH_UNSCALED, PAGE_HEIGHT_UNSCALED, TEX_SIZE, TEX_SIZE);
        } else {
            graphics.drawTexture(this.book.appearance.getBookTexture(), this.width / 2, this.height / 2 - PAGE_HEIGHT_UNSCALED / 2, PAGE_WIDTH_UNSCALED, PAGE_HEIGHT_UNSCALED, PAGE_WIDTH_UNSCALED, PAGE_HEIGHT_UNSCALED, TEX_SIZE, TEX_SIZE);
        }
        graphics.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderPageLayer(DrawContext graphics, int mouseX, int mouseY, float partialTicks, List<BookElement> elements, ILayerRenderFunction layerFunc) {
        RenderSystem.setShaderTexture(0, this.book.appearance.getCoverTexture());

        TextRenderer font = this.getFontRenderer();

        for (BookElement element : elements) {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            layerFunc.draw(element, graphics, mouseX, mouseY, partialTicks, font);
        }
    }

    @Override
    protected void init() {
        super.init();

        this.clearChildren();

        this.previousArrow = this.addDrawableChild(new ArrowButton(this.book, 50, -50, ArrowButton.ArrowType.PREV, this.book.appearance.arrowColor, this.book.appearance.arrowColorHover, (p_212998_1_) -> {
            this.page--;

            if (this.page < -1) {
                this.page = -1;
            }

            this.oldPage = -2;
            this.buildPages();
        }));

        this.nextArrow = this.addDrawableChild(new ArrowButton(this.book, -50, -50, ArrowButton.ArrowType.NEXT, this.book.appearance.arrowColor, this.book.appearance.arrowColorHover, (p_212998_1_) -> {
            this.page++;

            int fullPageCount = this.book.getFullPageCount(this.advancementCache);

            if (this.page >= fullPageCount) {
                this.page = fullPageCount - 1;
            }

            this.oldPage = -2;
            this.buildPages();
        }));

        this.backArrow = this.addDrawableChild(new ArrowButton(this.book, this.width / 2 - ArrowButton.WIDTH / 2, this.height / 2 + ArrowButton.HEIGHT / 2 + PAGE_HEIGHT / 2, ArrowButton.ArrowType.LEFT, this.book.appearance.arrowColor, this.book.appearance.arrowColorHover, (p_212998_1_) -> {
            if (this.oldPage >= -1) {
                this.page = this.oldPage;
            }

            this.oldPage = -2;
            this.buildPages();
        }));

        this.indexArrow = this.addDrawableChild(new ArrowButton(this.book, this.width / 2 - PAGE_WIDTH_UNSCALED - ArrowButton.WIDTH / 2, this.height / 2 - PAGE_HEIGHT_UNSCALED / 2, ArrowButton.ArrowType.BACK_UP, this.book.appearance.arrowColor, this.book.appearance.arrowColorHover, (p_212998_1_) -> {
            this.openPage(this.book.findPageNumber("index.page1", this.advancementCache));

            this.oldPage = -2;
            this.buildPages();
        }));

        if (this.bookPickup != null) {
            int margin = 10;
            if (this.height / 2 + PAGE_HEIGHT_UNSCALED / 2 + margin + 20 >= this.height) {
                margin = 0;
            }

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("lectern.take_book"), button -> {
                this.close();
                this.bookPickup.accept(null);
            }).position(this.width / 2 - 196 / 2, this.height / 2 + PAGE_HEIGHT_UNSCALED / 2 + margin).size(196, 20).build());
        }

        this.buildPages();
    }

    @Override
    public void tick() {
        super.tick();

        this.previousArrow.visible = this.page != -1 && this.drawArrows;
        this.nextArrow.visible = this.page + 1 < this.book.getFullPageCount(this.advancementCache) && this.drawArrows;
        this.backArrow.visible = this.oldPage >= -1 && this.drawArrows;

        if (this.page == -1) {
            this.nextArrow.method_46421(this.width / 2 + 80);
            this.indexArrow.visible = false;
        } else {
            this.previousArrow.method_46421(this.width / 2 - 184);
            this.nextArrow.method_46421(this.width / 2 + 165);

            SectionData index = this.book.findSection("index", this.advancementCache);
            this.indexArrow.visible = index != null && (this.page - 1) * 2 + 2 > index.getPageCount() && this.drawArrows;
        }

        this.previousArrow.method_46419(this.height / 2 + 75);
        this.nextArrow.method_46419(this.height / 2 + 75);
    }

    /**
     * Goes to the previous page
     */
    public boolean previousPage() {
        this.page--;
        if (this.page < -1) {
            this.page = -1;

            return false;
        }
        this.oldPage = -2;
        this.buildPages();

        return true;
    }

    /**
     * Goes to the next page
     */
    public boolean nextPage() {
        this.page++;
        int fullPageCount = this.book.getFullPageCount(this.advancementCache);
        if (this.page >= fullPageCount) {
            this.page = fullPageCount - 1;

            return false;
        }
        this.oldPage = -2;
        this.buildPages();

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> {
                this.previousPage();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> {
                this.nextPage();
                return true;
            }
            case GLFW.GLFW_KEY_F3 -> {
                debug = !debug;
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double unKnown1, double unKnown2, double scrollDelta) {
        if (scrollDelta < 0.0D) {
            this.nextPage();
            return true;
        } else if (scrollDelta > 0.0D) {
            this.previousPage();
            return true;
        }

        return super.mouseScrolled(scrollDelta, unKnown1, unKnown2);
    }

    @Override
    public boolean mouseClicked(double originalMouseX, double originalMouseY, int mouseButton) {
        boolean right = false;

        double mouseX = this.getMouseX(false);
        double mouseY = this.getMouseY();

        if (mouseX > PAGE_WIDTH + (PAGE_MARGIN + PAGE_PADDING_LEFT) / PAGE_SCALE) {
            mouseX = this.getMouseX(true);
            right = true;
        }

        this.lastClick = new double[]{mouseX, mouseY};

        // Not foreach to prevent conmodification crashes
        int oldPage = this.page;
        List<BookElement> elementList = ImmutableList.copyOf(right ? this.rightElements : this.leftElements);
        for (BookElement element : elementList) {
            element.mouseClicked(mouseX, mouseY, mouseButton);
            // if we changed page stop so we don't act on the new page
            if (this.page != oldPage) {
                return true;
            }
        }

        return super.mouseClicked(originalMouseX, originalMouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double originalMouseX, double originalMouseY, int mouseButton) {
        boolean right = false;
        double mouseX = this.getMouseX(false);
        double mouseY = this.getMouseY();

        if (mouseX > PAGE_WIDTH + (PAGE_MARGIN + PAGE_PADDING_LEFT) / PAGE_SCALE) {
            mouseX = this.getMouseX(true);
            right = true;
        }

        // Not foreach to prevent conmodification crashes
        for (int i = 0; right ? i < this.rightElements.size() : i < this.leftElements.size(); i++) {
            BookElement element = right ? this.rightElements.get(i) : this.leftElements.get(i);
            element.mouseReleased(mouseX, mouseY, mouseButton);
        }

        this.lastClick = null;
        this.lastDrag = null;

        return super.mouseReleased(originalMouseX, originalMouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean right = false;
        mouseX = this.getMouseX(false);
        mouseY = this.getMouseY();

        if (mouseX > PAGE_WIDTH + (PAGE_MARGIN + PAGE_PADDING_LEFT) / PAGE_SCALE) {
            mouseX = this.getMouseX(true);
            right = true;
        }

        if (this.lastClick != null) {
            if (this.lastDrag == null)
                this.lastDrag = new double[]{mouseX, mouseY};

            // Not foreach to prevent conmodification crashes
            for (int i = 0; right ? i < this.rightElements.size() : i < this.leftElements.size(); i++) {
                BookElement element = right ? this.rightElements.get(i) : this.leftElements.get(i);
                element.mouseDragged(this.lastClick[0], this.lastClick[1], mouseX, mouseY, this.lastDrag[0], this.lastDrag[1], button);
            }

            this.lastDrag = new double[]{mouseX, mouseY};
        }


        return true;
    }

    @Override
    public void removed() {
        if (this.client == null || this.client.player == null) {
            return;
        }
        // find what page to update
        if (this.pageUpdater != null) {
            String pageStr = "";
            if (this.page >= 0) {
                PageData page = this.page == 0 ? this.book.findPage(0, this.advancementCache) : this.book.findPage((this.page - 1) * 2 + 1, this.advancementCache);
                if (page == null) {
                    page = this.book.findPage((this.page - 1) * 2 + 2, this.advancementCache);
                }
                if (page != null && page.parent != null) {
                    pageStr = page.parent.name + "." + page.name;
                }
            }
            this.pageUpdater.accept(pageStr);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void drawerTransform(MatrixStack matrixStack, boolean rightSide) {
        if (rightSide) {
            matrixStack.translate(this.width / 2 + PAGE_PADDING_RIGHT + PAGE_MARGIN, this.height / 2 - PAGE_HEIGHT_UNSCALED / 2 + PAGE_PADDING_TOP + PAGE_MARGIN, 0);
        } else {
            matrixStack.translate(this.width / 2 - PAGE_WIDTH_UNSCALED + PAGE_PADDING_LEFT + PAGE_MARGIN, this.height / 2 - PAGE_HEIGHT_UNSCALED / 2 + PAGE_PADDING_TOP + PAGE_MARGIN, 0);
        }
    }

    // offset to the left edge of the left/right side
    protected float leftOffset(boolean rightSide) {
        if (rightSide) {
            // from center: go padding + margin to the right
            return this.width / 2 + PAGE_PADDING_RIGHT + PAGE_MARGIN;
        } else {
            // from center: go page width left, then right with padding and margin
            return this.width / 2 - PAGE_WIDTH_UNSCALED + PAGE_PADDING_LEFT + PAGE_MARGIN;
        }
    }

    protected float topOffset() {
        return this.height / 2 - PAGE_HEIGHT_UNSCALED / 2 + PAGE_PADDING_TOP + PAGE_MARGIN;
    }

    protected int getMouseX(boolean rightSide) {
        assert this.client != null;
        if (!this.mouseInput) {
            return -1;
        }

        return (int) ((MinecraftClient.getInstance().mouse.getX() * this.width / this.client.getWindow().getWidth() - this.leftOffset(rightSide)) / PAGE_SCALE);
    }

    protected int getMouseY() {
        assert this.client != null;
        if (!this.mouseInput) {
            return -1;
        }

        return (int) ((MinecraftClient.getInstance().mouse.getY() * this.height / this.client.getWindow().getHeight() - 1 - this.topOffset()) / PAGE_SCALE);
    }

    public int openPage(int page) {
        return this.openPage(page, false);
    }

    public int openPage(int page, boolean returner) {
        if (page < 0) {
            return -1;
        }

        int bookPage;
        if (page == 1) {
            bookPage = 0;
        } else if (page % 2 == 0) {
            bookPage = (page - 1) / 2 + 1;
        } else {
            bookPage = (page - 2) / 2 + 1;
        }

        if (bookPage >= -1 && bookPage < this.book.getFullPageCount(this.advancementCache)) {
            if (returner) {
                this.oldPage = this.page;
            }

            this._setPage(bookPage);
        }

        return page % 2 == 0 ? 0 : 1;
    }

    public void _setPage(int page) {
        this.page = page;
        this.buildPages();
    }

    public int getPage(int side) {
        if (this.page == 0 && side == 0) {
            return -1;
        } else if (this.page == 0 && side == 1) {
            return 0;
        } else if (side == 0) {
            return (this.page - 1) * 2 + 1;
        } else if (side == 1) {
            return (this.page - 2) * 2 + 2;
        } else {
            return -1;
        }
    }

    public int getPage_() {
        return this.page;
    }

    public List<BookElement> getElements(int side) {
        return side == 0 ? this.leftElements : side == 1 ? this.rightElements : Collections.emptyList();
    }

    public void openCover() {
        this._setPage(-1);

        this.leftElements.clear();
        this.rightElements.clear();
        this.buildPages();
    }

    private void buildPages() {
        this.leftElements.clear();
        this.rightElements.clear();

        if (this.page == -1) {
            return;
        }

        if (this.page == 0) {
            PageData page = this.book.findPage(0, this.advancementCache);

            if (page != null) {
                page.content.build(this.book, this.rightElements, false);
            }
        } else {
            PageData leftPage = this.book.findPage((this.page - 1) * 2 + 1, this.advancementCache);
            PageData rightPage = this.book.findPage((this.page - 1) * 2 + 2, this.advancementCache);

            if (leftPage != null) {
                leftPage.content.build(this.book, this.leftElements, false);
            }
            if (rightPage != null) {
                rightPage.content.build(this.book, this.rightElements, true);
            }
        }

        for (BookElement element : this.leftElements) {
            element.parent = this;
        }
        for (BookElement element : this.rightElements) {
            element.parent = this;
        }
    }

    public static class AdvancementCache implements ClientAdvancementManager.Listener {

        private final HashMap<Advancement, AdvancementProgress> progress = new HashMap<>();
        private final HashMap<Identifier, Advancement> nameCache = new HashMap<>();

        @Nullable
        public AdvancementProgress getProgress(String id) {
            return this.getProgress(this.getAdvancement(id));
        }

        @Nullable
        public AdvancementProgress getProgress(Advancement advancement) {
            return this.progress.get(advancement);
        }

        public Advancement getAdvancement(String id) {
            return this.nameCache.get(new Identifier(id));
        }

        @Override
        public void setProgress(Advancement advancement, AdvancementProgress advancementProgress) {
            this.progress.put(advancement, advancementProgress);
        }

        @Override
        public void selectTab(@Nullable Advancement advancement) {
            // noop
        }

        @Override
        public void onRootAdded(Advancement advancement) {
            this.nameCache.put(advancement.getId(), advancement);
        }

        @Override
        public void onRootRemoved(Advancement advancement) {
            this.progress.remove(advancement);
            this.nameCache.remove(advancement.getId());
        }

        @Override
        public void onDependentAdded(Advancement advancement) {
            this.nameCache.put(advancement.getId(), advancement);
        }

        @Override
        public void onDependentRemoved(Advancement advancement) {
            this.progress.remove(advancement);
            this.nameCache.remove(advancement.getId());
        }

        @Override
        public void onClear() {
            this.progress.clear();
            this.nameCache.clear();
        }
    }
}
