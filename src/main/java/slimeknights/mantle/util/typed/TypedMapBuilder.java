package slimeknights.mantle.util.typed;

import com.google.common.collect.ImmutableMap;
import lombok.NoArgsConstructor;
import slimeknights.mantle.util.typed.TypedMap.Key;

import java.util.Map;

/**
 * Builder for a typed map, ensures key value pairs are consistent
 */
@NoArgsConstructor(staticName = "builder")
public class TypedMapBuilder {
    private final ImmutableMap.Builder<Key<?>, Object> builder = ImmutableMap.builder();

    /**
     * Adds a value to the map
     */
    public <K> TypedMapBuilder put(Key<K> key, K value) {
        this.builder.put(key, value);
        return this;
    }

    /**
     * Builds the final map
     */
    public TypedMap build() {
        Map<Key<?>, Object> map = this.builder.build();
        if (map.isEmpty()) {
            return TypedMap.empty();
        }
        return new BackedTypedMap(map);
    }
}
