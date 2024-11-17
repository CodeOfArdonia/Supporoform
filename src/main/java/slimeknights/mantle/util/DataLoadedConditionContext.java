package slimeknights.mantle.util;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.conditions.ICondition;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Condition context to use when data has already been loaded, used in books for processing their conditions for instance.
 */
public enum DataLoadedConditionContext implements ICondition.IContext {
    INSTANCE;

    @Override
    public <T> Collection<RegistryEntry<T>> getTag(TagKey<T> key) {
        Registry<T> registry = RegistryHelper.getRegistry(key.registry());
        if (registry != null) {
            Optional<RegistryEntryList.Named<T>> tag = registry.getEntryList(key);
            if (tag.isPresent()) {
                return tag.get().entries;
            }
        }
        return Set.of();
    }

    @Override
    public <T> Map<Identifier, Collection<RegistryEntry<T>>> getAllTags(RegistryKey<? extends Registry<T>> key) {
        Registry<T> registry = RegistryHelper.getRegistry(key);
        if (registry != null) {
            return registry.streamTagsAndEntries().collect(Collectors.toMap(entry -> entry.getFirst().id(), entry -> entry.getSecond().entries));
        }
        return Map.of();
    }
}
