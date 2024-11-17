package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.potion.PotionUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;
import slimeknights.tconstruct.library.recipe.TagPredicate;

import java.util.List;

/**
 * Spilling effect that pulls the potion from a NBT potion fluid and applies it
 */
public record PotionFluidEffect(float scale, TagPredicate predicate) implements FluidEffect<Entity> {
    public static final RecordLoadable<PotionFluidEffect> LOADER = RecordLoadable.create(
            FloatLoadable.FROM_ZERO.requiredField("scale", e -> e.scale),
            TagPredicate.LOADABLE.defaultField("nbt", TagPredicate.ANY, e -> e.predicate),
            PotionFluidEffect::new);

    @Override
    public RecordLoadable<PotionFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getEntity();
        // must match the tag predicate
        if (target != null && this.predicate.test(fluid.getTag())) {
            List<StatusEffectInstance> effects = PotionUtil.getPotion(fluid.getTag()).getEffects();
            if (!effects.isEmpty()) {
                // prevent effects like instant damage from hitting hurt resistance
                int oldInvulnerableTime = target.timeUntilRegen;
                // report whichever effect used the most
                float used = 0;
                for (StatusEffectInstance instance : effects) {
                    StatusEffect effect = instance.getEffectType();
                    if (effect.isInstant()) {
                        // instant effects just apply full value always
                        used = level.value();
                        if (action.execute()) {
                            target.timeUntilRegen = 0;
                            effect.applyInstantEffect(attacker, attacker, target, instance.getAmplifier(), used * this.scale);
                        }
                    } else {
                        // if the potion already exists, we scale up the existing time
                        StatusEffectInstance existingEffect = target.getStatusEffect(effect);
                        int duration;
                        if (existingEffect != null && existingEffect.getAmplifier() >= instance.getAmplifier()) {
                            float existingLevel = existingEffect.getDuration() / this.scale / instance.getDuration();
                            float effective = level.effective(existingLevel);
                            // no potion to add? just save effort and stop here
                            if (effective <= existingLevel) {
                                continue;
                            }
                            duration = (int) (instance.getDuration() * this.scale * effective);
                            // update how much we used, which is likely less than our max possible
                            used = Math.max(used, effective - existingLevel);
                        } else {
                            // no relevant effect? just compute duration directly
                            used = level.value();
                            duration = (int) (instance.getDuration() * this.scale * used);
                        }
                        if (action.execute()) {
                            target.addStatusEffect(new StatusEffectInstance(effect, duration, instance.getAmplifier(), instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
                        }
                    }
                }
                target.timeUntilRegen = oldInvulnerableTime;
                return used;
            }
        }
        return 0;
    }
}
