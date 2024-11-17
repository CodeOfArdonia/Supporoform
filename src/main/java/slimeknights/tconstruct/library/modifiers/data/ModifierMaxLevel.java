package slimeknights.tconstruct.library.modifiers.data;

import lombok.Getter;
import net.minecraft.entity.EquipmentSlot;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;

import org.jetbrains.annotations.Nullable;

/**
 * Helper class to keep track the max modifier level in a modifier, floats, keeps track of max slot, and tracks all 6 slots
 */
public class ModifierMaxLevel {
    /**
     * Level for each slot
     */
    private final float[] levels = new float[6];
    /**
     * Max level across all slots
     */
    @Getter
    private float max = 0;
    /**
     * Slot containing the max level
     */
    @Getter
    @Nullable
    private EquipmentSlot maxSlot;

    /**
     * Sets the given value in the structure
     */
    public void set(EquipmentSlot slot, float level) {
        float oldLevel = this.levels[slot.getArmorStandSlotId()];
        if (level != oldLevel) {
            // first, update level
            this.levels[slot.getArmorStandSlotId()] = level;
            // if larger than max, new max
            if (level >= this.max) {
                this.max = level;
                this.maxSlot = slot;
            } else if (slot == this.maxSlot) {
                // if the old level was max, find new max
                this.max = 0;
                for (EquipmentSlot armorSlot : ModifiableArmorMaterial.ARMOR_SLOTS) {
                    float value = this.levels[armorSlot.getArmorStandSlotId()];
                    if (value > this.max) {
                        this.max = value;
                        this.maxSlot = armorSlot;
                    }
                }
            }
        }
    }
}
