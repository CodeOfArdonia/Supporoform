package slimeknights.tconstruct.tables.client.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.block.entity.table.ModifierWorktableBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;
import slimeknights.tconstruct.tables.menu.ModifierWorktableContainerMenu;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

import java.util.Collections;
import java.util.List;

public class ModifierWorktableScreen extends BaseTabbedScreen<ModifierWorktableBlockEntity, ModifierWorktableContainerMenu> {
    protected static final Text TITLE = TConstruct.makeTranslation("gui", "modifier_worktable.title");
    protected static final Text TABLE_INFO = TConstruct.makeTranslation("gui", "modifier_worktable.info");

    private static final Identifier BACKGROUND = TConstruct.getResource("textures/gui/modifier_worktable.png");
    private static final Pattern[] INPUT_PATTERNS = {
            new Pattern(TConstruct.MOD_ID, "pickaxe"),
            new Pattern(TConstruct.MOD_ID, "ingot"),
            new Pattern(TConstruct.MOD_ID, "quartz")
    };

    /**
     * Side panels, for tools and modifiers
     */
    protected InfoPanelScreen tinkerInfo;
    protected InfoPanelScreen modifierInfo;

    /**
     * Current scrollbar position
     */
    private float sliderProgress = 0.0F;
    /**
     * Is {@code true} if the player clicked on the scroll wheel in the GUI
     */
    private boolean clickedOnScrollBar;

    /**
     * The index of the first recipe to display.
     * The number of recipes displayed at any time is 12 (4 recipes per row, and 3 rows). If the player scrolled down one
     * row, this value would be 4 (representing the index of the first slot on the second row).
     */
    private int modifierIndexOffset = 0;

    public ModifierWorktableScreen(ModifierWorktableContainerMenu container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);

        this.tinkerInfo = new InfoPanelScreen(this, container, playerInventory, title);
        this.tinkerInfo.setTextScale(8 / 9f);
        this.addModule(this.tinkerInfo);

        this.modifierInfo = new InfoPanelScreen(this, container, playerInventory, title);
        this.modifierInfo.setTextScale(7 / 9f);
        this.addModule(this.modifierInfo);

        this.tinkerInfo.yOffset = 0;
        this.modifierInfo.yOffset = this.tinkerInfo.backgroundHeight + 4;

        addChestSideInventory(playerInventory);
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        this.drawBackground(context, BACKGROUND);

        // draw scrollbar
        context.fill(this.cornerX + 103, this.cornerY + 15 + (int) (41.0F * this.sliderProgress), 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        this.drawModifierBackgrounds(context, mouseX, mouseY, this.cornerX + 28, this.cornerY + 15);

        // draw slot icons
        List<Slot> slots = this.getScreenHandler().getInputSlots();
        int max = Math.min(slots.size(), INPUT_PATTERNS.length);
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        for (int i = 0; i < max; i++) {
            this.drawIconEmpty(context, slots.get(i), INPUT_PATTERNS[i]);
        }
        this.drawModifierIcons(context, this.cornerX + 28, this.cornerY + 15);

        super.drawBackground(context, partialTicks, mouseX, mouseY);
    }

    /**
     * Gets the button at the given mouse location
     *
     * @param mouseX X position of button
     * @param mouseY Y position of button
     * @return Button index, or -1 if none
     */
    private int getButtonAt(int mouseX, int mouseY) {
        if (tile != null) {
            List<ModifierEntry> buttons = tile.getCurrentButtons();
            if (!buttons.isEmpty()) {
                int x = this.cornerX + 28;
                int y = this.cornerY + 15;
                int maxIndex = Math.min((this.modifierIndexOffset + 12), buttons.size());
                for (int l = this.modifierIndexOffset; l < maxIndex; ++l) {
                    int relative = l - this.modifierIndexOffset;
                    double buttonX = mouseX - (double) (x + relative % 4 * 18);
                    double buttonY = mouseY - (double) (y + relative / 4 * 18);
                    if (buttonX >= 0.0D && buttonY >= 0.0D && buttonX < 18.0D && buttonY < 18.0D) {
                        return l;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        // determime which button we are hovering
        if (tile != null) {
            List<ModifierEntry> buttons = tile.getCurrentButtons();
            if (!buttons.isEmpty()) {
                int index = getButtonAt(mouseX, mouseY);
                if (index >= 0) {
                    context.drawTooltip(this.textRenderer, buttons.get(index).getDisplayName(), mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Draw backgrounds for all modifiers
     */
    private void drawModifierBackgrounds(DrawContext context, int mouseX, int mouseY, int left, int top) {
        if (tile != null) {
            int selectedIndex = this.tile.getSelectedIndex();
            int max = Math.min(this.modifierIndexOffset + 12, this.getPartRecipeCount());
            for (int i = this.modifierIndexOffset; i < max; ++i) {
                int relative = i - this.modifierIndexOffset;
                int x = left + relative % 4 * 18;
                int y = top + (relative / 4) * 18;
                int u = this.backgroundHeight;
                if (i == selectedIndex) {
                    u += 18;
                } else if (mouseX >= x && mouseY >= y && mouseX < x + 18 && mouseY < y + 18) {
                    u += 36;
                }
                context.fill(x, y, 0, u, 18, 18);
            }
        }
    }

    /**
     * Draw slot icons for all patterns
     */
    private void drawModifierIcons(DrawContext context, int left, int top) {
        // use block texture list
        if (tile != null) {
            assert this.client != null;
            RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            // iterate all recipes
            List<ModifierEntry> list = this.tile.getCurrentButtons();
            int max = Math.min(this.modifierIndexOffset + 12, this.getPartRecipeCount());
            for (int i = this.modifierIndexOffset; i < max; ++i) {
                int relative = i - this.modifierIndexOffset;
                int x = left + relative % 4 * 18 + 1;
                int y = top + (relative / 4) * 18 + 1;
                ModifierIconManager.renderIcon(context, list.get(i).getModifier(), x, y, 100, 16);
            }
        }
    }

    @Override
    public void updateDisplay() {
        // if we can no longer scroll, reset scrollbar progress
        // fixes the case where we added an item and lost recipes
        if (!canScroll()) {
            this.sliderProgress = 0.0F;
            this.modifierIndexOffset = 0;
        }

        if (tile != null) {
            Text message = tile.getCurrentMessage();
            if (!message.getString().isEmpty()) {
                message(message);
                return;
            }

            LazyToolStack lazyResult = tile.getResult();
            if (lazyResult == null) {
                message(TABLE_INFO);
                return;
            }

            // reuse logic from tinker station for final result
            ToolStack result = lazyResult.getTool();
            ItemStack resultStack = lazyResult.getStack();
            TinkerStationScreen.updateToolPanel(tinkerInfo, result, resultStack);

            this.modifierInfo.setCaption(Text.empty());
            this.modifierInfo.setText(Text.empty());
            if (result.hasTag(TinkerTags.Items.MODIFIABLE)) {
                TinkerStationScreen.updateModifierPanel(modifierInfo, result);
            } else {
                // modifier crystals can show their modifier, along with anything else with a modifier there
                ModifierId modifierId = ModifierCrystalItem.getModifier(resultStack);
                if (modifierId != null) {
                    Modifier modifier = ModifierManager.getValue(modifierId);
                    modifierInfo.setCaption(TConstruct.makeTranslation("gui", "tinker_station.modifiers"));
                    modifierInfo.setText(Collections.singletonList(modifier.getDisplayName()), Collections.singletonList(modifier.getDescription()));
                }
            }
        }
    }


    /* Scrollbar logic */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.clickedOnScrollBar = false;
        if (this.tinkerInfo.handleMouseClicked(mouseX, mouseY, mouseButton)
                || this.modifierInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) {
            return false;
        }

        if (tile != null && !tile.getCurrentButtons().isEmpty()) {
            // handle button click
            int index = getButtonAt((int) mouseX, (int) mouseY);
            assert this.client != null && this.client.player != null;
            if (index >= 0 && this.getScreenHandler().onButtonClick(this.client.player, index)) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                assert this.client.interactionManager != null;
                this.client.interactionManager.clickButton(this.getScreenHandler().syncId, index);
                return true;
            }

            // scrollbar position
            int x = this.cornerX + 103;
            int y = this.cornerY + 15;
            if (mouseX >= x && mouseX < (x + 12) && mouseY >= y && mouseY < (y + 54)) {
                this.clickedOnScrollBar = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick, double unknown) {
        if (this.tinkerInfo.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
                || this.modifierInfo.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
            return false;
        }

        if (this.clickedOnScrollBar && this.canScroll()) {
            int i = this.cornerY + 14;
            int j = i + 54;
            this.sliderProgress = ((float) mouseY - i - 7.5F) / ((float) (j - i) - 15.0F);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.modifierIndexOffset = (int) ((this.sliderProgress * this.getHiddenRows()) + 0.5D) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick, unknown);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.tinkerInfo.handleMouseScrolled(mouseX, mouseY, delta)
                || this.modifierInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
            return false;
        }
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        if (this.canScroll()) {
            int i = this.getHiddenRows();
            this.sliderProgress = MathHelper.clamp((float) (this.sliderProgress - delta / i), 0.0F, 1.0F);
            this.modifierIndexOffset = (int) ((this.sliderProgress * (float) i) + 0.5f) * 4;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (this.tinkerInfo.handleMouseReleased(mouseX, mouseY, state)
                || this.modifierInfo.handleMouseReleased(mouseX, mouseY, state)) {
            return false;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }


    /* Update error logic */

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

    private Text getInfoTitle() {
        if (tile != null) {
            IModifierWorktableRecipe recipe = tile.getCurrentRecipe();
            if (recipe != null) {
                return recipe.getTitle();
            }
        }
        return TITLE;
    }

    /**
     * Displays a message with the default title
     */
    public void message(Text message) {
        this.tinkerInfo.setCaption(getInfoTitle());
        this.tinkerInfo.setText(message);
        this.modifierInfo.setCaption(Text.empty());
        this.modifierInfo.setText(Text.empty());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TinkerStationScreen.needsDisplayUpdate(keyCode)) {
            updateDisplay();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (TinkerStationScreen.needsDisplayUpdate(keyCode)) {
            updateDisplay();
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }


    /* Helpers */

    /**
     * Gets the number of part recipes
     */
    private int getPartRecipeCount() {
        return tile == null ? 0 : tile.getCurrentButtons().size();
    }

    /**
     * If true, we can scroll
     */
    private boolean canScroll() {
        return this.getPartRecipeCount() > 12;
    }

    /**
     * Gets the number of hidden part recipe rows
     */
    private int getHiddenRows() {
        return (this.getPartRecipeCount() + 4 - 1) / 4 - 3;
    }
}