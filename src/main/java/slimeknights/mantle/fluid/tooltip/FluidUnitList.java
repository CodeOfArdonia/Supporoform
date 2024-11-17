package slimeknights.mantle.fluid.tooltip;

import lombok.RequiredArgsConstructor;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.RegistryHelper;

import java.util.List;

/**
 * Represents a list of tooltip unit types for a fluid
 */
@SuppressWarnings("ClassCanBeRecord") // needed in GSON
@RequiredArgsConstructor
public class FluidUnitList {
    // TODO: switch to fluid ingredient
    @Nullable
    private final TagKey<Fluid> tag;
    private final List<FluidUnit> units;

    /**
     * Checks if this matches the given fluid
     */
    public boolean matches(Fluid fluid) {
        return this.tag != null && RegistryHelper.contains(this.tag, fluid);
    }

    /**
     * Applies the text of all child units
     */
    public long getText(List<Text> tooltip, long amount) {
        if (this.units != null) {
            for (FluidUnit unit : this.units) {
                amount = unit.getText(tooltip, amount);
            }
        }
        return amount;
    }
}
