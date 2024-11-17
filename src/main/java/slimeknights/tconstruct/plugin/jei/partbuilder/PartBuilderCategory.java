package slimeknights.tconstruct.plugin.jei.partbuilder;

import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.tools.layout.Patterns;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.tables.TinkerTables;

import java.awt.Color;

public class PartBuilderCategory implements IRecipeCategory<IDisplayPartBuilderRecipe> {
    private static final Identifier BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");
    private static final Text TITLE = TConstruct.makeTranslation("jei", "part_builder.title");
    private static final String KEY_COST = TConstruct.makeTranslationKey("jei", "part_builder.cost");

    @Getter
    private final IDrawable background;
    @Getter
    private final IDrawable icon;

    public PartBuilderCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(BACKGROUND_LOC, 0, 117, 121, 46);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(TinkerTables.partBuilder));
    }

    @Override
    public RecipeType<IDisplayPartBuilderRecipe> getRecipeType() {
        return TConstructJEIConstants.PART_BUILDER;
    }

    @Override
    public Text getTitle() {
        return TITLE;
    }

    @Override
    public void draw(IDisplayPartBuilderRecipe recipe, IRecipeSlotsView slots, MatrixStack matrixStack, double mouseX, double mouseY) {
        MaterialVariant variant = recipe.getMaterial();
        if (!variant.isEmpty()) {
            TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
            Text name = MaterialTooltipCache.getColoredDisplayName(variant.getVariant());
            fontRenderer.drawShadow(matrixStack, name, 3, 2, -1);
            String coolingString = I18n.translate(KEY_COST, recipe.getCost());
            fontRenderer.draw(matrixStack, coolingString, 3, 35, Color.GRAY.getRGB());
        } else {
            RenderUtils.setup(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            GuiUtil.renderPattern(matrixStack, Patterns.INGOT, 25, 16);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IDisplayPartBuilderRecipe recipe, IFocusGroup focuses) {
        // items
        MaterialVariant material = recipe.getMaterial();
        if (!material.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 25, 16).addItemStacks(MaterialItemList.getItems(material.getVariant()));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 16).addItemStacks(recipe.getPatternItems());
        // patterns
        builder.addSlot(RecipeIngredientRole.INPUT, 46, 16).addIngredient(TConstructJEIConstants.PATTERN_TYPE, recipe.getPattern());
        // TODO: material input?

        // output
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 15).addItemStack(recipe.getOutput());
    }
}
