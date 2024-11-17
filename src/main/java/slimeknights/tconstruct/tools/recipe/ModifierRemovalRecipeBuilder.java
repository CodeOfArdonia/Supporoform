package slimeknights.tconstruct.tools.recipe;

import com.mojang.datafixers.util.Function6;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for {@link ModifierRemovalRecipe} and {@link ExtractModifierRecipe}
 */
@RequiredArgsConstructor(staticName = "removal")
public class ModifierRemovalRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierRemovalRecipeBuilder> {
    private final Function6<Identifier, String, SizedIngredient, List<SizedIngredient>, List<ItemStack>, IJsonPredicate<ModifierId>, ModifierRemovalRecipe> constructor;
    private final List<ItemStack> leftovers = new ArrayList<>();
    @Accessors(chain = true)
    @Setter
    private String name = "modifiers";
    private SizedIngredient tools = ModifierRemovalRecipe.DEFAULT_TOOLS;
    @Accessors(fluent = true)
    @Setter
    private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;

    public static ModifierRemovalRecipeBuilder removal() {
        return removal(ModifierRemovalRecipe::new);
    }

    public static ModifierRemovalRecipeBuilder extract() {
        return removal(ExtractModifierRecipe::new);
    }

    /**
     * Sets the name from the given slot
     */
    public ModifierRemovalRecipeBuilder slotName(SlotType slot) {
        return this.setName(slot.getName());
    }

    /**
     * Sets the tool requirement for this recipe
     */
    public ModifierRemovalRecipeBuilder setTools(SizedIngredient ingredient) {
        this.tools = ingredient;
        return this;
    }

    /**
     * Sets the tool requirement for this recipe
     */
    public ModifierRemovalRecipeBuilder setTools(Ingredient ingredient) {
        return this.setTools(SizedIngredient.of(ingredient));
    }

    /**
     * Adds a leftover stack to the recipe
     */
    public ModifierRemovalRecipeBuilder addLeftover(ItemStack stack) {
        this.leftovers.add(stack);
        return this;
    }

    /**
     * Adds a leftover stack to the recipe
     */
    public ModifierRemovalRecipeBuilder addLeftover(ItemConvertible item) {
        return this.addLeftover(new ItemStack(item));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, Registry.ITEM.getKey(this.leftovers.get(0).getItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (this.inputs.isEmpty()) {
            throw new IllegalStateException("Must have at least one input");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "modifiers");
        consumer.accept(new LoadableFinishedRecipe<>(this.constructor.apply(id, this.name, this.tools, this.inputs, this.leftovers, this.modifierPredicate), ModifierRemovalRecipe.LOADER, advancementId));
    }
}
