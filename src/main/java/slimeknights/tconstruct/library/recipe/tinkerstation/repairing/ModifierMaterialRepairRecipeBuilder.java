package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

import java.util.function.Consumer;

/**
 * Builds a recipe to repair a tool using a modifier via a material
 */
@RequiredArgsConstructor(staticName = "repair")
public class ModifierMaterialRepairRecipeBuilder extends AbstractRecipeBuilder<ModifierMaterialRepairRecipeBuilder> {
    private final ModifierId modifier;
    private final MaterialId material;
    private final MaterialStatsId statType;

    public static ModifierMaterialRepairRecipeBuilder repair(LazyModifier modifier, MaterialId material, MaterialStatsId statType) {
        return repair(modifier.getId(), material, statType);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        save(consumer, modifier);
    }

    /**
     * Builds the recipe for the crafting table using a repair kit
     */
    public ModifierMaterialRepairRecipeBuilder saveCraftingTable(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = buildOptionalAdvancement(id, "tinker_station");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierMaterialRepairKitRecipe(id, modifier, material, statType), ModifierMaterialRepairKitRecipe.LOADER, advancementId));
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = buildOptionalAdvancement(id, "tinker_station");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierMaterialRepairRecipe(id, this.modifier, this.material, this.statType), ModifierMaterialRepairRecipe.LOADER, advancementId));
    }
}
