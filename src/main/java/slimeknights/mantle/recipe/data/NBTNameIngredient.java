package slimeknights.mantle.recipe.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.NbtIngredient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Ingredient for a NBT sensitive item from another mod, should never be used outside datagen
 */
public class NBTNameIngredient extends NbtIngredient {
    private final Identifier name;
    @Nullable
    private final NbtCompound nbt;

    protected NBTNameIngredient(Identifier name, @Nullable NbtCompound nbt) {
        super(Ingredient.ofStacks(ItemStack.EMPTY), nbt, true);
        this.name = name;
        this.nbt = nbt;
    }

    /**
     * Creates an ingredient for the given name and NBT
     *
     * @param name Item name
     * @param nbt  NBT
     * @return Ingredient
     */
    public static NBTNameIngredient from(Identifier name, NbtCompound nbt) {
        return new NBTNameIngredient(name, nbt);
    }

    /**
     * Creates an ingredient for an item that must have no NBT
     *
     * @param name Item name
     * @return Ingredient
     */
    public static NBTNameIngredient from(Identifier name) {
        return new NBTNameIngredient(name, null);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", Objects.requireNonNull(CraftingHelper.getID(Serializer.INSTANCE)).toString());
        json.addProperty("item", this.name.toString());
        if (this.nbt != null) {
            json.addProperty("nbt", this.nbt.toString());
        }
        return json;
    }
}
