package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import slimeknights.mantle.data.loadable.Loadable;

/**
 * Loadable for ingredients, handling Forge ingredients
 */
public enum IngredientLoadable implements Loadable<Ingredient> {
    ALLOW_EMPTY,
    DISALLOW_EMPTY;

    @Override
    public Ingredient convert(JsonElement element, String key) {
        return Ingredient.fromJson(element, this == ALLOW_EMPTY);
    }

    @Override
    public JsonElement serialize(Ingredient object) {
        if (object.isEmpty() && this == DISALLOW_EMPTY) {
            throw new IllegalArgumentException("Ingredient cannot be empty");
        }
        return object.toJson();
    }

    @Override
    public Ingredient decode(PacketByteBuf buffer) {
        return Ingredient.fromPacket(buffer);
    }

    @Override
    public void encode(PacketByteBuf buffer, Ingredient object) {
        object.write(buffer);
    }
}
