package slimeknights.tconstruct.tables.client.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.Icons;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.tables.client.inventory.module.SideInventoryScreen;
import slimeknights.tconstruct.tables.client.inventory.widget.TinkerTabsWidget;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.tables.menu.module.SideInventoryContainer;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

public class BaseTabbedScreen<TILE extends BlockEntity, CONTAINER extends TabbedContainerMenu<TILE>> extends MultiModuleScreen<CONTAINER> {
    protected static final Text COMPONENT_WARNING = TConstruct.makeTranslation("gui", "warning");
    protected static final Text COMPONENT_ERROR = TConstruct.makeTranslation("gui", "error");

    public static final Identifier BLANK_BACK = TConstruct.getResource("textures/gui/blank.png");

    @Nullable
    protected final TILE tile;
    protected TinkerTabsWidget tabsScreen;

    public BaseTabbedScreen(CONTAINER container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);
        this.tile = container.getTile();
    }

    @Override
    protected void init() {
        super.init();

        this.tabsScreen = addDrawableChild(new TinkerTabsWidget(this));
    }

    @Nullable
    public TILE getTileEntity() {
        return this.tile;
    }

    protected void drawIcon(DrawContext matrices, Slot slot, ElementScreen element) {
        RenderSystem.setShaderTexture(0, Icons.ICONS);
        element.draw(matrices, slot.x + this.cornerX - 1, slot.y + this.cornerY - 1);
    }

    protected void drawIconEmpty(DrawContext context, Slot slot, ElementScreen element) {
        if (slot.hasStack()) {
            return;
        }

        this.drawIcon(context, slot, element);
    }

    protected void drawIconEmpty(DrawContext context, Slot slot, Pattern pattern) {
        if (!slot.hasStack()) {
            GuiUtil.renderPattern(context, pattern, slot.x + this.cornerX, slot.y + this.cornerY);
        }
    }

    public void error(Text message) {
    }

    public void warning(Text message) {
    }

    public void updateDisplay() {
    }

    protected void addChestSideInventory(PlayerInventory inventory) {
        SideInventoryContainer<?> sideInventoryContainer = getScreenHandler().getSubContainer(SideInventoryContainer.class);
        if (sideInventoryContainer != null) {
            // no title if missing one
            Text sideInventoryName = Text.empty();
            BlockEntity te = sideInventoryContainer.getTile();
            if (te instanceof NamedScreenHandlerFactory) {
                sideInventoryName = Objects.requireNonNullElse(((NamedScreenHandlerFactory) te).getDisplayName(), Text.empty());
            }

            this.addModule(new SideInventoryScreen<>(this, sideInventoryContainer, inventory, sideInventoryName, sideInventoryContainer.getSlotCount(), sideInventoryContainer.getColumns()));
        }
    }

    @Override
    public List<Rect2i> getModuleAreas() {
        List<Rect2i> areas = super.getModuleAreas();
        areas.add(this.tabsScreen.getArea());
        return areas;
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.isClickOutsideBounds(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && !this.tabsScreen.isMouseOver(mouseX, mouseY);
    }
}
