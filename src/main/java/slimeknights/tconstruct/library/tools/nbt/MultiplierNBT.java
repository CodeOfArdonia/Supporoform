package slimeknights.tconstruct.library.tools.nbt;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generic container for tool stats, allows addons to select which stats they wish to use
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
@ToString
public class MultiplierNBT {
    /**
     * Loadable instance to parse this from buffers and alike
     */
    public static final Loadable<MultiplierNBT> LOADABLE = ToolStats.NUMERIC_LOADER.mapWithValues(FloatLoadable.ANY, 0).flatXmap(MultiplierNBT::new, m -> m.stats);
    /**
     * Empty stats
     */
    public static final MultiplierNBT EMPTY = new MultiplierNBT(ImmutableMap.of());

    /**
     * All currently contained multipliers
     */
    private final Map<INumericToolStat<?>, Float> stats;

    /**
     * Creates a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets a set of all stats contained
     *
     * @return Stat type set
     */
    public Set<INumericToolStat<?>> getContainedStats() {
        return stats.keySet();
    }

    /**
     * Checks if the NBT contains the given stat
     *
     * @param stat Stat to check for
     * @return True if the stat is contained
     */
    public boolean hasStat(INumericToolStat<?> stat) {
        return stats.containsKey(stat);
    }

    /**
     * Gets the given multiplier
     *
     * @param stat Stat
     * @return Value, or default if the stat is missing
     */
    public float get(INumericToolStat<?> stat) {
        return stats.getOrDefault(stat, 1f);
    }


    /* NBT */

    /**
     * Reads the multipliers from NBT
     */
    public static MultiplierNBT readFromNBT(@Nullable NbtElement inbt) {
        if (inbt == null || inbt.getType() != NbtElement.COMPOUND_TYPE) {
            return EMPTY;
        }
        // simply try each key as a tool stat
        Builder builder = builder();
        NbtCompound nbt = (NbtCompound) inbt;
        for (String key : nbt.getKeys()) {
            if (nbt.contains(key, NbtElement.NUMBER_TYPE) && StatsNBT.readStatIdFromNBT(key) instanceof INumericToolStat<?> stat) {
                builder.set(stat, nbt.getFloat(key));
            }
        }
        return builder.build();
    }

    /**
     * Writes these stats to NBT
     */
    public NbtCompound serializeToNBT() {
        NbtCompound nbt = new NbtCompound();
        for (Entry<INumericToolStat<?>, Float> entry : stats.entrySet()) {
            nbt.putFloat(entry.getKey().getName().toString(), entry.getValue());
        }
        return nbt;
    }


    /* Network */

    /**
     * Writes this to a packet buffer
     */
    @Deprecated
    public void toNetwork(PacketByteBuf buffer) {
        LOADABLE.encode(buffer, this);
    }

    /**
     * Reads this object from the network
     */
    @Deprecated
    public static MultiplierNBT fromNetwork(PacketByteBuf buffer) {
        return LOADABLE.decode(buffer);
    }

    /**
     * Builder for a multiplier, mostly prevents nulls from being added
     */
    public static class Builder {
        private final ImmutableMap.Builder<INumericToolStat<?>, Float> builder = ImmutableMap.builder();

        private Builder() {
        }

        /**
         * Sets the given stat in the builder
         */
        public Builder set(INumericToolStat<?> stat, float value) {
            if (value != 1f) {
                builder.put(stat, Math.max(value, 0));
            }
            return this;
        }

        /**
         * Builds the stats from the given values
         */
        public MultiplierNBT build() {
            Map<INumericToolStat<?>, Float> map = builder.build();
            if (map.isEmpty()) {
                return EMPTY;
            }
            return new MultiplierNBT(map);
        }
    }
}
