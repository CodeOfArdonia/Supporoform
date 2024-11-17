package slimeknights.tconstruct.library.recipe.modifiers.severing;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builder for entity melting recipes
 */
@RequiredArgsConstructor(staticName = "severing")
public class SeveringRecipeBuilder extends AbstractRecipeBuilder<SeveringRecipeBuilder> {
    private final EntityIngredient ingredient;
    private final ItemOutput output;
    @Nullable
    private ItemOutput childOutput = null;

    /**
     * Creates a new builder from an item
     */
    public static SeveringRecipeBuilder severing(EntityIngredient ingredient, ItemConvertible output) {
        return SeveringRecipeBuilder.severing(ingredient, ItemOutput.fromItem(output));
    }

    /**
     * Makes this an ageable severing recipe
     *
     * @param childOutput Output when a child, if empty just does no output for children
     * @return Builder instance
     */
    public SeveringRecipeBuilder setChildOutput(ItemOutput childOutput) {
        this.childOutput = childOutput;
        return this;
    }

    /**
     * Makes this an ageable severing recipe with no child output
     *
     * @return Builder instance
     */
    public SeveringRecipeBuilder noChildOutput() {
        return setChildOutput(ItemOutput.EMPTY);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        save(consumer, Registries.ITEM.getId(this.output.get().getItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = this.buildOptionalAdvancement(id, "severing");
        if (this.childOutput != null) {
            consumer.accept(new LoadableFinishedRecipe<>(new AgeableSeveringRecipe(id, this.ingredient, this.output, this.childOutput), AgeableSeveringRecipe.LOADER, advancementId));
        } else {
            consumer.accept(new LoadableFinishedRecipe<>(new SeveringRecipe(id, this.ingredient, this.output), SeveringRecipe.LOADER, advancementId));
        }
    }
}
