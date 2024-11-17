package slimeknights.tconstruct.library.recipe.melting;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.function.Consumer;

/**
 * Builder for a recipe to melt a dynamic part material item
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MaterialMeltingRecipeBuilder extends AbstractRecipeBuilder<MaterialMeltingRecipeBuilder> {
    private final MaterialVariantId inputId;
    private final int temperature;
    private final FluidStack result;

    /**
     * Creates a recipe using the fluids temperature
     */
    public static MaterialMeltingRecipeBuilder material(MaterialVariantId materialId, int temperature, FluidStack result) {
        if (temperature < 0) {
            throw new IllegalArgumentException("Invalid temperature " + temperature + ", must be 0 or greater");
        }
        return new MaterialMeltingRecipeBuilder(materialId, temperature, result);
    }

    /**
     * Creates a recipe using the fluids temperature
     */
    public static MaterialMeltingRecipeBuilder material(MaterialVariantId materialId, FluidStack result) {
        return material(materialId, result.getFluid().getFluidType().getTemperature(result) - 300, result);
    }

    /**
     * Creates a recipe using the fluids temperature
     */
    public static MaterialMeltingRecipeBuilder material(MaterialId materialId, Fluid result, int amount) {
        return material(materialId, new FluidStack(result, amount));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, this.inputId.getId());
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementID = this.buildOptionalAdvancement(id, "melting");
        consumer.accept(new LoadableFinishedRecipe<>(new MaterialMeltingRecipe(id, this.inputId, this.temperature, this.result), MaterialMeltingRecipe.LOADER, advancementID));
    }
}
