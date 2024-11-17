package slimeknights.tconstruct.tables.client.inventory.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.TabsWidget;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.tables.block.ITabbedBlock;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.tables.network.StationTabPacket;

import java.util.List;

public class TinkerTabsWidget implements Drawable, Element, Selectable {
    private static final Identifier TAB_IMAGE = TConstruct.getResource("textures/gui/icons.png");
    protected static final ElementScreen TAB_ELEMENT = new ElementScreen(0, 18, 26, 30, 256, 256);
    protected static final ElementScreen ACTIVE_TAB_L_ELEMENT = new ElementScreen(26, 18, 26, 30, 256, 256);
    protected static final ElementScreen ACTIVE_TAB_C_ELEMENT = new ElementScreen(52, 18, 26, 30, 256, 256);
    protected static final ElementScreen ACTIVE_TAB_R_ELEMENT = new ElementScreen(78, 18, 26, 30, 256, 256);

    private final int leftPos;
    private final int topPos;
    private final int imageWidth;
    private final int imageHeight;

    private final TabsWidget tabs;
    private final List<BlockPos> tabData;
    private final BaseTabbedScreen<?, ?> parent;

    public TinkerTabsWidget(BaseTabbedScreen<?, ?> parent) {
        this.parent = parent;

        var tabs = collectTabs(MinecraftClient.getInstance(), this.parent.getScreenHandler());

        this.tabs = new TabsWidget(parent, TAB_ELEMENT, TAB_ELEMENT, TAB_ELEMENT, ACTIVE_TAB_L_ELEMENT, ACTIVE_TAB_C_ELEMENT, ACTIVE_TAB_R_ELEMENT);
        this.tabs.tabsResource = TAB_IMAGE;

        int count = tabs.size();
        this.imageWidth = count * ACTIVE_TAB_C_ELEMENT.w + (count - 1) * this.tabs.spacing;
        this.imageHeight = ACTIVE_TAB_C_ELEMENT.h;

        this.leftPos = parent.cornerX + 4;
        this.topPos = parent.cornerY - this.imageHeight;

        this.tabs.setPosition(this.leftPos, this.topPos);

        tabs.stream().map(Pair::getLeft).forEach(this.tabs::addTab);
        this.tabData = tabs.stream().map(Pair::getRight).toList();

        // preselect the correct tab
        BlockEntity blockEntity = this.parent.getTileEntity();
        if (blockEntity != null)
            this.selectTabForPos(blockEntity.getPos());
    }

    private static List<Pair<ItemStack, BlockPos>> collectTabs(MinecraftClient minecraft, TabbedContainerMenu<?> menu) {
        List<Pair<ItemStack, BlockPos>> tabs = Lists.newArrayList();

        World level = minecraft.world;
        if (level != null) {
            for (Pair<BlockPos, BlockState> pair : menu.stationBlocks) {
                BlockState state = pair.getRight();
                BlockPos blockPos = pair.getLeft();
                ItemStack stack = state.getBlock().getPickStack(state, null, level, blockPos, minecraft.player);
                tabs.add(Pair.of(stack, blockPos));
            }
        }
        return tabs;
    }

    private void selectTabForPos(BlockPos pos) {
        for (int i = 0; i < this.tabData.size(); i++) {
            if (this.tabData.get(i).equals(pos)) {
                this.tabs.selected = i;
                return;
            }
        }
    }

    private void onNewTabSelection(BlockPos pos) {
        World level = MinecraftClient.getInstance().world;

        if (level != null) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ITabbedBlock) {
                TinkerNetwork.getInstance().sendToServer(new StationTabPacket(pos));

                // sound!
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.leftPos - 1 && mouseX < this.guiRight() + 1 && mouseY >= this.topPos - 1 && mouseY < this.guiBottom() + 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.tabs.handleMouseClicked((int) mouseX, (int) mouseY, mouseButton);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.tabs.handleMouseReleased();

        return true;
    }

    public int guiRight() {
        return this.leftPos + this.imageWidth;
    }

    public int guiBottom() {
        return this.topPos + this.imageHeight;
    }

    public Rect2i getArea() {
        return new Rect2i(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int sel = this.tabs.selected;
        this.tabs.update(mouseX, mouseY);
        this.tabs.draw(context);

        // new selection
        if (sel != this.tabs.selected) {
            if (0 <= this.tabs.selected && this.tabs.selected < this.tabData.size())
                this.onNewTabSelection(this.tabData.get(this.tabs.selected));
        }

        this.renterTooltip(context, mouseX, mouseY);
    }

    protected void renterTooltip(DrawContext context, int mouseX, int mouseY) {
        // highlighted tooltip
        World world = MinecraftClient.getInstance().world;
        if (this.tabs.highlighted > -1 && world != null) {
            BlockPos pos = this.tabData.get(this.tabs.highlighted);
            Text title;
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof NamedScreenHandlerFactory) {
                title = ((NamedScreenHandlerFactory) te).getDisplayName();
            } else {
                title = world.getBlockState(pos).getBlock().getName();
            }

            // TODO: renderComponentTooltip->renderTooltip
            this.parent.renderComponentTooltip(context, Lists.newArrayList(title), mouseX, mouseY);
        }
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder narrationOutput) {
    }
}
