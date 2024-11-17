package slimeknights.mantle.client.book.data.content;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.element.ImageData;
import slimeknights.mantle.client.book.data.element.IngredientData;
import slimeknights.mantle.client.book.data.element.TextData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.element.*;

import java.util.ArrayList;

import static slimeknights.mantle.client.screen.book.Textures.TEX_SMELTING;

public class ContentSmelting extends PageContent {
    public static final Identifier ID = Mantle.getResource("smelting");

    private static final DefaultedList<ItemStack> FUELS;

    public static final int TEX_SIZE = 128;
    public static final ImageData IMG_SMELTING = new ImageData(TEX_SMELTING, 0, 0, 110, 114, TEX_SIZE, TEX_SIZE);

    public static final int INPUT_X = 5;
    public static final int INPUT_Y = 5;
    public static final int RESULT_X = 74;
    public static final int RESULT_Y = 41;
    public static final int FUEL_X = 5;
    public static final int FUEL_Y = 77;

    public static final float ITEM_SCALE = 2.0F;

    @Getter
    public String title = "Smelting";
    public IngredientData input;
    public IngredientData result;
    public IngredientData fuel;
    public int cookTime = 200;
    public TextData[] description;
    public String recipe;

    @Override
    public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
        int x = BookScreen.PAGE_WIDTH / 2 - IMG_SMELTING.width / 2;

        int y;
        if (this.title == null || this.title.isEmpty()) {
            y = 0;
        } else {
            this.addTitle(list, this.title);
            y = this.getTitleHeight();
        }

        list.add(new ImageElement(x, y, IMG_SMELTING.width, IMG_SMELTING.height, IMG_SMELTING, book.appearance.slotColor));
        list.add(new TooltipElement(ImmutableList.of(Text.translatable("mantle:tooltip.cooktime", this.cookTime / 20)), x + 7, y + 42, 60, 28));

        if (this.input != null && !this.input.getItems().isEmpty()) {
            list.add(new ItemElement(x + INPUT_X, y + INPUT_Y, ITEM_SCALE, this.input.getItems(), this.input.action));
        }

        if (this.result != null && !this.result.getItems().isEmpty()) {
            list.add(new ItemElement(x + RESULT_X, y + RESULT_Y, ITEM_SCALE, this.result.getItems(), this.result.action));
        }

        list.add(new ItemElement(x + FUEL_X, y + FUEL_Y, ITEM_SCALE, this.getFuelsList()));

        if (this.description != null && this.description.length > 0) {
            list.add(new TextElement(0, IMG_SMELTING.height + y + 5, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - y - 5, this.description));
        }
    }

    public DefaultedList<ItemStack> getFuelsList() {
        //TODO ask JEI for fuel list if it is present
        if (this.fuel != null) {
            return this.fuel.getItems();
        }

        return FUELS;
    }

    @Override
    public void load() {
        super.load();

        if (!StringUtils.isEmpty(this.recipe) && Identifier.isValid(this.recipe)) {
            World level = MinecraftClient.getInstance().world;
            assert level != null;
            Recipe<?> recipe = level.getRecipeManager().get(new Identifier(this.recipe)).orElse(null);

            if (recipe instanceof AbstractCookingRecipe) {
                this.input = IngredientData.getItemStackData(DefaultedList.copyOf(ItemStack.EMPTY, recipe.getIngredients().get(0).getMatchingStacks()));
                this.cookTime = ((AbstractCookingRecipe) recipe).getCookTime();
                this.result = IngredientData.getItemStackData(recipe.getOutput(level.getRegistryManager()));
            }
        }
    }

    static {
        FUELS = DefaultedList.copyOf(ItemStack.EMPTY,
                new ItemStack(Blocks.OAK_SLAB),
                new ItemStack(Blocks.SPRUCE_SLAB),
                new ItemStack(Blocks.BIRCH_SLAB),
                new ItemStack(Blocks.JUNGLE_SLAB),
                new ItemStack(Blocks.ACACIA_SLAB),
                new ItemStack(Blocks.DARK_OAK_SLAB),
                new ItemStack(Blocks.OAK_PLANKS),
                new ItemStack(Blocks.SPRUCE_PLANKS),
                new ItemStack(Blocks.BIRCH_PLANKS),
                new ItemStack(Blocks.JUNGLE_PLANKS),
                new ItemStack(Blocks.ACACIA_PLANKS),
                new ItemStack(Blocks.DARK_OAK_PLANKS),
                new ItemStack(Blocks.COAL_BLOCK),
                new ItemStack(Items.WOODEN_PICKAXE),
                new ItemStack(Items.WOODEN_SWORD),
                new ItemStack(Items.WOODEN_HOE),
                new ItemStack(Items.STICK),
                new ItemStack(Items.COAL),
                new ItemStack(Items.LAVA_BUCKET),
                new ItemStack(Blocks.OAK_SAPLING),
                new ItemStack(Blocks.SPRUCE_SAPLING),
                new ItemStack(Blocks.BIRCH_SAPLING),
                new ItemStack(Blocks.JUNGLE_SAPLING),
                new ItemStack(Blocks.ACACIA_SAPLING),
                new ItemStack(Blocks.DARK_OAK_SAPLING),
                new ItemStack(Items.BLAZE_ROD),
                new ItemStack(Items.WOODEN_SHOVEL),
                new ItemStack(Items.WOODEN_AXE));
    }
}
