package slimeknights.mantle.data.gson;

import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;

/**
 * Serializer for a generic tag key type.
 */
@RequiredArgsConstructor
public class TagKeySerializer<T> implements JsonSerializer<TagKey<T>>, JsonDeserializer<TagKey<T>> {
    private final RegistryKey<Registry<T>> registry;

    @Override
    public TagKey<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return TagKey.of(this.registry, JsonHelper.convertToResourceLocation(json, "tag"));
    }

    @Override
    public JsonElement serialize(TagKey<T> src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.id().toString());
    }
}
