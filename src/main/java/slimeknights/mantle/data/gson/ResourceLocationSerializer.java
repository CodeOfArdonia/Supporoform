package slimeknights.mantle.data.gson;

import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Extension to Resource Location serializer to change the default mod ID.
 */
@RequiredArgsConstructor
public class ResourceLocationSerializer<T extends Identifier> implements JsonDeserializer<T>, JsonSerializer<T> {
    private final Function<String, T> constructor;
    private final String modId;

    /**
     * Creates an instance for resource locations
     */
    public static ResourceLocationSerializer<Identifier> resourceLocation(String modId) {
        return new ResourceLocationSerializer<>(Identifier::new, modId);
    }

    @Override
    public JsonElement serialize(Identifier loc, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(loc.toString());
    }

    @Override
    public T deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        String loc = JsonHelper.asString(element, "location");
        if (!loc.contains(":")) {
            loc = this.modId + ":" + loc;
        }
        return this.constructor.apply(loc);
    }
}
