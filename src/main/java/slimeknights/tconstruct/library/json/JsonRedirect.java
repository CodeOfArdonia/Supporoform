package slimeknights.tconstruct.library.json;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import lombok.Data;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import slimeknights.mantle.util.JsonHelper;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a redirect in a material or modifier JSON
 */
@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
public class JsonRedirect {
    private final Identifier id;
    @Nullable
    private final ICondition condition;

    /**
     * Serializes this to JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id.toString());
        if (this.condition != null) {
            json.add("condition", CraftingHelper.serialize(this.condition));
        }
        return json;
    }

    /**
     * Deserializes this to JSON
     */
    public static JsonRedirect fromJson(JsonObject json) {
        Identifier id = JsonHelper.getResourceLocation(json, "id");
        ICondition condition = null;
        if (json.has("condition")) {
            condition = CraftingHelper.getCondition(json);
        }
        return new JsonRedirect(id, condition);
    }
}
