package slimeknights.mantle.recipe.helper;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;

/**
 * Type aware recipe serializer, for the sake of recipes that serializers to swap out the type.
 */
public interface TypeAwareRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
    /**
     * Gets the type of this recipe
     */
    RecipeType<?> getType();
}
