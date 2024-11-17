package slimeknights.mantle.client.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.inventory.WrapperSlot;

import java.util.ArrayList;
import java.util.List;

public class MultiModuleScreen<CONTAINER extends MultiModuleContainerMenu<?>> extends HandledScreen<CONTAINER> {

    protected List<ModuleScreen<?, ?>> modules = Lists.newArrayList();

    public int cornerX;
    public int cornerY;
    public int realWidth;
    public int realHeight;

    public MultiModuleScreen(CONTAINER container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);

        this.realWidth = -1;
        this.realHeight = -1;
//    this.passEvents = true;  // TODO: needed?
    }

    protected void addModule(ModuleScreen<?, ?> module) {
        this.modules.add(module);
    }

    public List<Rect2i> getModuleAreas() {
        List<Rect2i> areas = new ArrayList<>(this.modules.size());
        for (ModuleScreen<?, ?> module : this.modules) {
            areas.add(module.getArea());
        }
        return areas;
    }

    @Override
    protected void init() {
        if (this.realWidth > -1) {
            // has to be reset before calling initGui so the position is getting retained
            this.backgroundWidth = this.realWidth;
            this.backgroundHeight = this.realHeight;
        }

        super.init();

        this.cornerX = this.x;
        this.cornerY = this.y;
        this.realWidth = this.backgroundWidth;
        this.realHeight = this.backgroundHeight;

        assert this.client != null;
        for (ModuleScreen<?, ?> module : this.modules) {
            this.updateSubmodule(module);
        }
        // TODO: this is a small ordering change, does it need another hook?
        for (ModuleScreen<?, ?> module : this.modules) {
            module.init(this.client, this.width, this.height);
            this.updateSubmodule(module);
        }
    }

//  @Override
//  public void init(Minecraft mc, int width, int height) {
//    super.init(mc, width, height);
//
//    for (ModuleScreen<?,?> module : this.modules) {
//      module.init(mc, width, height);
//      this.updateSubmodule(module);
//    }
//  }

    @Override
    protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
        for (ModuleScreen<?, ?> module : this.modules) {
            module.handleDrawGuiContainerBackgroundLayer(graphics, partialTicks, mouseX, mouseY);
        }
    }

    @Override
    protected void drawForeground(DrawContext graphics, int mouseX, int mouseY) {
        this.drawContainerName(graphics);
        this.drawPlayerInventoryName(graphics);

        MatrixStack poses = graphics.getMatrices();
        for (ModuleScreen<?, ?> module : this.modules) {
            // set correct state for the module
            poses.push();
            poses.translate(module.x - this.x, module.y - this.y, 0.0F);
            module.handleDrawGuiContainerForegroundLayer(graphics, mouseX, mouseY);
            poses.pop();
        }
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext graphics, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(graphics, mouseX, mouseY);

        for (ModuleScreen<?, ?> module : this.modules) {
            module.handleRenderHoveredTooltip(graphics, mouseX, mouseY);
        }
    }

    protected void drawBackground(DrawContext graphics, Identifier background) {
        graphics.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.drawTexture(background, this.cornerX, this.cornerY, 0, 0, this.realWidth, this.realHeight);
    }

    protected void drawContainerName(DrawContext graphics) {
        graphics.drawText(this.textRenderer, this.getTitle().asOrderedText(), 8, 6, 0x404040, false);
    }

    protected void drawPlayerInventoryName(DrawContext graphics) {
        assert MinecraftClient.getInstance().player != null;
        Text localizedName = MinecraftClient.getInstance().player.getInventory().getDisplayName();
        graphics.drawText(this.textRenderer, localizedName.asOrderedText(), 8, this.backgroundHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void resize(MinecraftClient mc, int width, int height) {
        super.resize(mc, width, height);

        for (ModuleScreen<?, ?> module : this.modules) {
            module.resize(mc, width, height);
            this.updateSubmodule(module);
        }
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        int oldX = this.x;
        int oldY = this.y;
        int oldW = this.backgroundWidth;
        int oldH = this.backgroundHeight;

        this.x = this.cornerX;
        this.y = this.cornerY;
        this.backgroundWidth = this.realWidth;
        this.backgroundHeight = this.realHeight;
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(graphics, mouseX, mouseY);
        this.x = oldX;
        this.y = oldY;
        this.backgroundWidth = oldW;
        this.backgroundHeight = oldH;
    }

    // needed to get the correct slot on clicking
    @Override
    protected boolean isPointWithinBounds(int left, int top, int right, int bottom, double pointX, double pointY) {
        pointX -= this.cornerX;
        pointY -= this.cornerY;
        return pointX >= left - 1 && pointX < left + right + 1 && pointY >= top - 1 && pointY < top + bottom + 1;
    }

    protected void updateSubmodule(ModuleScreen<?, ?> module) {
        module.updatePosition(this.cornerX, this.cornerY, this.realWidth, this.realHeight);

        if (module.x < this.x) {
            this.backgroundWidth += this.x - module.x;
            this.x = module.x;
        }

        if (module.y < this.y) {
            this.backgroundHeight += this.y - module.y;
            this.y = module.y;
        }

        if (module.guiRight() > this.x + this.backgroundWidth) {
            this.backgroundWidth = module.guiRight() - this.x;
        }

        if (module.guiBottom() > this.y + this.backgroundHeight) {
            this.backgroundHeight = module.guiBottom() - this.y;
        }
    }

    @Override
    public void drawSlot(DrawContext graphics, Slot slotIn) {
        ModuleScreen<?, ?> module = this.getModuleForSlot(slotIn.id);

        if (module != null) {
            Slot slot = slotIn;
            // unwrap for the call to the module
            if (slotIn instanceof WrapperSlot) {
                slot = ((WrapperSlot) slotIn).parent;
            }

            if (!module.shouldDrawSlot(slot)) {
                return;
            }
        }

        // update slot positions
        if (slotIn instanceof WrapperSlot) {
            slotIn.x = ((WrapperSlot) slotIn).parent.x;
            slotIn.y = ((WrapperSlot) slotIn).parent.y;
        }

        super.drawSlot(graphics, slotIn);
    }

    @Override
    public boolean isPointOverSlot(Slot slotIn, double mouseX, double mouseY) {
        ModuleScreen<?, ?> module = this.getModuleForSlot(slotIn.id);

        // mouse inside the module of the slot?
        if (module != null) {
            Slot slot = slotIn;
            // unwrap for the call to the module
            if (slotIn instanceof WrapperSlot) {
                slot = ((WrapperSlot) slotIn).parent;
            }

            if (!module.shouldDrawSlot(slot)) {
                return false;
            }
        }

        return super.isPointOverSlot(slotIn, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        ModuleScreen<?, ?> module = this.getModuleForPoint(mouseX, mouseY);

        if (module != null) {
            if (module.handleMouseClicked(mouseX, mouseY, mouseButton)) {
                return false;
            }
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick, double unkowwn) {
        ModuleScreen<?, ?> module = this.getModuleForPoint(mouseX, mouseY);

        if (module != null) {
            if (module.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
                return false;
            }
        }

        return super.mouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick, unkowwn);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        ModuleScreen<?, ?> module = this.getModuleForPoint(mouseX, mouseY);

        if (module != null) {
            if (module.handleMouseScrolled(mouseX, mouseY, delta)) {
                return false;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        ModuleScreen<?, ?> module = this.getModuleForPoint(mouseX, mouseY);

        if (module != null) {
            if (module.handleMouseReleased(mouseX, mouseY, state)) {
                return false;
            }
        }

        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Nullable
    protected ModuleScreen<?, ?> getModuleForPoint(double x, double y) {
        for (ModuleScreen<?, ?> module : this.modules) {
            if (this.isPointWithinBounds(module.x, module.y, module.guiRight(), module.guiBottom(), x + this.cornerX, y + this.cornerY)) {
                return module;
            }
        }

        return null;
    }

    @Nullable
    protected ModuleScreen<?, ?> getModuleForSlot(int slotNumber) {
        return this.getModuleForContainer(this.getScreenHandler().getSlotContainer(slotNumber));
    }

    @Nullable
    protected ModuleScreen<?, ?> getModuleForContainer(ScreenHandler container) {
        for (ModuleScreen<?, ?> module : this.modules) {
            if (module.getScreenHandler() == container) {
                return module;
            }
        }

        return null;
    }

    @Override
    public CONTAINER getScreenHandler() {
        return this.handler;
    }
}
