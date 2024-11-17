package slimeknights.mantle.data.gson;

import com.google.gson.*;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.gson.GenericRegisteredSerializer.IJsonSerializable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.NamedComponentRegistry;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a serialier/deserializer to/from JsonObjects that automatically handles dispatching responsibilities to named serializers.
 *
 * @param <T> Type of the serializable object
 * @see GenericLoaderRegistry GenericLoaderRegistry for an alternative that also supports network friendly byte buffers
 */
public class GenericRegisteredSerializer<T extends IJsonSerializable> implements JsonSerializer<T>, JsonDeserializer<T> {
    /**
     * Map of all serializers for implementations.
     * TODO 1.19: would using {@link NamedComponentRegistry} make this implemention simplier?
     */
    private final Map<Identifier, JsonDeserializer<? extends T>> deserializers = new HashMap<>();

    /**
     * Registers a deserializer by name
     */
    public void registerDeserializer(Identifier name, JsonDeserializer<? extends T> jsonDeserializer) {
        this.deserializers.put(name, jsonDeserializer);
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = net.minecraft.util.JsonHelper.asObject(json, "transformer");
        Identifier type = JsonHelper.getResourceLocation(object, "type");
        JsonDeserializer<? extends T> deserializer = this.deserializers.get(type);
        if (deserializer == null) {
            throw new JsonSyntaxException("Unknown serializer " + type);
        }
        return deserializer.deserialize(json, typeOfT, context);
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject serialized = src.serialize(context);
        if (!serialized.has("type")) {
            throw new IllegalArgumentException("Invalid serialized object, missing type");
        }
        String typeStr = net.minecraft.util.JsonHelper.getString(serialized, "type");
        Identifier typeRL = Identifier.tryParse(typeStr);
        if (typeRL == null) {
            throw new IllegalArgumentException("Invalid object type '" + typeStr + '\'');
        }
        if (!this.deserializers.containsKey(typeRL)) {
            throw new IllegalArgumentException("Unregistered object " + typeStr);
        }
        return serialized;
    }

    /**
     * Interface to make a generic interface serializable
     */
    public interface IJsonSerializable {
        /**
         * Serializes this object
         */
        JsonObject serialize(JsonSerializationContext context);
    }
}
