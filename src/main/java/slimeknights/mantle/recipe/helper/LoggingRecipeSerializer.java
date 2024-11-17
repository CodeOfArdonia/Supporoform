package slimeknights.mantle.recipe.helper;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;

/**
 * Recipe serializer that logs network exceptions before throwing them as otherwise the exceptions may be invisible
 *
 * @param <T> Recipe class
 */
public interface LoggingRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
    /**
     * Read the recipe from the packet
     *
     * @param id     Recipe ID
     * @param buffer Buffer instance
     * @return Parsed recipe
     * @throws RuntimeException If any errors happen, the exception will be logged automatically
     */
    @Nullable
    T fromNetworkSafe(Identifier id, PacketByteBuf buffer);

    /**
     * Write the method to the buffer
     *
     * @param buffer Buffer instance
     * @param recipe Recipe instance
     * @throws RuntimeException If any errors happen, the exception will be logged automatically
     */
    void toNetworkSafe(PacketByteBuf buffer, T recipe);

    @Nullable
    @Override
    default T read(Identifier id, PacketByteBuf buffer) {
        try {
            return this.fromNetworkSafe(id, buffer);
        } catch (RuntimeException e) {
            Mantle.logger.error("{}: Error reading recipe {} from packet", this.getClass().getSimpleName(), id, e);
            throw e;
        }
    }

    @Override
    default void write(PacketByteBuf buffer, T recipe) {
        try {
            this.toNetworkSafe(buffer, recipe);
        } catch (RuntimeException e) {
            Mantle.logger.error("{}: Error writing recipe {} of class {} and type {} to packet", this.getClass().getSimpleName(), recipe.getId(), recipe.getClass().getSimpleName(), recipe.getType(), e);
            throw e;
        }
    }
}
