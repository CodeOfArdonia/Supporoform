package slimeknights.tconstruct.library.recipe.casting.material;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builder to make parts and composites castable
 */
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "material")
public class MaterialFluidRecipeBuilder extends AbstractRecipeBuilder<MaterialFluidRecipeBuilder> {
    /**
     * Output material ID
     */
    private final MaterialVariantId outputId;
    /**
     * Fluid used for casting
     */
    @Setter
    private FluidIngredient fluid = FluidIngredient.EMPTY;
    /**
     * Temperature for cooling time calculations
     */
    @Setter
    private int temperature = -1;
    /**
     * Material base for composite
     */
    @Setter
    @Nullable
    private MaterialVariantId inputId;

    /**
     * Sets the fluid for this recipe, and cooling time if unset.
     *
     * @param fluidStack Fluid input
     * @return Builder instance
     */
    public MaterialFluidRecipeBuilder setFluidAndTemp(FluidStack fluidStack) {
        this.fluid = FluidIngredient.of(fluidStack);
        if (this.temperature == -1) {
            this.temperature = fluidStack.getFluid().getFluidType().getTemperature(fluidStack) - 300;
        }
        return this;
    }

    /**
     * Sets the fluid for this recipe, and cooling time
     *
     * @param tagIn  Tag<Fluid> instance
     * @param amount Fluid amount
     */
    public MaterialFluidRecipeBuilder setFluid(TagKey<Fluid> tagIn, int amount) {
        this.setFluid(FluidIngredient.of(tagIn, amount));
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, this.outputId.getId());
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (this.fluid == FluidIngredient.EMPTY) {
            throw new IllegalStateException("Material fluid recipes require a fluid input");
        }
        if (this.temperature < 0) {
            throw new IllegalStateException("Temperature is too low, must be at least 0");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "materials");
        consumer.accept(new LoadableFinishedRecipe<>(new MaterialFluidRecipe(id, this.fluid, this.temperature, this.inputId, this.outputId), MaterialFluidRecipe.LOADER, advancementId));
    }
}
