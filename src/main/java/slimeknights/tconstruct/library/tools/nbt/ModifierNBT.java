package slimeknights.tconstruct.library.tools.nbt;

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import slimeknights.tconstruct.library.modifiers.IncrementalModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierBuilder;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NBT object containing all current modifiers
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ModifierNBT {

    /**
     * Instance containing no modifiers
     */
    public static final ModifierNBT EMPTY = new ModifierNBT(Collections.emptyList());

    /**
     * Sorted list of modifiers
     */
    private final List<ModifierEntry> modifiers;

    /**
     * Checks if the NBT has no modifiers
     *
     * @return True if there are no modifiers
     */
    public boolean isEmpty() {
        return this.modifiers.isEmpty();
    }

    /**
     * Gets the modifier entry for a modifier
     *
     * @param modifier Modifier to check
     * @return Modifier entry, or {@link ModifierEntry#EMPTY} if absent
     */
    public ModifierEntry getEntry(ModifierId modifier) {
        for (ModifierEntry entry : this.modifiers) {
            if (entry.matches(modifier)) {
                return entry;
            }
        }
        return ModifierEntry.EMPTY;
    }

    /**
     * Gets the level of a modifier
     *
     * @param modifier Modifier to check
     * @return Modifier level, or 0 if modifier is missing
     */
    public int getLevel(ModifierId modifier) {
        return this.getEntry(modifier).getLevel();
    }


    /* Withers */

    /**
     * Creates a copy of this NBT with the given modifier added. Result will be unsorted
     * Do not use if you need to make multiple additions, use {@link Builder}
     *
     * @param modifier Modifier
     * @param level    Levels of the modifier to add
     * @return Instance with the given modifier
     */
    public ModifierNBT withModifier(ModifierId modifier, int level) {
        if (level <= 0) {
            throw new IllegalArgumentException("Invalid level, must be above zero");
        }

        // rather than using the builder, just use a raw list builder
        // easier for adding a single entry, and the cases that call this method don't care about sorting
        ImmutableList.Builder<ModifierEntry> builder = ImmutableList.builder();
        boolean found = false;
        for (ModifierEntry entry : this.modifiers) {
            // first match increases the level
            // shouldn't be a second match (all the methods are protected), but just in case we prevent modifier duplication
            if (!found && entry.matches(modifier)) {
                builder.add(entry.withLevel(entry.getLevel() + level));
                found = true;
            } else {
                builder.add(entry);
            }
        }
        // if no matching modifier, create a new entry
        if (!found) {
            builder.add(new ModifierEntry(modifier, level));
        }
        return new ModifierNBT(builder.build());
    }

    /**
     * Creates a copy of this NBT with the given incremental modifier amount added.
     * Do not use if you need to make multiple additions, use {@link Builder}
     *
     * @param modifier Modifier
     * @param amount   Increments to add
     * @param needed   Increment scale to reach a full level
     * @return Instance with the given modifier
     */
    public ModifierNBT addAmount(ModifierId modifier, int amount, int needed) {
        // no need to do anything if amount or need is not at least 1
        if (amount <= 0 || needed <= 0) {
            return this;
        }
        // no shortcut here as amount being greater than need should lead to a full level
        // rather than using the builder, just use a raw list builder
        // easier for adding a single entry, and the cases that call this method don't care about sorting
        ImmutableList.Builder<ModifierEntry> builder = ImmutableList.builder();
        boolean found = false;
        for (ModifierEntry entry : this.modifiers) {
            // first match increases the level
            // shouldn't be a second match (all the methods are protected), but just in case we prevent modifier duplication
            if (!found && entry.matches(modifier)) {
                builder.add(entry.addAmount(amount, needed));
                found = true;
            } else {
                builder.add(entry);
            }
        }
        // if no matching modifier, create a new entry
        if (!found) {
            builder.add(IncrementalModifierEntry.of(modifier, 1, amount, needed));
        }
        return new ModifierNBT(builder.build());
    }

    /**
     * Creates a copy of this NBT without the given modifier
     *
     * @param modifier Modifier to remove
     * @param level    Level to remove
     * @return ModifierNBT without the given modifier
     */
    public ModifierNBT withoutModifier(ModifierId modifier, int level) {
        if (level <= 0) {
            throw new IllegalArgumentException("Invalid level, must be above zero");
        }

        // rather than using the builder, just use a raw list builder
        // easier for adding a single entry, and the cases that call this method don't care about sorting
        ImmutableList.Builder<ModifierEntry> builder = ImmutableList.builder();
        for (ModifierEntry entry : this.modifiers) {
            if (entry.matches(modifier) && level > 0) {
                if (entry.getLevel() > level) {
                    builder.add(entry.withLevel(entry.getLevel() - level));
                    level = 0;
                } else {
                    level -= entry.getLevel();
                }
            } else {
                builder.add(entry);
            }
        }
        return new ModifierNBT(builder.build());
    }


    /* NBT */

    /**
     * Re-adds the modifier list from NBT
     */
    public static ModifierNBT readFromNBT(@Nullable NbtElement inbt) {
        if (inbt == null || inbt.getType() != NbtElement.LIST_TYPE) {
            return EMPTY;
        }

        NbtList listNBT = (NbtList) inbt;
        if (listNBT.getHeldType() != NbtElement.COMPOUND_TYPE) {
            return EMPTY;
        }

        ImmutableList.Builder<ModifierEntry> builder = ImmutableList.builder();
        for (int i = 0; i < listNBT.size(); i++) {
            ModifierEntry entry = ModifierEntry.readFromNBT(listNBT.getCompound(i));
            if (entry != ModifierEntry.EMPTY) {
                builder.add(entry);
            }
        }
        return new ModifierNBT(builder.build());
    }

    /**
     * Writes these modifiers to NBT
     */
    public NbtList serializeToNBT() {
        NbtList list = new NbtList();
        for (ModifierEntry entry : this.modifiers) {
            list.add(entry.serializeToNBT());
        }
        return list;
    }


    /* Builder */

    /**
     * Creates a new builder for modifier NBT
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating a modifier list with multiple additions. Builder results will be sorted
     */
    public static class Builder implements ModifierBuilder {
        /**
         * Intentionally using modifiers to ensure they are resolved
         */
        private final Map<ModifierId, ModifierEntry> modifiers = new LinkedHashMap<>();

        private Builder() {
        }

        @Override
        public Builder add(ModifierEntry entry) {
            if (entry != ModifierEntry.EMPTY) {
                ModifierId id = entry.getId();
                ModifierEntry current = this.modifiers.get(id);
                if (current != null) {
                    entry = current.merge(entry);
                }
                this.modifiers.put(id, entry);
            }
            return this;
        }

        @Override
        public ModifierNBT build() {
            // converts the map into a list of entries, priority sorted
            // note priority is negated so higher numbers go first
            List<ModifierEntry> list = this.modifiers.values().stream()
                    // sort on priority, falls back to the order they were added
                    .sorted(Comparator.comparingInt(entry -> -entry.getModifier().getPriority()))
                    .collect(Collectors.toList());
            // it's rare to see no modifiers, but no sense creating a new instance for that
            if (list.isEmpty()) {
                return EMPTY;
            }
            return new ModifierNBT(ImmutableList.copyOf(list));
        }
    }
}
