package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;
import slimeknights.tconstruct.library.modifiers.fluid.FluidMobEffect;
import slimeknights.tconstruct.library.modifiers.fluid.TimeAction;

/**
 * Spilling effect to apply a potion effect
 *
 * @param effect Effect to apply
 * @param action How the time scales
 * @see FluidMobEffect.Builder
 */
public record MobEffectFluidEffect(FluidMobEffect effect, TimeAction action) implements FluidEffect<Entity> {
    public static final RecordLoadable<MobEffectFluidEffect> LOADER = RecordLoadable.create(
            FluidMobEffect.LOADABLE.directField(e -> e.effect),
            TimeAction.LOADABLE.requiredField("action", e -> e.action),
            MobEffectFluidEffect::new);

    @Override
    public RecordLoadable<MobEffectFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel scale, Entity context, FluidAction action) {
        // first, need a target
        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            float used;
            int time;
            // add and set both have distinct behavior under an existing effect, same otherwise
            StatusEffectInstance existingInstance = target.getStatusEffect(this.effect.effect());
            if (existingInstance != null && existingInstance.getAmplifier() >= this.effect.amplifier()) {
                if (this.action == TimeAction.ADD) {
                    int extraTime = (int) (this.effect.time() * scale.value());
                    if (extraTime <= 0) {
                        return 0;
                    }
                    // add simply sums existing time with the desired time. If the level is higher than ours, it produces a hidden effect
                    time = existingInstance.getDuration() + extraTime;
                    used = scale.value();
                } else {
                    // set can produce time up to the maximum allowed by scale,
                    float existing = existingInstance.getDuration() / (float) this.effect.time();
                    float effective = scale.effective(existing);
                    // no change? means we skip applying entirely
                    if (effective < existing) {
                        return 0;
                    }
                    used = effective - existing;
                    time = (int) (this.effect.time() * effective);
                }
            } else {
                // if no existing, time and set behave the same way, use maximum amount to compute time
                time = (int) (this.effect.time() * scale.value());
                used = scale.value();
            }
            // if we got time, add the effect
            if (time > 0) {
                StatusEffectInstance newInstance = this.effect.effectWithTime(time);
                // if simulating, just ask if it's applicable. Won't handle event canceling, but thats acceptable.
                if (action.simulate()) {
                    return target.canHaveStatusEffect(newInstance) ? used : 0;
                }
                return target.addStatusEffect(newInstance) ? used : 0;
            }
        }
        return 0;
    }
}
