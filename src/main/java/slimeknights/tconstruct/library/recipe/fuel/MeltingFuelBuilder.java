package slimeknights.tconstruct.library.recipe.fuel;

import lombok.AllArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.function.Consumer;

/**
 * Builds a new recipe for a melter or smeltery fuel
 */
@AllArgsConstructor(staticName = "fuel")
public class MeltingFuelBuilder extends AbstractRecipeBuilder<MeltingFuelBuilder> {
    private final FluidIngredient input;
    private final int duration;
    private final int temperature;

    /**
     * Creates a new builder instance with automatic temperature
     *
     * @param fluid    Fluid stack
     * @param duration Fluid duration
     * @return Builder instance
     */
    public static MeltingFuelBuilder fuel(FluidStack fluid, int duration) {
        return fuel(FluidIngredient.of(fluid), duration, fluid.getFluid().getFluidType().getTemperature(fluid) - 300);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        if (input.getFluids().isEmpty()) {
            throw new IllegalStateException("Must have at least one fluid for dynamic input");
        }
        save(consumer, Registries.FLUID.getId(input.getFluids().get(0).getFluid()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = this.buildOptionalAdvancement(id, "melting_fuel");
        consumer.accept(new LoadableFinishedRecipe<>(new MeltingFuel(id, group, this.input, this.duration, this.temperature), MeltingFuel.LOADER, advancementId));
    }
}
