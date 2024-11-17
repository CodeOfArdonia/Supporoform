package slimeknights.tconstruct.library.recipe.modifiers.severing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.recipe.container.IEmptyContainer;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.tools.TinkerModifiers;

/**
 * Recipe to convert an entity into a head or other item for the severing modifier
 */
@RequiredArgsConstructor
public class SeveringRecipe implements ICustomOutputRecipe<IEmptyContainer> {
    protected static LoadableField<EntityIngredient, SeveringRecipe> ENTITY_FIELD = EntityIngredient.LOADABLE.requiredField("entity", r -> r.ingredient);
    /**
     * Loader instance
     */
    public static final RecordLoadable<SeveringRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(), ENTITY_FIELD,
            ItemOutput.Loadable.REQUIRED_STACK.requiredField("result", r -> r.output),
            SeveringRecipe::new);

    @Getter
    private final Identifier id;
    @Getter
    protected final EntityIngredient ingredient;
    protected final ItemOutput output;

    /**
     * Checks if the recipe matches the given type
     *
     * @param type Type
     * @return True if it matches
     */
    public boolean matches(EntityType<?> type) {
        return ingredient.test(type);
    }

    /**
     * Gets the output for this recipe for display in JEI, needs to be consistent
     *
     * @return Display output
     */
    public ItemStack getOutput() {
        return output.get();
    }

    /**
     * Gets the output for this recipe, does not need to be consistent (can use randomness) and may be empty
     *
     * @param entity Entity being melted
     * @return Item output
     */
    public ItemStack getOutput(Entity entity) {
        return getOutput().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.severingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TinkerRecipeTypes.SEVERING.get();
    }

    /**
     * @deprecated use {@link #matches(EntityType)}
     */
    @Deprecated
    @Override
    public boolean matches(IEmptyContainer inv, World worldIn) {
        return false;
    }
}
