package slimeknights.mantle.recipe.helper;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.mantle.config.Config;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.mantle.util.RegistryHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Utility that helps get the preferred item from a tag based on mod ID.
 */
public class TagPreference {
    /**
     * Just an alphabetically late RL to simplify null checks
     */
    private static final Identifier DEFAULT_ID = new Identifier("zzzzz:zzzzz"); // simplfies null checks

    /**
     * Cache from any tag key to its value
     */
    private static final Map<TagKey<?>, Optional<?>> PREFERENCE_CACHE = new ConcurrentHashMap<>();
    /**
     * Cache of comparator instances, not concurrent because it's only used inside {@link #getUncachedPreference(TagKey)} which is only used inside the concurrent {@link #PREFERENCE_CACHE}.
     */
    private static final Map<RegistryKey<?>, RegistryComparator<?>> COMPARATOR_CACHE = new HashMap<>();

    /**
     * Registers the listener with the event bus
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TagsUpdatedEvent.class, e -> PREFERENCE_CACHE.clear());
    }

    /**
     * Gets the comparator for the given registry
     */
    @SuppressWarnings("unchecked")
    private static <T> Comparator<T> getComparator(Registry<T> registry) {
        return (Comparator<T>) COMPARATOR_CACHE.computeIfAbsent(registry.getKey(), k -> new RegistryComparator<>(registry));
    }

    /**
     * Gets the preference from a tag without going through the cache, internal logic behind {@link #getPreference(TagKey)}
     */
    private static <T> Optional<T> getUncachedPreference(TagKey<T> tag) {
        Registry<T> registry = RegistryHelper.getRegistry(tag.registry());
        if (registry == null) {
            return Optional.empty();
        }
        // streams have a lovely function to get the minimum element based on a comparator
        // if the tag is empty, stream is empty so returns empty
        return RegistryHelper.getTagValueStream(tag).min(getComparator(registry));
    }

    /**
     * Don't create a new lambda instance every time we call {@link #getPreference(TagKey)}
     */
    private static final Function<TagKey<?>, Optional<?>> PREFERENCE_LOOKUP = TagPreference::getUncachedPreference;

    /**
     * Gets the preferred value from a tag based on mod ID
     *
     * @param tag Tag to fetch
     * @return Preferred value from the tag, or empty optional if the tag is empty
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getPreference(TagKey<T> tag) {
        // fetch cached value if we have one
        return (Optional<T>) PREFERENCE_CACHE.computeIfAbsent(tag, PREFERENCE_LOOKUP);
    }

    /**
     * Logic to compare two registry values
     */
    private record RegistryComparator<T>(Registry<T> registry) implements Comparator<T> {
        @Override
        public int compare(T a, T b) {
            // first get registry names, use default ID if null (unlikely)
            Identifier idA = Objects.requireNonNullElse(this.registry.getId(a), DEFAULT_ID);
            Identifier idB = Objects.requireNonNullElse(this.registry.getId(b), DEFAULT_ID);
            // first compare preferences
            List<? extends String> entries = Config.TAG_PREFERENCES.get();
            int size = entries.size();
            int indexA = LogicHelper.defaultIf(entries.indexOf(idA.getNamespace()), -1, size);
            int indexB = LogicHelper.defaultIf(entries.indexOf(idB.getNamespace()), -1, size);
            if (indexA != indexB) {
                return Integer.compare(indexA, indexB);
            }
            // for stability, fallback to registry name compare
            return idA.compareNamespaced(idB);
        }
    }
}
