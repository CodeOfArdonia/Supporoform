package slimeknights.mantle.util.typed;

import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Typed map backed by a map. It is the callers responsibility to ensure the passed map contains valid key to value pairs.
 * Note this class implements {@link MutableTypedMap}, but it will throw if the backing map is not mutable. It is the caller's responsibility to use the correct interface for non-mutable maps.
 */
@SuppressWarnings("unchecked")  // the nature of this map means we inherently have unchecked operations
public record BackedTypedMap(Map<Key<?>, Object> map) implements MutableTypedMap {
    /**
     * Creates a new mutable backed map
     */
    public BackedTypedMap() {
        // using identity as keys are typically identity objects, if you have non-identity keys you can use the regular constructor.
        this(new IdentityHashMap<>());
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Key<?> key) {
        return this.map.containsKey(key);
    }

    @Nullable
    @Override
    public <K> K get(Key<K> key) {
        return (K) this.map.get(key);
    }

    @Override
    public Set<Key<?>> keySet() {
        return this.map.keySet();
    }

    @Nullable
    @Override
    public <R, K extends R> R getOrDefault(Key<K> key, @Nullable R defaultValue) {
        return (R) this.map.getOrDefault(key, defaultValue);
    }

    @Override
    public <K> void put(Key<K> key, K value) {
        this.map.put(key, value);
    }

    @Override
    public void remove(Key<?> key) {
        this.map.remove(key);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public <K> K computeIfAbsent(ComputingKey<K> key) {
        return (K) this.map.computeIfAbsent(key, k -> key.get());
    }
}
