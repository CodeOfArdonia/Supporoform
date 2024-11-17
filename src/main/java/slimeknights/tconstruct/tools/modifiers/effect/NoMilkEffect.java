package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.common.TinkerEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Effect that cannot be cured with milk
 */
public class NoMilkEffect extends TinkerEffect {
    public NoMilkEffect(StatusEffectCategory typeIn, int color, boolean show) {
        super(typeIn, color, show);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
