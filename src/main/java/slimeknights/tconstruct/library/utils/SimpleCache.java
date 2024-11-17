package slimeknights.tconstruct.library.utils;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Simple implementation of a clearable hash map backed cache
 *
 * @see net.minecraft.util.Util#memoize(Function)
 */
@RequiredArgsConstructor
public class SimpleCache<K, V> implements Function<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Function<K, V> ifAbsent;

    @Override
    public V apply(K key) {
        return this.cache.computeIfAbsent(key, this.ifAbsent);
    }

    /**
     * Clears the cache
     */
    public void clear() {
        this.cache.clear();
    }
}