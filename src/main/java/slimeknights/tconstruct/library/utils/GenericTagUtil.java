package slimeknights.tconstruct.library.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Static helpers for generic tag loading
 */
public class GenericTagUtil {
    private GenericTagUtil() {
    }

    /**
     * Converts the results of the loader into a map from tag keys to lists
     */
    public static <T> Map<TagKey<T>, List<T>> mapLoaderResults(RegistryKey<? extends Registry<T>> registry, Map<Identifier, Collection<T>> map) {
        return map.entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> TagKey.of(registry, entry.getKey()), entry -> List.copyOf(entry.getValue())));
    }

    /**
     * Creates a map of reverse tags for the given map of tags
     */
    public static <T, I extends Identifier> Map<I, Set<TagKey<T>>> reverseTags(Function<T, I> keyMapper, Map<TagKey<T>, ? extends Collection<T>> tags) {
        Map<I, Builder<TagKey<T>>> reverseTags = new HashMap<>();
        Function<I, Builder<TagKey<T>>> makeSet = id -> ImmutableSet.builder();
        for (Entry<TagKey<T>, ? extends Collection<T>> entry : tags.entrySet()) {
            TagKey<T> key = entry.getKey();
            for (T value : entry.getValue()) {
                reverseTags.computeIfAbsent(keyMapper.apply(value), makeSet).add(key);
            }
        }
        return reverseTags.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().build()));
    }

    /**
     * Decodes a map of tags from the packet
     */
    public static <T> Map<TagKey<T>, List<T>> decodeTags(PacketByteBuf buf, RegistryKey<? extends Registry<T>> registry, Function<Identifier, T> valueGetter) {
        ImmutableMap.Builder<TagKey<T>, List<T>> builder = ImmutableMap.builder();
        int mapSize = buf.readVarInt();
        for (int i = 0; i < mapSize; i++) {
            Identifier tagId = buf.readIdentifier();
            int tagSize = buf.readVarInt();
            ImmutableList.Builder<T> tagBuilder = ImmutableList.builder();
            for (int j = 0; j < tagSize; j++) {
                tagBuilder.add(valueGetter.apply(buf.readIdentifier()));
            }
            builder.put(TagKey.of(registry, tagId), tagBuilder.build());
        }
        return builder.build();
    }

    /**
     * Writes a map of tags to a packet
     */
    public static <T> void encodeTags(PacketByteBuf buf, Function<T, Identifier> keyGetter, Map<TagKey<T>, ? extends Collection<T>> tags) {
        buf.writeVarInt(tags.size());
        for (Entry<TagKey<T>, ? extends Collection<T>> entry : tags.entrySet()) {
            buf.writeIdentifier(entry.getKey().id());
            Collection<T> values = entry.getValue();
            buf.writeVarInt(values.size());
            for (T value : values) {
                buf.writeIdentifier(keyGetter.apply(value));
            }
        }
    }
}
