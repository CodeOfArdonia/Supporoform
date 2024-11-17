package slimeknights.mantle.data.loadable.mapping;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Loadable for a map type.
 *
 * @param keyLoadable   Loadable for the map keys, parsed from strings
 * @param valueLoadable Loadable for map values, parsed from elements
 * @param <K>           Key type
 * @param <V>           Value type
 */
public record MapLoadable<K, V>(StringLoadable<K> keyLoadable, Loadable<V> valueLoadable,
                                int minSize) implements Loadable<Map<K, V>> {
    @Override
    public Map<K, V> convert(JsonElement element, String key) {
        JsonObject json = JsonHelper.asObject(element, key);
        if (json.size() < this.minSize) {
            throw new JsonSyntaxException(key + " must have at least " + this.minSize + " elements");
        }
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        String mapKey = key + "'s key";
        for (Entry<String, JsonElement> entry : json.entrySet()) {
            String entryKey = entry.getKey();
            builder.put(
                    this.keyLoadable.parseString(entryKey, mapKey),
                    this.valueLoadable.convert(entry.getValue(), entryKey));
        }
        return builder.build();
    }

    @Override
    public JsonElement serialize(Map<K, V> map) {
        if (map.size() < this.minSize) {
            throw new RuntimeException("Collection must have at least " + this.minSize + " elements");
        }
        JsonObject json = new JsonObject();
        for (Entry<K, V> entry : map.entrySet()) {
            json.add(
                    this.keyLoadable.getString(entry.getKey()),
                    this.valueLoadable.serialize(entry.getValue()));
        }
        return json;
    }

    @Override
    public Map<K, V> decode(PacketByteBuf buffer) {
        int size = buffer.readVarInt();
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for (int i = 0; i < size; i++) {
            builder.put(
                    this.keyLoadable.decode(buffer),
                    this.valueLoadable.decode(buffer));
        }
        return builder.build();
    }

    @Override
    public void encode(PacketByteBuf buffer, Map<K, V> map) {
        buffer.writeVarInt(map.size());
        for (Entry<K, V> entry : map.entrySet()) {
            this.keyLoadable.encode(buffer, entry.getKey());
            this.valueLoadable.encode(buffer, entry.getValue());
        }
    }
}
