package slimeknights.tconstruct.tools.recipe;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;

import java.util.function.Consumer;

/**
 * Builder for an enchantment converting recipe
 */
@RequiredArgsConstructor(staticName = "converting")
public class EnchantmentConvertingRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<EnchantmentConvertingRecipeBuilder> {
    private final String name;
    private final boolean matchBook;
    private boolean returnInput = false;
    @Setter
    @Accessors(fluent = true)
    private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;

    /**
     * If true, returns the unenchanted form of the item as an extra result
     */
    public EnchantmentConvertingRecipeBuilder returnInput() {
        this.returnInput = true;
        return this;
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, TConstruct.getResource(this.name));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (this.inputs.isEmpty()) {
            throw new IllegalStateException("Must have at least one input");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "modifiers");
        consumer.accept(new LoadableFinishedRecipe<>(new EnchantmentConvertingRecipe(id, this.name, this.inputs, this.matchBook, this.returnInput, this.modifierPredicate), EnchantmentConvertingRecipe.LOADER, advancementId));
    }
}
