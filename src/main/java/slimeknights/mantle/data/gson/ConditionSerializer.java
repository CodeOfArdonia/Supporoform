package slimeknights.mantle.data.gson;

import com.google.gson.*;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.minecraft.util.JsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

import java.lang.reflect.Type;

/**
 * Serializer for a forge condition.
 */
public class ConditionSerializer implements JsonDeserializer<ICondition>, JsonSerializer<ICondition> {
    public static final ConditionSerializer INSTANCE = new ConditionSerializer();

    private ConditionSerializer() {
    }

    @Override
    public ICondition deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return CraftingHelper.getCondition(JsonHelper.asObject(json, "condition"));
    }

    @Override
    public JsonElement serialize(ICondition condition, Type type, JsonSerializationContext context) {
        return CraftingHelper.serialize(condition);
    }
}
