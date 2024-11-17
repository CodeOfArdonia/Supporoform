package slimeknights.tconstruct.library.recipe.worktable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.function.Consumer;

/**
 * Builder for recipes to add or remove a modifier from a set in persistent data
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModifierSetWorktableRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierSetWorktableRecipeBuilder> {
    private final Identifier dataKey;
    @Setter
    @Accessors(fluent = true)
    private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;
    private final boolean addToSet;
    private Ingredient tools = AbstractWorktableRecipe.DEFAULT_TOOLS;
    private boolean allowTraits = false;

    /**
     * Creates a new recipe for adding to a set
     */
    public static ModifierSetWorktableRecipeBuilder setAdding(Identifier dataKey) {
        return new ModifierSetWorktableRecipeBuilder(dataKey, true);
    }

    /**
     * Creates a new recipe for removing from a set
     */
    public static ModifierSetWorktableRecipeBuilder setRemoving(Identifier dataKey) {
        return new ModifierSetWorktableRecipeBuilder(dataKey, false);
    }

    /**
     * Sets the tool requirement for this recipe
     */
    public ModifierSetWorktableRecipeBuilder setTools(Ingredient ingredient) {
        this.tools = ingredient;
        return this;
    }

    /**
     * Sets the tool requirement for this recipe
     */
    public ModifierSetWorktableRecipeBuilder setTools(TagKey<Item> tag) {
        return this.setTools(Ingredient.fromTag(tag));
    }

    /**
     * Sets the recipe to allow traits
     */
    public ModifierSetWorktableRecipeBuilder allowTraits() {
        this.allowTraits = true;
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, this.dataKey);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (this.inputs.isEmpty()) {
            throw new IllegalStateException("Must have at least one ingredient");
        }
        if (this.tools == Ingredient.EMPTY) {
            throw new IllegalStateException("Tools cannot be empty");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "modifiers");
        consumer.accept(new LoadableFinishedRecipe<>(new ModifierSetWorktableRecipe(id, this.dataKey, this.inputs, this.tools, this.modifierPredicate, this.addToSet, this.allowTraits), ModifierSetWorktableRecipe.LOADER, advancementId));
    }
}
