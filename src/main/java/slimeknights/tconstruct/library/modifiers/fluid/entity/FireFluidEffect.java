package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.Entity;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.TimeAction;

/**
 * Effect to set an entity on fire
 *
 * @param action Determines whether to set or add time
 * @param time   Time in seconds
 */
public record FireFluidEffect(TimeAction action, int time) implements FluidEffect<FluidEffectContext.Entity> {
    public static final RecordLoadable<FireFluidEffect> LOADER = RecordLoadable.create(
            TimeAction.LOADABLE.requiredField("action", e -> e.action),
            IntLoadable.FROM_ONE.requiredField("time", e -> e.time),
            FireFluidEffect::new);

    @Override
    public RecordLoadable<FireFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Entity context, FluidAction action) {
        // if fire immune or bad parameters, fail
        Entity target = context.getTarget();
        if (target.isFireImmune()) {
            return 0;
        }
        if (this.action == TimeAction.ADD) {
            float value = level.value();
            if (action.execute()) {
                // current time is in ticks, so need to divide to get seconds, do a rounded divide
                target.setOnFireFor(Math.round(this.time * value) + (target.getFireTicks() + 10) / 20);
            }
            return value;
        } else {
            // we are allowed to increase fire up to time*level.max, however we may get less if level.value is low
            float existing = target.getFireTicks() / 20f / this.time;
            float effective = level.effective(existing);
            if (action.execute()) {
                target.setOnFireFor(Math.round(this.time * effective));
            }
            // only consume what we changed
            return effective - existing;
        }
    }
}
