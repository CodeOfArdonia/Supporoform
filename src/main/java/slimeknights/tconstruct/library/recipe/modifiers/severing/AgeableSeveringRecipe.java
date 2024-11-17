package slimeknights.tconstruct.library.recipe.modifiers.severing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.tools.TinkerModifiers;

public class AgeableSeveringRecipe extends SeveringRecipe {
    /**
     * Loader instance
     */
    public static final RecordLoadable<AgeableSeveringRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(), ENTITY_FIELD,
            ItemOutput.Loadable.REQUIRED_STACK.requiredField("adult_result", r -> r.output),
            ItemOutput.Loadable.OPTIONAL_STACK.emptyField("child_result", r -> r.childOutput),
            AgeableSeveringRecipe::new);

    private final ItemOutput childOutput;

    public AgeableSeveringRecipe(Identifier id, EntityIngredient ingredient, ItemOutput adultOutput, ItemOutput childOutput) {
        super(id, ingredient, adultOutput);
        this.childOutput = childOutput;
    }

    @Override
    public ItemStack getOutput(Entity entity) {
        if (entity instanceof LivingEntity && ((LivingEntity) entity).isBaby()) {
            return childOutput.get().copy();
        }
        return getOutput().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.ageableSeveringSerializer.get();
    }
}
