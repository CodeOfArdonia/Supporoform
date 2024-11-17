package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.damagesource.EntityDamageSource;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.tools.modifiers.traits.melee.LaceratingModifier;

/**
 * Potion effect from {@link LaceratingModifier}
 */
public class BleedingEffect extends NoMilkEffect {
    private static final String SOURCE_KEY = TConstruct.prefix("bleed");

    public BleedingEffect() {
        super(StatusEffectCategory.HARMFUL, 0xa80000, true);
    }

    @Override
    public boolean canApplyUpdateEffect(int tick, int level) {
        // every half second
        return tick > 0 && tick % 20 == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity target, int level) {
        // attribute to player kill
        LivingEntity lastAttacker = target.getAttacking();
        DamageSource source;
        if (lastAttacker != null) {
            source = new BleedingDamageSource(SOURCE_KEY, lastAttacker);
        } else {
            source = new DamageSource(SOURCE_KEY);
        }
        source.bypassMagic();

        // perform damage
        int hurtResistantTime = target.timeUntilRegen;
        ToolAttackUtil.attackEntitySecondary(source, (level + 1f) / 2f, target, target, true);
        target.timeUntilRegen = hurtResistantTime;

        // damage particles
        if (target.getWorld() instanceof ServerWorld) {
            ((ServerWorld) target.getWorld()).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0.1, 0, 0.1, 0.2);
        }
    }

    /**
     * Guardians use the direct entity to determine if they should thorns, while the direct marks for player kills
     * treat this as indirect damage by making the direct entity null, so guardians treat it like arrows
     */
    private static class BleedingDamageSource extends DamageSource {
        public BleedingDamageSource(String name, Entity entity) {
            super(name, entity);
        }
    }
}
