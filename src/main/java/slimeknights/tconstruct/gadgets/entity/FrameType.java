package slimeknights.tconstruct.gadgets.entity;

import lombok.Getter;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

/**
 * All frame variants for the entity
 */
@Getter
public enum FrameType implements StringIdentifiable {
    // order is weird for the sake of preserving backwards compat, as its saved in the entity as an int
    REVERSED_GOLD, // rotation timer
    DIAMOND, // slowly winds down
    MANYULLYN, // item inside rendered full bright
    GOLD, // rotation timer
    CLEAR, // frame hidden when filled, extra large items
    NETHERITE; // immune to fire and explosions

    private static final FrameType[] VALUES = values();
    private final int id = this.ordinal();

    public static FrameType byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            id = 0;
        }

        return VALUES[id];
    }

    @Override
    public String asString() {
        return this.toString().toLowerCase(Locale.US);
    }
}
