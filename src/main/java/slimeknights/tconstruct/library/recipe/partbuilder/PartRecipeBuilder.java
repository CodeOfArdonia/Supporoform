package slimeknights.tconstruct.library.recipe.partbuilder;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.function.Consumer;

/**
 * Builder for a material item part crafting recipe
 */
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "partRecipe")
public class PartRecipeBuilder extends AbstractRecipeBuilder<PartRecipeBuilder> {
    private final IMaterialItem output;
    private final int outputAmount;
    @Setter
    private int cost = 1;
    @Setter
    private Identifier pattern = null;
    @Setter
    private Ingredient patternItem = IPartBuilderRecipe.DEFAULT_PATTERNS;

    /**
     * Creates a new part recipe that outputs a single item
     *
     * @param output Output item
     * @return Builder instance
     */
    public static PartRecipeBuilder partRecipe(IMaterialItem output) {
        return partRecipe(output, 1);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumerIn) {
        this.save(consumerIn, Registries.ITEM.getId(this.output.asItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumerIn, Identifier id) {
        if (this.outputAmount <= 0) {
            throw new IllegalStateException("recipe " + id + " must output at least 1");
        }
        if (this.cost <= 0) {
            throw new IllegalStateException("recipe " + id + " has no cost associated with it");
        }
        if (this.pattern == null) {
            throw new IllegalStateException("recipe " + id + " has no pattern associated with it");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "parts");
        consumerIn.accept(new LoadableFinishedRecipe<>(new PartRecipe(id, group, new Pattern(this.pattern), this.patternItem, this.cost, this.output, this.outputAmount), PartRecipe.LOADER, advancementId));
    }
}
