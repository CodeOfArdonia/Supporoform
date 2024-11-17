package slimeknights.tconstruct.tables.client.inventory;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.item.TooltipContext.Default;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.ModuleScreen;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.layout.LayoutIcon;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;
import slimeknights.tconstruct.tables.client.inventory.widget.SlotButtonItem;
import slimeknights.tconstruct.tables.client.inventory.widget.TinkerStationButtonsWidget;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.menu.slot.TinkerStationSlot;
import slimeknights.tconstruct.tables.network.TinkerStationRenamePacket;
import slimeknights.tconstruct.tables.network.TinkerStationSelectionPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.INPUT_SLOT;
import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.TINKER_SLOT;

public class TinkerStationScreen extends BaseTabbedScreen<TinkerStationBlockEntity, TinkerStationContainerMenu> {
    // titles to display
    private static final Text COMPONENTS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.components");
    private static final Text MODIFIERS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.modifiers");
    private static final Text UPGRADES_TEXT = TConstruct.makeTranslation("gui", "tinker_station.upgrades");
    private static final Text TRAITS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.traits");
    // fallback text for crafting with no named slots
    private static final Text ASCII_ANVIL = Text.literal("\n\n")
            .append("       .\n")
            .append("     /( _________\n")
            .append("     |  >:=========`\n")
            .append("     )(  \n")
            .append("     \"\"")
            .formatted(Formatting.DARK_GRAY);

    // parameters to display the still filled slots when changing layout
    private static final int STILL_FILLED_X = 112;
    private static final int STILL_FILLED_Y = 62;
    private static final int STILL_FILLED_SPACING = 18;

    // texture
    private static final Identifier TINKER_STATION_TEXTURE = TConstruct.getResource("textures/gui/tinker_station.png");
    // texture elements
    private static final ElementScreen ACTIVE_TEXT_FIELD = new ElementScreen(0, 210, 91, 12, 256, 256);
    private static final ElementScreen ITEM_COVER = new ElementScreen(176, 18, 70, 64);
    // slots
    private static final ElementScreen SLOT_BACKGROUND = new ElementScreen(176, 0, 18, 18);
    private static final ElementScreen SLOT_BORDER = new ElementScreen(194, 0, 18, 18);
    private static final ElementScreen SLOT_SPACE_TOP = new ElementScreen(0, 174 + 2, 18, 2);
    private static final ElementScreen SLOT_SPACE_BOTTOM = new ElementScreen(0, 174, 18, 2);
    // panel
    private static final ElementScreen PANEL_SPACE_LEFT = new ElementScreen(0, 174, 5, 4);
    private static final ElementScreen PANEL_SPACE_RIGHT = new ElementScreen(9, 174, 9, 4);
    private static final ElementScreen LEFT_BEAM = new ElementScreen(0, 180, 2, 7);
    private static final ElementScreen RIGHT_BEAM = new ElementScreen(131, 180, 2, 7);
    private static final ScalableElementScreen CENTER_BEAM = new ScalableElementScreen(2, 180, 129, 7);
    // text boxes
    private static final ElementScreen TEXT_BOX = new ElementScreen(0, 222, 90, 12);

    /**
     * Number of button columns in the UI
     */
    public static final int COLUMN_COUNT = 5;

    // configurable elements
    protected ElementScreen buttonDecorationTop = SLOT_SPACE_TOP;
    protected ElementScreen buttonDecorationBot = SLOT_SPACE_BOTTOM;
    protected ElementScreen panelDecorationL = PANEL_SPACE_LEFT;
    protected ElementScreen panelDecorationR = PANEL_SPACE_RIGHT;

    protected ElementScreen leftBeam = new ElementScreen(0, 0, 0, 0);
    protected ElementScreen rightBeam = new ElementScreen(0, 0, 0, 0);
    protected ScalableElementScreen centerBeam = new ScalableElementScreen(0, 0, 0, 0);

    /**
     * Gets the default layout to apply, the "repair" button
     */
    @NotNull
    @Getter
    private final StationSlotLayout defaultLayout;
    /**
     * Currently selected tool
     */
    @NotNull
    @Getter
    private StationSlotLayout currentLayout;

    // components
    protected TextFieldWidget textField;
    protected InfoPanelScreen tinkerInfo;
    protected InfoPanelScreen modifierInfo;
    protected TinkerStationButtonsWidget buttonsScreen;

    /**
     * Maximum available slots
     */
    @Getter
    private final int maxInputs;
    /**
     * How many of the available input slots are active
     */
    protected int activeInputs;

    public TinkerStationScreen(TinkerStationContainerMenu container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);

        this.tinkerInfo = new InfoPanelScreen(this, container, playerInventory, title);
        this.tinkerInfo.setTextScale(8 / 9f);
        this.addModule(this.tinkerInfo);

        this.modifierInfo = new InfoPanelScreen(this, container, playerInventory, title);
        this.modifierInfo.setTextScale(7 / 9f);
        this.addModule(this.modifierInfo);

        this.tinkerInfo.yOffset = 5;
        this.modifierInfo.yOffset = this.tinkerInfo.backgroundHeight + 9;

        this.backgroundHeight = 174;

        // determine number of inputs
        int max = 5;
        TinkerStationBlockEntity te = container.getTile();
        if (te != null) {
            max = te.getInputCount(); // TODO: not station sensitive
        }
        this.maxInputs = max;

        // large if at least 4, todo can configure?
        if (max > 3) {
            this.metal();
        } else {
            this.wood();
        }
        // apply base slot information
        if (te == null) {
            this.defaultLayout = StationSlotLayout.EMPTY;
        } else {
            this.defaultLayout = StationSlotLayoutLoader.getInstance().get(Registries.BLOCK.getId(te.getCachedState().getBlock()));
        }
        this.currentLayout = this.defaultLayout;
        this.activeInputs = Math.min(defaultLayout.getInputCount(), max);
        this.passEvents = false;
    }

    @Override
    public void init() {

        assert this.client != null;

        // workaround to line up the tabs on switching even though the GUI is a tad higher
        this.y += 4;
        this.cornerY += 4;

        this.tinkerInfo.xOffset = 2;
        this.tinkerInfo.yOffset = this.centerBeam.h + this.panelDecorationL.h;
        this.modifierInfo.xOffset = this.tinkerInfo.xOffset;
        this.modifierInfo.yOffset = this.tinkerInfo.yOffset + this.tinkerInfo.backgroundHeight + 4;

        for (ModuleScreen<?, ?> module : this.modules) {
            module.y += 4;
        }

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        textField = new TextFieldWidget(this.textRenderer, x + 80, y + 5, 82, 9, Text.empty());
        textField.setFocusUnlocked(true);
        textField.setEditableColor(-1);
        textField.setUneditableColor(-1);
        textField.setDrawsBackground(false);
        textField.setMaxLength(50);
        textField.setChangedListener(this::onNameChanged);
        textField.setText("");
        addSelectableChild(textField);
        textField.visible = false;
        textField.setEditable(false);

        super.init();

        int buttonsStyle = this.maxInputs > 3 ? TinkerStationButtonsWidget.METAL_STYLE : TinkerStationButtonsWidget.WOOD_STYLE;

        List<StationSlotLayout> layouts = Lists.newArrayList();
        // repair layout
        layouts.add(this.defaultLayout);
        // tool layouts
        layouts.addAll(StationSlotLayoutLoader.getInstance().getSortedSlots().stream()
                .filter(layout -> layout.getInputSlots().size() <= this.maxInputs).toList());

        this.buttonsScreen = new TinkerStationButtonsWidget(this, this.cornerX - TinkerStationButtonsWidget.width(COLUMN_COUNT) - 2,
                this.cornerY + this.centerBeam.h + this.buttonDecorationTop.h, layouts, buttonsStyle);

        this.updateLayout();
    }

    /**
     * Updates all slots for the current slot layout
     */
    public void updateLayout() {
        int stillFilled = 0;
        for (int i = 0; i <= maxInputs; i++) {
            Slot slot = this.getScreenHandler().getSlot(i);
            LayoutSlot layoutSlot = currentLayout.getSlot(i);
            if (layoutSlot.isHidden()) {
                // put the position in the still filled line
                slot.x = STILL_FILLED_X - STILL_FILLED_SPACING * stillFilled;
                slot.y = STILL_FILLED_Y;
                stillFilled++;
                if (slot instanceof TinkerStationSlot tinkerSlot) {
                    tinkerSlot.deactivate();
                }
            } else {
                slot.x = layoutSlot.getX();
                slot.y = layoutSlot.getY();
                if (slot instanceof TinkerStationSlot tinkerSlot) {
                    tinkerSlot.activate(layoutSlot);
                }
            }
        }

        this.updateDisplay();
    }

    /**
     * Updates the tool panel area
     */
    static void updateToolPanel(InfoPanelScreen tinkerInfo, ToolStack tool, ItemStack result) {
        if (tool.getItem() instanceof ITinkerStationDisplay display) {
            tinkerInfo.setCaption(display.getLocalizedName());
            tinkerInfo.setText(display.getStatInformation(tool, MinecraftClient.getInstance().player, new ArrayList<>(), SafeClientAccess.getTooltipKey(), TinkerTooltipFlags.TINKER_STATION));
        } else {
            tinkerInfo.setCaption(result.getName());
            List<Text> list = new ArrayList<>();
            result.getItem().appendTooltip(result, MinecraftClient.getInstance().world, list, Default.BASIC);
            tinkerInfo.setText(list);
        }
    }

    /**
     * Updates the modifier panel with relevant info
     */
    static void updateModifierPanel(InfoPanelScreen modifierInfo, ToolStack tool) {
        List<Text> modifierNames = new ArrayList<>();
        List<Text> modifierTooltip = new ArrayList<>();
        Text title;
        // control displays just traits, bit trickier to do
        if (hasControlDown()) {
            title = TRAITS_TEXT;
            Map<Modifier, Integer> upgrades = tool.getUpgrades().getModifiers().stream()
                    .collect(Collectors.toMap(ModifierEntry::getModifier, ModifierEntry::getLevel));
            for (ModifierEntry entry : tool.getModifierList()) {
                Modifier mod = entry.getModifier();
                if (mod.shouldDisplay(true)) {
                    int level = entry.getLevel() - upgrades.getOrDefault(mod, 0);
                    if (level > 0) {
                        ModifierEntry trait = new ModifierEntry(entry.getModifier(), level);
                        modifierNames.add(mod.getDisplayName(tool, trait));
                        modifierTooltip.add(mod.getDescription(tool, trait));
                    }
                }
            }
        } else {
            // shift is just upgrades/abilities, otherwise all
            List<ModifierEntry> modifiers;
            if (hasShiftDown()) {
                modifiers = tool.getUpgrades().getModifiers();
                title = UPGRADES_TEXT;
            } else {
                modifiers = tool.getModifierList();
                title = MODIFIERS_TEXT;
            }
            for (ModifierEntry entry : modifiers) {
                Modifier mod = entry.getModifier();
                if (mod.shouldDisplay(true)) {
                    modifierNames.add(mod.getDisplayName(tool, entry));
                    modifierTooltip.add(mod.getDescription(tool, entry));
                }
            }
        }

        modifierInfo.setCaption(title);
        modifierInfo.setText(modifierNames, modifierTooltip);
    }

    @Override
    public void updateDisplay() {
        if (this.tile == null) {
            return;
        }

        ItemStack toolStack = this.getScreenHandler().getResult();

        // if we have a message, display instead of refreshing the tool
        Text currentError = tile.getCurrentError();
        if (currentError != null) {
            error(currentError);
            return;
        }

        // only get to rename new tool in the station
        // anvil can rename on any tool change
        if (toolStack.isEmpty() || (tile.getInputCount() <= 4 && this.getScreenHandler().getSlot(TINKER_SLOT).hasStack())) {
            textField.setEditable(false);
            textField.setText("");
            textField.visible = false;
        } else if (!textField.isEditable()) {
            textField.setEditable(true);
            textField.setText("");
            textField.visible = true;
        } else {
            // ensure the text matches
            textField.setText(tile.getItemName());
        }

        // normal refresh
        if (toolStack.isEmpty()) {
            toolStack = this.getScreenHandler().getSlot(TINKER_SLOT).getStack();
        }

        // if the contained stack is modifiable, display some information
        if (toolStack.isIn(TinkerTags.Items.MODIFIABLE)) {
            ToolStack tool = ToolStack.from(toolStack);
            updateToolPanel(tinkerInfo, tool, toolStack);
            updateModifierPanel(modifierInfo, tool);
        }
        // tool build info
        else {
            this.tinkerInfo.setCaption(this.currentLayout.getDisplayName());
            this.tinkerInfo.setText(this.currentLayout.getDescription());

            // for each named slot, color the slot if the slot is filled
            // typically all input slots should be named, or none of them
            MutableText fullText = Text.literal("");
            boolean hasComponents = false;
            for (int i = 0; i <= activeInputs; i++) {
                LayoutSlot layout = currentLayout.getSlot(i);
                String key = layout.getTranslationKey();
                if (!layout.isHidden() && !key.isEmpty()) {
                    hasComponents = true;
                    MutableText textComponent = Text.literal(" * ");
                    ItemStack slotStack = this.getScreenHandler().getSlot(i).getStack();
                    if (!layout.isValid(slotStack)) {
                        textComponent.formatted(Formatting.RED);
                    }
                    textComponent.append(Text.translatable(key)).append("\n");
                    fullText.append(textComponent);
                }
            }
            // if we found any components, set the text, use the anvil if no components
            if (hasComponents) {
                this.modifierInfo.setCaption(COMPONENTS_TEXT);
                this.modifierInfo.setText(fullText);
            } else {
                this.modifierInfo.setCaption(Text.empty());
                this.modifierInfo.setText(ASCII_ANVIL);
            }
        }
    }

    @Override
    protected void drawContainerName(DrawContext context) {
        context.drawText(textRenderer, this.getTitle(), 8, 8, 4210752, false);
    }

    public static void renderIcon(DrawContext context, LayoutIcon icon, int x, int y) {
        Pattern pattern = icon.getValue(Pattern.class);
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (pattern != null) {
            // draw pattern sprite
            RenderUtils.setup(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            RenderSystem.applyModelViewMatrix();
            GuiUtil.renderPattern(context, pattern, x, y);
            return;
        }

        ItemStack stack = icon.getValue(ItemStack.class);
        if (stack != null) {
            context.drawItem(stack, x, y);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        this.drawBackground(context, TINKER_STATION_TEXTURE);

        int x = 0;
        int y = 0;

        // draw the item background
        final float scale = 3.7f;
        final float xOff = 12.5f;
        final float yOff = 22f;

        // render the background icon
        MatrixStack renderPose = RenderSystem.getModelViewStack();
        renderPose.push();
        renderPose.translate(xOff, yOff, 0.0F);
        renderPose.scale(scale, scale, 1.0f);
        renderIcon(context, currentLayout.getIcon(), (int) (this.cornerX / scale), (int) (this.cornerY / scale));
        renderPose.pop();
        RenderSystem.applyModelViewMatrix();

        // rebind gui texture since itemstack drawing sets it to something else
        RenderUtils.setup(TINKER_STATION_TEXTURE, 1.0f, 1.0f, 1.0f, 0.82f);
        RenderSystem.enableBlend();
        //RenderSystem.enableAlphaTest();
        //RenderHelper.turnOff();
        RenderSystem.disableDepthTest();
        ITEM_COVER.draw(context, this.cornerX + 7, this.cornerY + 18);

        // slot backgrounds, are transparent
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.28f);
        if (!this.currentLayout.getToolSlot().isHidden()) {
            Slot slot = this.getScreenHandler().getSlot(TINKER_SLOT);
            SLOT_BACKGROUND.draw(context, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
        }
        for (int i = 0; i < this.activeInputs; i++) {
            Slot slot = this.getScreenHandler().getSlot(i + INPUT_SLOT);
            SLOT_BACKGROUND.draw(context, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
        }

        // slot borders, are opaque
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        for (int i = 0; i <= maxInputs; i++) {
            Slot slot = this.getScreenHandler().getSlot(i);
            if ((slot instanceof TinkerStationSlot && (!((TinkerStationSlot) slot).isDormant() || slot.hasStack()))) {
                SLOT_BORDER.draw(context, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
            }
        }

        // sidebar beams
        x = this.buttonsScreen.getLeftPos() - this.leftBeam.w;
        y = this.cornerY;
        // draw the beams at the top
        x += this.leftBeam.draw(context, x, y);
        x += this.centerBeam.drawScaledX(context, x, y, this.buttonsScreen.getImageWidth());
        this.rightBeam.draw(context, x, y);

        x = tinkerInfo.x - this.leftBeam.w;
        x += this.leftBeam.draw(context, x, y);
        x += this.centerBeam.drawScaledX(context, x, y, this.tinkerInfo.backgroundWidth);
        this.rightBeam.draw(context, x, y);

        // draw the decoration for the buttons
        for (SlotButtonItem button : this.buttonsScreen.getButtons()) {
            this.buttonDecorationTop.draw(context, button.getX(), button.getY() - this.buttonDecorationTop.h);
            // don't draw the bottom for the buttons in the last row
            if (button.buttonId < this.buttonsScreen.getButtons().size() - COLUMN_COUNT) {
                this.buttonDecorationBot.draw(context, button.getX(), button.getY() + button.getHeight());
            }
        }

        // draw the decorations for the panels
        this.panelDecorationL.draw(context, this.tinkerInfo.x + 5, this.tinkerInfo.y - this.panelDecorationL.h);
        this.panelDecorationR.draw(context, this.tinkerInfo.guiRight() - 5 - this.panelDecorationR.w, this.tinkerInfo.y - this.panelDecorationR.h);
        this.panelDecorationL.draw(context, this.modifierInfo.x + 5, this.modifierInfo.y - this.panelDecorationL.h);
        this.panelDecorationR.draw(context, this.modifierInfo.guiRight() - 5 - this.panelDecorationR.w, this.modifierInfo.y - this.panelDecorationR.h);

        // render slot background icons
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        for (int i = 0; i <= maxInputs; i++) {
            Slot slot = this.getScreenHandler().getSlot(i);
            if (!slot.hasStack()) {
                Pattern icon = currentLayout.getSlot(i).getIcon();
                if (icon != null) {
                    GuiUtil.renderPattern(context, icon, this.cornerX + slot.x, this.cornerY + slot.y);
                }
            }
        }

        RenderSystem.enableDepthTest();

        super.drawBackground(context, partialTicks, mouseX, mouseY);

        this.buttonsScreen.render(context, mouseX, mouseY, partialTicks);

        // text field
        if (textField != null && textField.visible) {
            RenderUtils.setup(TINKER_STATION_TEXTURE, 1.0f, 1.0f, 1.0f, 1.0f);
            TEXT_BOX.draw(context, this.cornerX + 79, this.cornerY + 3);
            this.textField.render(context, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.tinkerInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) {
            return false;
        }

        if (this.modifierInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) {
            return false;
        }

        if (this.buttonsScreen.handleMouseClicked(mouseX, mouseY, mouseButton)) {
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick, double unkowwn) {
        if (this.tinkerInfo.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
            return false;
        }

        if (this.modifierInfo.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
            return false;
        }

        return super.mouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick, unkowwn);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.tinkerInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
            return false;
        }

        if (this.modifierInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
            return false;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (this.tinkerInfo.handleMouseReleased(mouseX, mouseY, state)) {
            return false;
        }

        if (this.modifierInfo.handleMouseReleased(mouseX, mouseY, state)) {
            return false;
        }

        if (this.buttonsScreen.handleMouseReleased(mouseX, mouseY, state)) {
            return false;
        }

        return super.mouseReleased(mouseX, mouseY, state);
    }

    /**
     * Returns true if a key changed that requires a display update
     */
    static boolean needsDisplayUpdate(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return true;
        }
        if (MinecraftClient.IS_SYSTEM_MAC) {
            return keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
        }
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        if (needsDisplayUpdate(keyCode)) {
            updateDisplay();
        }
        if (textField.isActive()) {
            textField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (needsDisplayUpdate(keyCode)) {
            updateDisplay();
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void drawSlot(DrawContext context, Slot slotIn) {
        // don't draw dormant slots with no item
        if (slotIn instanceof TinkerStationSlot && ((TinkerStationSlot) slotIn).isDormant() && !slotIn.hasStack()) {
            return;
        }
        super.drawSlot(context, slotIn);
    }

    @Override
    public boolean isPointOverSlot(Slot slotIn, double mouseX, double mouseY) {
        if (slotIn instanceof TinkerStationSlot && ((TinkerStationSlot) slotIn).isDormant() && !slotIn.hasStack()) {
            return false;
        }
        return super.isPointOverSlot(slotIn, mouseX, mouseY);
    }

    protected void wood() {
        this.tinkerInfo.wood();
        this.modifierInfo.wood();

        this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w, 0);
        this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w, 0);
        this.panelDecorationL = PANEL_SPACE_LEFT.shift(18, 0);
        this.panelDecorationR = PANEL_SPACE_RIGHT.shift(18, 0);

        this.leftBeam = LEFT_BEAM;
        this.rightBeam = RIGHT_BEAM;
        this.centerBeam = CENTER_BEAM;
    }

    protected void metal() {
        this.tinkerInfo.metal();
        this.modifierInfo.metal();

        this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w * 2, 0);
        this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w * 2, 0);
        this.panelDecorationL = PANEL_SPACE_LEFT.shift(18 * 2, 0);
        this.panelDecorationR = PANEL_SPACE_RIGHT.shift(18 * 2, 0);

        this.leftBeam = LEFT_BEAM.shift(0, LEFT_BEAM.h);
        this.rightBeam = RIGHT_BEAM.shift(0, RIGHT_BEAM.h);
        this.centerBeam = CENTER_BEAM.shift(0, CENTER_BEAM.h);
    }

    @Override
    public void error(Text message) {
        this.tinkerInfo.setCaption(COMPONENT_ERROR);
        this.tinkerInfo.setText(message);
        this.modifierInfo.setCaption(Text.empty());
        this.modifierInfo.setText(Text.empty());
    }

    @Override
    public void warning(Text message) {
        this.tinkerInfo.setCaption(COMPONENT_WARNING);
        this.tinkerInfo.setText(message);
        this.modifierInfo.setCaption(Text.empty());
        this.modifierInfo.setText(Text.empty());
    }

    /**
     * Called when a tool button is pressed
     *
     * @param layout Data of the slot selected
     */
    public void onToolSelection(StationSlotLayout layout) {
        this.activeInputs = Math.min(layout.getInputCount(), maxInputs);
        this.currentLayout = layout;
        this.updateLayout();

        // update the active slots and filter in the container
        // this.container.setToolSelection(layout); TODO: needed?
        TinkerNetwork.getInstance().sendToServer(new TinkerStationSelectionPacket(layout.getName()));
    }

    @Override
    public List<Rect2i> getModuleAreas() {
        List<Rect2i> list = super.getModuleAreas();
        list.add(this.buttonsScreen.getArea());
        return list;
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.isClickOutsideBounds(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && !this.buttonsScreen.isMouseOver(mouseX, mouseY);
    }


    /* Text field stuff */

    private void onNameChanged(String name) {
        if (tile != null) {
            this.tile.setItemName(name);
            TinkerNetwork.getInstance().sendToServer(new TinkerStationRenamePacket(name));
        }
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        this.textField.tick();
    }

    @Override
    public void resize(MinecraftClient pMinecraft, int pWidth, int pHeight) {
        String s = this.textField.getText();
        super.resize(pMinecraft, pWidth, pHeight);
        this.textField.setText(s);
    }
}
