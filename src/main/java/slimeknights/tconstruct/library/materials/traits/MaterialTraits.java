package slimeknights.tconstruct.library.materials.traits;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.library.materials.json.MaterialTraitsJson;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Data object holding a list of traits and a map of stat type to trait
 */
@AllArgsConstructor
public class MaterialTraits {
    @Getter
    private final List<ModifierEntry> defaultTraits;
    @Getter(AccessLevel.PROTECTED)
    private final Map<MaterialStatsId, List<ModifierEntry>> traitsPerStats;

    /**
     * Checks if the stats ID has unique traits
     *
     * @param statsId Stats ID
     * @return True if the traits for this stat type are unique
     */
    public boolean hasUniqueTraits(MaterialStatsId statsId) {
        return this.traitsPerStats.containsKey(statsId);
    }

    /**
     * Gets the traits for a stat type
     *
     * @param statsId Stats ID
     * @return List of traits
     */
    public List<ModifierEntry> getTraits(MaterialStatsId statsId) {
        return this.traitsPerStats.getOrDefault(statsId, this.defaultTraits);
    }

    /**
     * Writes this object to the packet buffer
     *
     * @param buffer Buffer instance
     */
    public void write(PacketByteBuf buffer) {
        writeTraitList(buffer, this.defaultTraits);
        // write map of traits
        buffer.writeVarInt(this.traitsPerStats.size());
        for (Entry<MaterialStatsId, List<ModifierEntry>> entry : this.traitsPerStats.entrySet()) {
            buffer.writeIdentifier(entry.getKey());
            writeTraitList(buffer, entry.getValue());
        }
    }

    /**
     * Reads this object from the packet buffer
     *
     * @param buffer Buffer
     * @return Read MaterialTraits
     */
    public static MaterialTraits read(PacketByteBuf buffer) {
        List<ModifierEntry> defaultTraits = readTraitList(buffer);
        int statTypeCount = buffer.readVarInt();
        Map<MaterialStatsId, List<ModifierEntry>> statsTraits = new HashMap<>(statTypeCount);
        for (int i = 0; i < statTypeCount; i++) {
            MaterialStatsId statsId = new MaterialStatsId(buffer.readIdentifier());
            List<ModifierEntry> traitsList = readTraitList(buffer);
            statsTraits.put(statsId, traitsList);
        }
        return new MaterialTraits(defaultTraits, statsTraits);
    }

    /**
     * Reads a single list of traits from the buffer
     *
     * @param buffer Buffer
     * @return List of traits
     */
    private static List<ModifierEntry> readTraitList(PacketByteBuf buffer) {
        ImmutableList.Builder<ModifierEntry> builder = ImmutableList.builder();
        int count = buffer.readVarInt();
        for (int i = 0; i < count; i++) {
            builder.add(ModifierEntry.read(buffer));
        }
        return builder.build();
    }

    /**
     * Writes a single list of traits to the buffer
     *
     * @param buffer Buffer
     * @param traits List of traits
     */
    private static void writeTraitList(PacketByteBuf buffer, List<ModifierEntry> traits) {
        buffer.writeVarInt(traits.size());
        for (ModifierEntry entry : traits) {
            entry.write(buffer);
        }
    }

    /**
     * Builder for use in deserializing and datagen
     */
    public static class Builder {
        @Getter
        private List<ModifierEntry> defaultTraits = null;
        @Getter(AccessLevel.PROTECTED)
        private final Map<MaterialStatsId, List<ModifierEntry>> traitsPerStats = new HashMap<>();

        /**
         * Sets the default traits
         *
         * @param traits Traits list
         */
        public void setDefaultTraits(@Nullable List<ModifierEntry> traits) {
            if (traits != null) {
                this.defaultTraits = traits;
            }
        }

        /**
         * Sets the traits for a stat type
         *
         * @param statsId Stats ID
         * @param traits  Traits list
         */
        public void setTraits(MaterialStatsId statsId, @Nullable List<ModifierEntry> traits) {
            if (traits != null) {
                this.traitsPerStats.put(statsId, traits);
            } else {
                // allow higher level datapacks to reset a stat type to default
                this.traitsPerStats.remove(statsId);
            }
        }

        /**
         * Serializes this object to a trait mapping JSON
         *
         * @return Trait mapping Json
         */
        public MaterialTraitsJson serialize() {
            // need to adjust the map to the right generics
            // also suppress the map if no stat types were defined
            Map<Identifier, List<ModifierEntry>> newMap = null;
            if (!this.traitsPerStats.isEmpty()) {
                newMap = new HashMap<>(this.traitsPerStats.size());
                newMap.putAll(this.traitsPerStats);
            }
            return new MaterialTraitsJson(this.defaultTraits, newMap);
        }

        /**
         * Builds this into a material trait object
         *
         * @param fallbacks Map of stat type fallbacks
         * @return Material traits
         */
        public MaterialTraits build(Map<MaterialStatsId, MaterialStatsId> fallbacks) {
            List<ModifierEntry> defaultTraits = this.defaultTraits;
            if (defaultTraits == null || defaultTraits.isEmpty()) {
                defaultTraits = Collections.emptyList();
            }
            Map<MaterialStatsId, List<ModifierEntry>> traitsPerStats;
            if (this.traitsPerStats.isEmpty()) {
                traitsPerStats = Collections.emptyMap();
            } else {
                // add in fallbacks now so no hit to lookup times
                ImmutableMap.Builder<MaterialStatsId, List<ModifierEntry>> builder = ImmutableMap.builder();
                builder.putAll(this.traitsPerStats);
                for (Entry<MaterialStatsId, MaterialStatsId> fallback : fallbacks.entrySet()) {
                    MaterialStatsId statType = fallback.getKey();
                    if (!this.traitsPerStats.containsKey(statType)) {
                        List<ModifierEntry> fallbackTraits = this.traitsPerStats.get(fallback.getValue());
                        if (fallbackTraits != null) {
                            builder.put(statType, fallbackTraits);
                        }
                    }
                }
                traitsPerStats = builder.build();
            }
            return new MaterialTraits(defaultTraits, traitsPerStats);
        }
    }
}
