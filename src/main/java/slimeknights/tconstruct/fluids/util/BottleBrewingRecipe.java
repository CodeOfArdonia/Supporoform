package slimeknights.tconstruct.fluids.util;

import io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;

/**
 * Recipe for transforming a bottle, depending on a vanilla brewing recipe to get the ingredient
 */
public class BottleBrewingRecipe extends BrewingRecipe {
    private final Item from;
    private final Item to;

    public BottleBrewingRecipe(Ingredient input, Item from, Item to, ItemStack output) {
        super(input, Ingredient.EMPTY, output);
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        for (BrewingRecipeRegistry.Recipe<Item> recipe : BrewingRecipeRegistry.ITEM_RECIPES) {
            if (recipe.input == this.from && recipe.output == this.to) {
                return recipe.ingredient.test(stack);
            }
        }
        return false;
    }

    @Override
    public Ingredient getIngredient() {
        for (BrewingRecipeRegistry.Recipe<Item> recipe : BrewingRecipeRegistry.ITEM_RECIPES) {
            if (recipe.input == this.from && recipe.output == this.to) {
                return recipe.ingredient;
            }
        }
        return Ingredient.EMPTY;
    }
}
