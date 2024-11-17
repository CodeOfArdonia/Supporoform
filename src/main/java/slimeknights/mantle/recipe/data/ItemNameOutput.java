package slimeknights.mantle.recipe.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;

/**
 * Extension of {@link ItemOutput} for datagen of recipes for compat. Should never be used in an actual recipe
 */
@RequiredArgsConstructor(staticName = "fromName")
public class ItemNameOutput extends ItemOutput {
    private final Identifier name;
    private final int count;
    @Nullable
    private final NbtCompound nbt;

    /**
     * Creates an output for the given item with no NBT
     *
     * @param name  Item name
     * @param count Count
     * @return Output
     */
    public static ItemNameOutput fromName(Identifier name, int count) {
        return fromName(name, count, null);
    }

    /**
     * Creates an output for the given item with a count of 1
     *
     * @param name Item name
     * @return Output
     */
    public static ItemNameOutput fromName(Identifier name) {
        return fromName(name, 1);
    }

    @Override
    public ItemStack get() {
        throw new UnsupportedOperationException("Cannot get the item stack from a item name output");
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
        String itemName = this.name.toString();
        if (this.nbt == null && (this.count <= 1 || !writeCount)) {
            return new JsonPrimitive(itemName);
        } else {
            JsonObject jsonResult = new JsonObject();
            jsonResult.addProperty("item", itemName);
            if (writeCount) {
                jsonResult.addProperty("count", this.count);
            }
            if (this.nbt != null) {
                jsonResult.add("nbt", NBTLoadable.ALLOW_STRING.serialize(this.nbt));
            }
            return jsonResult;
        }
    }
}
