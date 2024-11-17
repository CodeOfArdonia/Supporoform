package slimeknights.tconstruct.library.recipe.modifiers.adding;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;

import java.util.function.Consumer;

/**
 * Builder for overslime recipes
 */
@RequiredArgsConstructor(staticName = "modifier")
public class OverslimeModifierRecipeBuilder extends AbstractRecipeBuilder<OverslimeModifierRecipeBuilder> {
    private final Ingredient ingredient;
    private final int restoreAmount;

    /**
     * Creates a new builder for the given item
     */
    public static OverslimeModifierRecipeBuilder modifier(ItemConvertible item, int restoreAmount) {
        return modifier(Ingredient.ofItems(item), restoreAmount);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        if (stacks.length == 0) {
            throw new IllegalStateException("Empty ingredient not allowed");
        }
        save(consumer, Registries.ITEM.getId(stacks[0].getItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (ingredient == Ingredient.EMPTY) {
            throw new IllegalStateException("Empty ingredient not allowed");
        }
        Identifier advancementId = buildOptionalAdvancement(id, "modifiers");
        consumer.accept(new LoadableFinishedRecipe<>(new OverslimeModifierRecipe(id, this.ingredient, this.restoreAmount), OverslimeModifierRecipe.LOADER, advancementId));
    }
}
