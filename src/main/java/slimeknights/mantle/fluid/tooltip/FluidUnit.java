package slimeknights.mantle.fluid.tooltip;

import lombok.RequiredArgsConstructor;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Single entry for text options
 */
// needed in GSON
@RequiredArgsConstructor
public class FluidUnit {

    private final String key;
    private final int needed;

    /**
     * Gets the display text for this fluid entry
     *
     * @return Display text
     */
    public long getText(List<Text> tooltip, long amount) {
        long full = amount / this.needed;
        if (full > 0) {
            tooltip.add(Text.translatable(this.key, full).formatted(Formatting.GRAY));
        }
        return amount % this.needed;
    }
}
