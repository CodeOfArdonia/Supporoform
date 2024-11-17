package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import slimeknights.tconstruct.tools.modifiers.upgrades.general.MagneticModifier;

public class MagneticEffect extends NoMilkEffect {
    public MagneticEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x720000, false);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return (duration & 1) == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        MagneticModifier.applyMagnet(entity, amplifier);
    }
}
