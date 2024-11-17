package slimeknights.mantle.recipe.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Ingredient for a non-NBT sensitive item from another mod, should never be used outside datagen
 */
public class ItemNameIngredient extends AbstractIngredient {
    private final List<Identifier> names;

    protected ItemNameIngredient(List<Identifier> names) {
        super(names.stream().map(NamedValue::new));
        this.names = names;
    }

    /**
     * Creates a new ingredient from a list of names
     */
    public static ItemNameIngredient from(List<Identifier> names) {
        return new ItemNameIngredient(names);
    }

    /**
     * Creates a new ingredient from a list of names
     */
    public static ItemNameIngredient from(Identifier... names) {
        return from(Arrays.asList(names));
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a JSON object for a name
     */
    private static JsonObject forName(Identifier name) {
        JsonObject json = new JsonObject();
        json.addProperty("item", name.toString());
        return json;
    }

    @Override
    public JsonElement toJson() {
        if (this.names.size() == 1) {
            return forName(this.names.get(0));
        }
        JsonArray array = new JsonArray();
        for (Identifier name : this.names) {
            array.add(forName(name));
        }
        return array;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return VanillaIngredientSerializer.INSTANCE;
    }

    @RequiredArgsConstructor
    public static class NamedValue implements Ingredient.Entry {
        private final Identifier name;

        @Override
        public Collection<ItemStack> getStacks() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("item", this.name.toString());
            return json;
        }
    }
}
