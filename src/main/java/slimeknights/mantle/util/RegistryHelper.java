package slimeknights.mantle.util;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RegistryHelper {
    private RegistryHelper() {
    }

    /**
     * Gets the registry for the given key, dealing with tags
     */
    @Nullable
    @SuppressWarnings({"unchecked"})
    public static <T> Registry<T> getRegistry(RegistryKey<? extends Registry<T>> key) {
        return (Registry<T>) Registries.REGISTRIES.get(key.getValue());
    }

    /**
     * Gets a stream of tag holders for the given registry
     */
    public static <T> Stream<RegistryEntry<T>> getTagStream(Registry<T> registry, TagKey<T> key) {
        return StreamSupport.stream(registry.iterateEntries(key).spliterator(), false);
    }

    /**
     * Gets a stream of tag holders for the given registry
     */
    public static <T> Stream<RegistryEntry<T>> getTagStream(TagKey<T> key) {
        Registry<T> registry = getRegistry(key.registry());
        if (registry == null) {
            return Stream.empty();
        }
        return getTagStream(registry, key);
    }

    /**
     * Gets a stream of tag values for the given registry
     */
    public static <T> Stream<T> getTagValueStream(Registry<T> registry, TagKey<T> key) {
        return getTagStream(registry, key).filter(RegistryEntry::hasKeyAndValue).map(RegistryEntry::value);
    }

    /**
     * Gets a stream of tag values for the given registry
     */
    public static <T> Stream<T> getTagValueStream(TagKey<T> key) {
        return getTagStream(key).filter(RegistryEntry::hasKeyAndValue).map(RegistryEntry::value);
    }

    /**
     * Checks if the given tag contains the given registry object
     */
    public static <T> boolean contains(Registry<T> registry, TagKey<T> tag, T value) {
        int index = registry.getRawId(value);
        if (index == Registry.ABSENT_RAW_ID) {
            return false;
        }
        return registry.getEntry(index).filter(holder -> holder.isIn(tag)).isPresent();
    }

    /**
     * Checks if the given tag contains the given registry object
     */
    public static <T> boolean contains(TagKey<T> tag, T value) {
        Registry<T> registry = getRegistry(tag.registry());
        if (registry == null) {
            return false;
        }
        return contains(registry, tag, value);
    }

    /**
     * Checks if the given tag contains the given registry object
     */
    @SuppressWarnings("deprecation")
    public static boolean contains(TagKey<Block> tag, Block value) {
        return value.getRegistryEntry().isIn(tag);
    }

    /**
     * Checks if the given tag contains the given registry object
     */
    @SuppressWarnings("deprecation")
    public static boolean contains(TagKey<Item> tag, Item value) {
        return value.getRegistryEntry().isIn(tag);
    }

    /**
     * Checks if the given tag contains the given registry object
     */
    @SuppressWarnings("deprecation")
    public static boolean contains(TagKey<EntityType<?>> tag, EntityType<?> value) {
        return value.getRegistryEntry().isIn(tag);
    }

    /**
     * Checks if the given tag contains the given registry object
     */
    @SuppressWarnings("deprecation")
    public static boolean contains(TagKey<Fluid> tag, Fluid value) {
        return value.getRegistryEntry().isIn(tag);
    }

    /**
     * Gets a holder for a registry object
     *
     * @param registry Registry instance
     * @param entry    Entry to fetch holder
     * @param <T>      Registry type
     * @return Supplier for the given registry
     */
    public static <T> Supplier<T> getHolder(DefaultedRegistry<T> registry, T entry) {
        return registry.getEntry(registry.getRawId(entry)).orElseThrow();
    }
}
