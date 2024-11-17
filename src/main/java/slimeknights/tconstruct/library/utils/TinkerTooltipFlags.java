package slimeknights.tconstruct.library.utils;

import net.minecraft.client.item.TooltipContext;

/**
 * Pseudo-copy of the vanilla flag, extended with screen context
 */
public enum TinkerTooltipFlags implements TooltipContext {
    /**
     * Display in a station screen
     */
    TINKER_STATION;

    @Override
    public boolean isAdvanced() {
        return true;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
