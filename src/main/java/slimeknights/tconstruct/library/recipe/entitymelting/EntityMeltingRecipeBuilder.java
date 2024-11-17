package slimeknights.tconstruct.library.recipe.entitymelting;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import java.util.function.Consumer;

/**
 * Builder for entity melting recipes
 */
@RequiredArgsConstructor(staticName = "melting")
public class EntityMeltingRecipeBuilder extends AbstractRecipeBuilder<EntityMeltingRecipeBuilder> {
    private final EntityIngredient ingredient;
    private final FluidStack output;
    private final int damage;

    /**
     * Creates a new builder doing 2 damage
     */
    public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output) {
        return melting(ingredient, output, 2);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, Registries.FLUID.getId(this.output.getFluid()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = this.buildOptionalAdvancement(id, "entity_melting");
        consumer.accept(new LoadableFinishedRecipe<>(new EntityMeltingRecipe(id, this.ingredient, this.output, this.damage), EntityMeltingRecipe.LOADER, advancementId));
    }
}
