package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import slimeknights.tconstruct.tools.modifiers.upgrades.general.MagneticModifier;

public class RepulsiveEffect extends NoMilkEffect {
    public RepulsiveEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x727272, false);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return (duration & 1) == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        MagneticModifier.applyVelocity(entity, amplifier, LivingEntity.class, 2, -0.1f, 10);
    }
}
