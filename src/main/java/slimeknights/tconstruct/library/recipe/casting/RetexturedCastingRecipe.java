package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.item.RetexturedBlockItem;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

/**
 * Extension of item recipe that sets the result block to the input block
 */
public class RetexturedCastingRecipe extends ItemCastingRecipe {
    /**
     * Loader instance
     */
    public static final RecordLoadable<RetexturedCastingRecipe> LOADER = RecordLoadable.create(
            LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(),
            LoadableRecipeSerializer.RECIPE_GROUP, CAST_FIELD, FLUID_FIELD, RESULT_FIELD, COOLING_TIME_FIELD, CAST_CONSUMED_FIELD, SWITCH_SLOTS_FIELD,
            RetexturedCastingRecipe::new);

    public RetexturedCastingRecipe(TypeAwareRecipeSerializer<?> serializer, Identifier id, String group, Ingredient cast, FluidIngredient fluid, ItemOutput result, int coolingTime, boolean consumed, boolean switchSlots) {
        super(serializer, id, group, cast, fluid, result, coolingTime, consumed, switchSlots);
    }

    @Override
    public ItemStack assemble(ICastingContainer inv) {
        ItemStack result = getOutput().copy();
        if (inv.getStack().getItem() instanceof BlockItem blockItem) {
            return RetexturedBlockItem.setTexture(result, blockItem.getBlock());
        }
        return result;
    }
}
