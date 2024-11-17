package slimeknights.tconstruct.tools.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Enum to aid in armor registraton
 */
@RequiredArgsConstructor
@Getter
public enum ArmorSlotType implements StringIdentifiable {
    BOOTS(EquipmentSlot.FEET),
    LEGGINGS(EquipmentSlot.LEGS),
    CHESTPLATE(EquipmentSlot.CHEST),
    HELMET(EquipmentSlot.HEAD);

    /**
     * Armor slots in order from helmet to boots, {@link #values()} will go from boots to helmet.
     */
    public static final ArmorSlotType[] TOP_DOWN = {HELMET, CHESTPLATE, LEGGINGS, BOOTS};
    /**
     * copy of the vanilla array for use in builders
     */
    public static final int[] MAX_DAMAGE_ARRAY = {13, 15, 16, 11};
    /**
     * factor for shield durability
     */
    public static final int SHIELD_DAMAGE = 22;

    private final EquipmentSlot equipmentSlot;
    private final String serializedName = this.toString().toLowerCase(Locale.ROOT);
    private final int index = this.ordinal();

    /**
     * Gets an equipment slot for the given armor slot
     */
    @Nullable
    public static ArmorSlotType fromEquipment(EquipmentSlot slotType) {
        return switch (slotType) {
            case FEET -> BOOTS;
            case LEGS -> LEGGINGS;
            case CHEST -> CHESTPLATE;
            case HEAD -> HELMET;
            default -> null;
        };
    }

    @Override
    public String asString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Interface for armor module builders, which are builders designed to create slightly varied modules based on the armor slot
     */
    public interface ArmorBuilder<T> {
        /**
         * Builds the object for the given slot
         */
        T build(ArmorSlotType slot);
    }

    /**
     * Builder for an object that also includes shields
     */
    public interface ArmorShieldBuilder<T> extends ArmorBuilder<T> {
        /**
         * Builds the object for the shield
         */
        T buildShield();
    }
}
