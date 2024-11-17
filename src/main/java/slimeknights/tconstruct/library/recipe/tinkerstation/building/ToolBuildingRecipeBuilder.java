package slimeknights.tconstruct.library.recipe.tinkerstation.building;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for a recipe that builds a tool
 */
@RequiredArgsConstructor(staticName = "toolBuildingRecipe")
@Accessors(fluent = true)
public class ToolBuildingRecipeBuilder extends AbstractRecipeBuilder<ToolBuildingRecipeBuilder> {
    private final IModifiable output;
    @Setter
    private int outputSize = 1;
    @Nullable
    @Setter
    private Identifier layoutSlot = null;
    private final List<Ingredient> extraRequirements = new ArrayList<>();

    /**
     * Adds an extra ingredient requirement
     */
    public ToolBuildingRecipeBuilder addExtraRequirement(Ingredient ingredient) {
        extraRequirements.add(ingredient);
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumerIn) {
        this.save(consumerIn, Registries.ITEM.getId(this.output.asItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumerIn, Identifier id) {
        Identifier advancementId = this.buildOptionalAdvancement(id, "parts");
        consumerIn.accept(new LoadableFinishedRecipe<>(new ToolBuildingRecipe(id, group, this.output, this.outputSize, this.layoutSlot, this.extraRequirements), ToolBuildingRecipe.LOADER, advancementId));
    }
}
