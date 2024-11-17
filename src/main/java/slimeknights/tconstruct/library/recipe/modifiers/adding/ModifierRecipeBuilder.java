package slimeknights.tconstruct.library.recipe.modifiers.adding;

import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class ModifierRecipeBuilder extends AbstractModifierRecipeBuilder<ModifierRecipeBuilder> {
    protected final List<SizedIngredient> inputs = new ArrayList<>();

    protected ModifierRecipeBuilder(ModifierId result) {
        super(result);
    }

    /**
     * Creates a new recipe for 1 level of a modifier
     *
     * @param modifier Modifier
     * @return Recipe for 1 level of the modifier
     */
    public static ModifierRecipeBuilder modifier(ModifierId modifier) {
        return new ModifierRecipeBuilder(modifier);
    }

    /**
     * Creates a new recipe for 1 level of a modifier
     *
     * @param modifier Modifier
     * @return Recipe for 1 level of the modifier
     */
    public static ModifierRecipeBuilder modifier(LazyModifier modifier) {
        return modifier(modifier.getId());
    }


    /* Inputs */

    /**
     * Adds an input to the recipe
     *
     * @param ingredient Input
     * @return Builder instance
     */
    public ModifierRecipeBuilder addInput(SizedIngredient ingredient) {
        this.inputs.add(ingredient);
        return this;
    }

    /**
     * Adds an input to the recipe
     *
     * @param ingredient Input
     * @return Builder instance
     */
    public ModifierRecipeBuilder addInput(Ingredient ingredient) {
        return addInput(SizedIngredient.of(ingredient));
    }

    /**
     * Adds an input with the given amount, does not affect the salvage builder
     *
     * @param item   Item
     * @param amount Amount
     * @return Builder instance
     */
    public ModifierRecipeBuilder addInput(ItemConvertible item, int amount) {
        return addInput(SizedIngredient.fromItems(amount, item));
    }

    /**
     * Adds an input with a size of 1, does not affect the salvage builder
     *
     * @param item Item
     * @return Builder instance
     */
    public ModifierRecipeBuilder addInput(ItemConvertible item) {
        return addInput(item, 1);
    }

    /**
     * Adds an input to the recipe
     *
     * @param tag    Tag input
     * @param amount Amount required
     * @return Builder instance
     */
    public ModifierRecipeBuilder addInput(TagKey<Item> tag, int amount) {
        return addInput(SizedIngredient.fromTag(tag, amount));
    }

    /**
     * Adds an input to the recipe
     *
     * @param tag Tag input
     * @return Builder instance
     */
    public ModifierRecipeBuilder addInput(TagKey<Item> tag) {
        return addInput(tag, 1);
    }


    /* Building */

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (inputs.isEmpty() && !allowCrystal) {
            throw new IllegalStateException("Must have at least 1 input");
        }
        Identifier advancementId = buildOptionalAdvancement(id, "modifiers");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierRecipe(id, inputs, tools, maxToolSize, result, ModifierEntry.VALID_LEVEL.range(minLevel, maxLevel), slots, allowCrystal, checkTraitLevel), ModifierRecipe.LOADER, advancementId));
    }
}
