package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

import java.util.function.Consumer;

/**
 * Builds a recipe to repair a tool using a modifier
 */
@RequiredArgsConstructor(staticName = "repair")
public class ModifierRepairRecipeBuilder extends AbstractRecipeBuilder<ModifierRepairRecipeBuilder> {
    private final ModifierId modifier;
    private final Ingredient ingredient;
    private final int repairAmount;

    public static ModifierRepairRecipeBuilder repair(LazyModifier modifier, Ingredient ingredient, int repairAmount) {
        return repair(modifier.getId(), ingredient, repairAmount);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        save(consumer, modifier);
    }

    /**
     * Builds the recipe for the crafting table using a repair kit
     */
    public ModifierRepairRecipeBuilder buildCraftingTable(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = buildOptionalAdvancement(id, "tinker_station");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierRepairCraftingRecipe(id, modifier, ingredient, repairAmount), ModifierRepairCraftingRecipe.LOADER, advancementId));
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = buildOptionalAdvancement(id, "tinker_station");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierRepairTinkerStationRecipe(id, this.modifier, this.ingredient, this.repairAmount), ModifierRepairTinkerStationRecipe.LOADER, advancementId));
    }
}
