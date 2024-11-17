package slimeknights.tconstruct.library.modifiers.data;

import lombok.Getter;
import net.minecraft.entity.EquipmentSlot;

/**
 * Helper class to keep track the max vanilla level in a modifier, ints and only on four armor slots
 */
public class VanillaMaxLevel {
    /**
     * Level for each slot
     */
    private final int[] levels = new int[4];
    /**
     * Max level across all slots
     */
    @Getter
    private int max = 0;

    /**
     * Sets the given vanilla level in the structure
     */
    public void set(EquipmentSlot slot, int level) {
        int oldLevel = this.levels[slot.getEntitySlotId()];
        if (level != oldLevel) {
            this.levels[slot.getEntitySlotId()] = level;
            // if new max, update max
            if (level > this.max) {
                this.max = level;
            } else if (this.max == oldLevel) {
                // if was max before, search for replacement max
                this.max = 0;
                for (int value : this.levels) {
                    if (value > this.max) {
                        this.max = value;
                    }
                }
            }
        }
    }
}
