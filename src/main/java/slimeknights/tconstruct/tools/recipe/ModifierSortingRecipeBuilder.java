package slimeknights.tconstruct.tools.recipe;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;

import java.util.function.Consumer;

/**
 * Builder for modifier sorting recipes
 */
@RequiredArgsConstructor(staticName = "sorting")
public class ModifierSortingRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierSortingRecipeBuilder> {

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, Registry.ITEM.getKey((this.inputs.get(0).getMatchingStacks().get(0).getItem())));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (this.inputs.isEmpty()) {
            throw new IllegalStateException("Must have at least one ingredient");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "modifiers");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierSortingRecipe(id, this.inputs), ModifierSortingRecipe.LOADER, advancementId));
    }
}
