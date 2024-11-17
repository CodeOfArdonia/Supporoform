package slimeknights.tconstruct.tables.client.inventory;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.Icons;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu;

import java.util.List;
import java.util.function.Function;

public class PartBuilderScreen extends BaseTabbedScreen<PartBuilderBlockEntity, PartBuilderContainerMenu> {
    private static final Text INFO_TEXT = TConstruct.makeTranslation("gui", "part_builder.info");
    private static final Text TRAIT_TITLE = TConstruct.makeTranslation("gui", "part_builder.trait").formatted(Formatting.UNDERLINE);
    private static final MutableText UNCRAFTABLE_MATERIAL = TConstruct.makeTranslation("gui", "part_builder.uncraftable").formatted(Formatting.RED);
    private static final MutableText UNCRAFTABLE_MATERIAL_TOOLTIP = TConstruct.makeTranslation("gui", "part_builder.uncraftable.tooltip");

    private static final Identifier BACKGROUND = TConstruct.getResource("textures/gui/partbuilder.png");

    /**
     * Part builder side panel
     */
    protected PartInfoPanelScreen infoPanelScreen;
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
    private int recipeIndexOffset = 0;

    public PartBuilderScreen(PartBuilderContainerMenu container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);

        this.infoPanelScreen = new PartInfoPanelScreen(this, container, playerInventory, title);
        this.infoPanelScreen.setTextScale(7 / 9f);
        this.infoPanelScreen.backgroundHeight = this.backgroundHeight;
        this.addModule(this.infoPanelScreen);
        addChestSideInventory(playerInventory);
    }

    @Override
    protected void drawBackground(DrawContext context, float partialTicks, int mouseX, int mouseY) {
        this.drawBackground(context, BACKGROUND);

        // draw scrollbar
        context.fill(this.cornerX + 126, this.cornerY + 15 + (int) (41.0F * this.sliderProgress), 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        this.drawRecipesBackground(context, mouseX, mouseY, this.cornerX + 51, this.cornerY + 15);

        // draw slot icons
        this.drawIconEmpty(context, this.getScreenHandler().getPatternSlot(), Icons.PATTERN);
        this.drawIconEmpty(context, this.getScreenHandler().getInputSlot(), Icons.INGOT);
        this.drawRecipesItems(context, this.cornerX + 51, this.cornerY + 15);

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
        List<Pattern> buttons = tile.getSortedButtons();
        if (!buttons.isEmpty()) {
            int x = this.cornerX + 51;
            int y = this.cornerY + 15;
            int maxIndex = Math.min((this.recipeIndexOffset + 12), buttons.size());
            for (int l = this.recipeIndexOffset; l < maxIndex; ++l) {
                int relative = l - this.recipeIndexOffset;
                double buttonX = mouseX - (double) (x + relative % 4 * 18);
                double buttonY = mouseY - (double) (y + relative / 4 * 18);
                if (buttonX >= 0.0D && buttonY >= 0.0D && buttonX < 18.0D && buttonY < 18.0D) {
                    return l;
                }
            }
        }
        return -1;
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        // determime which button we are hovering
        List<Pattern> buttons = tile.getSortedButtons();
        if (!buttons.isEmpty()) {
            int index = getButtonAt(mouseX, mouseY);
            if (index >= 0) {
                context.drawTooltip(textRenderer, buttons.get(index).getDisplayName(), mouseX, mouseY);
            }
        }
    }

    /**
     * Draw backgrounds for all patterns
     */
    private void drawRecipesBackground(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int max = Math.min(this.recipeIndexOffset + 12, this.getPartRecipeCount());
        for (int i = this.recipeIndexOffset; i < max; ++i) {
            int relative = i - this.recipeIndexOffset;
            int x = left + relative % 4 * 18;
            int y = top + (relative / 4) * 18;
            int u = this.backgroundHeight;
            if (i == this.tile.getSelectedIndex()) {
                u += 18;
            } else if (mouseX >= x && mouseY >= y && mouseX < x + 18 && mouseY < y + 18) {
                u += 36;
            }
            context.fill(x, y, 0, u, 18, 18);
        }
    }

    /**
     * Draw slot icons for all patterns
     */
    private void drawRecipesItems(DrawContext context, int left, int top) {
        // use block texture list
        assert this.client != null;
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        Function<Identifier, Sprite> spriteGetter = this.client.getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        // iterate all recipes
        List<Pattern> list = this.tile.getSortedButtons();
        int max = Math.min(this.recipeIndexOffset + 12, this.getPartRecipeCount());
        for (int i = this.recipeIndexOffset; i < max; ++i) {
            int relative = i - this.recipeIndexOffset;
            int x = left + relative % 4 * 18 + 1;
            int y = top + (relative / 4) * 18 + 1;
            // get the sprite for the pattern and draw
            Pattern pattern = list.get(i);
            Sprite sprite = spriteGetter.apply(pattern.getTexture());
            context.drawSprite(x, y, 100, 16, 16, sprite);
        }
    }

    @Override
    public void updateDisplay() {
        // if we can no longer scroll, reset scrollbar progress
        // fixes the case where we added an item and lost recipes
        if (!canScroll()) {
            this.sliderProgress = 0.0F;
            this.recipeIndexOffset = 0;
        }

        assert this.tile != null;

        // update material
        IMaterialValue materialRecipe = this.tile.getMaterialRecipe();
        if (materialRecipe != null) {
            this.setDisplayForMaterial(materialRecipe);
        } else {
            // default text
            this.infoPanelScreen.setCaption(this.getTitle());
            this.infoPanelScreen.setText(INFO_TEXT);
            this.infoPanelScreen.clearMaterialValue();
        }

        // update part recipe cost
        IPartBuilderRecipe partRecipe = this.tile.getPartRecipe();
        boolean skipCost = false;
        if (partRecipe == null) {
            partRecipe = this.tile.getFirstRecipe();
            skipCost = true;
        }
        if (partRecipe != null) {
            int cost = partRecipe.getCost();
            if (cost > 0 && !skipCost) {
                this.infoPanelScreen.setPatternCost(cost);
            } else {
                this.infoPanelScreen.clearPatternCost();
            }
            Text title = partRecipe.getTitle();
            if (title != null) {
                this.infoPanelScreen.setCaption(title);
                this.infoPanelScreen.setText(partRecipe.getText(this.tile.getInventoryWrapper()));
            }
        } else {
            this.infoPanelScreen.clearPatternCost();
        }
    }

    /**
     * Updates the data in the material display
     *
     * @param materialRecipe New material recipe
     */
    private void setDisplayForMaterial(IMaterialValue materialRecipe) {
        MaterialVariant materialVariant = materialRecipe.getMaterial();
        this.infoPanelScreen.setCaption(MaterialTooltipCache.getColoredDisplayName(materialVariant.getVariant()));

        // determine how much material we have
        // get exact number of material, rather than rounded
        float value = materialRecipe.getMaterialValue(this.tile.getInventoryWrapper());
        MutableText formatted = Text.literal(Util.COMMA_FORMAT.format(value));

        // if we have a part recipe, mark material red when not enough
        IPartBuilderRecipe partRecipe = this.tile.getPartRecipe();
        if (partRecipe != null && value < partRecipe.getCost()) {
            formatted = formatted.formatted(Formatting.DARK_RED);
        }
        this.infoPanelScreen.setMaterialValue(formatted);

        // update stats and traits
        List<Text> stats = Lists.newLinkedList();
        List<Text> tips = Lists.newArrayList();

        // add warning that the material is uncraftable
        if (!materialVariant.get().isCraftable()) {
            stats.add(UNCRAFTABLE_MATERIAL);
            stats.add(Text.empty());
            tips.add(UNCRAFTABLE_MATERIAL_TOOLTIP);
            tips.add(Text.empty());
        }

        MaterialId id = materialVariant.getId();
        for (IMaterialStats stat : MaterialRegistry.getInstance().getAllStats(id)) {
            List<Text> info = stat.getLocalizedInfo();

            if (!info.isEmpty()) {
                stats.add(stat.getLocalizedName().formatted(Formatting.UNDERLINE));
                tips.add(Text.empty());

                stats.addAll(info);
                tips.addAll(stat.getLocalizedDescriptions());

                List<ModifierEntry> traits = MaterialRegistry.getInstance().getTraits(id, stat.getIdentifier());
                if (!traits.isEmpty()) {
                    for (ModifierEntry trait : traits) {
                        Modifier mod = trait.getModifier();
                        stats.add(mod.getDisplayName(trait.getLevel()));
                        tips.add(mod.getDescription(trait.getLevel()));
                    }
                }

                stats.add(Text.empty());
                tips.add(Text.empty());
            }
        }

        // remove last line if empty
        if (!stats.isEmpty() && stats.get(stats.size() - 1).getString().isEmpty()) {
            stats.remove(stats.size() - 1);
            tips.remove(tips.size() - 1);
        }

        this.infoPanelScreen.setText(stats, tips);
    }


    /* Scrollbar logic */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.clickedOnScrollBar = false;

        if (this.infoPanelScreen.handleMouseClicked(mouseX, mouseY, mouseButton)) {
            return false;
        }

        List<Pattern> buttons = tile.getSortedButtons();
        if (!buttons.isEmpty()) {
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
            int x = this.cornerX + 126;
            int y = this.cornerY + 15;
            if (mouseX >= x && mouseX < (x + 12) && mouseY >= y && mouseY < (y + 54)) {
                this.clickedOnScrollBar = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick, double unknown) {
        if (this.infoPanelScreen.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
            return false;
        }

        if (this.clickedOnScrollBar && this.canScroll()) {
            int i = this.cornerY + 14;
            int j = i + 54;
            this.sliderProgress = ((float) mouseY - i - 7.5F) / ((float) (j - i) - 15.0F);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((this.sliderProgress * this.getHiddenRows()) + 0.5D) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick, unknown);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        //if (this.infoPanelScreen.handleMouseScrolled(mouseX, mouseY, delta)) {
        //  return false;
        //}
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        if (this.canScroll()) {
            int i = this.getHiddenRows();
            this.sliderProgress = MathHelper.clamp((float) (this.sliderProgress - delta / i), 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((this.sliderProgress * (float) i) + 0.5f) * 4;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (this.infoPanelScreen.handleMouseReleased(mouseX, mouseY, state)) {
            return false;
        }

        return super.mouseReleased(mouseX, mouseY, state);
    }


    /* Update error logic */

    @Override
    public void error(Text message) {
        this.infoPanelScreen.setCaption(COMPONENT_ERROR);
        this.infoPanelScreen.setText(message);
    }

    @Override
    public void warning(Text message) {
        this.infoPanelScreen.setCaption(COMPONENT_WARNING);
        this.infoPanelScreen.setText(message);
    }


    /* Helpers */

    /**
     * Gets the number of part recipes
     */
    private int getPartRecipeCount() {
        return tile.getSortedButtons().size();
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
