package slimeknights.tconstruct.library.recipe.partbuilder;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

import java.util.function.Consumer;

@RequiredArgsConstructor(staticName = "item")
public class ItemPartRecipeBuilder extends AbstractRecipeBuilder<ItemPartRecipeBuilder> {
    private final Identifier pattern;
    private final ItemOutput result;
    @Setter
    @Accessors(chain = true)
    private Ingredient patternItem = IPartBuilderRecipe.DEFAULT_PATTERNS;
    private MaterialId materialId = IMaterial.UNKNOWN_ID;
    private int cost = 0;

    /**
     * Sets the material Id and cost
     */
    public ItemPartRecipeBuilder material(MaterialId material, int cost) {
        this.materialId = material;
        this.cost = cost;
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        save(consumer, Registries.ITEM.getId(result.get().getItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = buildOptionalAdvancement(id, "parts");
        consumer.accept(new LoadableFinishedRecipe<>(new ItemPartRecipe(id, this.materialId, new Pattern(this.pattern), this.patternItem, this.cost, this.result), ItemPartRecipe.LOADER, advancementId));
    }
}
