package slimeknights.tconstruct.tables.recipe;

import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;

import java.util.function.Consumer;

/**
 * Builder for tinker station damaging recipes
 */
@RequiredArgsConstructor(staticName = "damage")
public class TinkerStationDamagingRecipeBuilder extends AbstractRecipeBuilder<TinkerStationDamagingRecipeBuilder> {

    private final Ingredient ingredient;
    private final int damageAmount;

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        if (stacks.length == 0) {
            throw new IllegalStateException("Empty ingredient not allowed");
        }
        save(consumer, Registries.ITEM.getId(stacks[0].getItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (ingredient == Ingredient.EMPTY) {
            throw new IllegalStateException("Empty ingredient not allowed");
        }
        Identifier advancementId = buildOptionalAdvancement(id, "tinker_station");
        consumer.accept(new LoadableFinishedRecipe<>(new TinkerStationDamagingRecipe(id, this.ingredient, this.damageAmount), TinkerStationDamagingRecipe.LOADER, advancementId));
    }
}
