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
 * Effect to set an entity freezing
 *
 * @param action Determines whether to set or add time
 * @param time   Time in seconds
 */
public record FreezeFluidEffect(TimeAction action, int time) implements FluidEffect<FluidEffectContext.Entity> {
    public static final RecordLoadable<FreezeFluidEffect> LOADER = RecordLoadable.create(
            TimeAction.LOADABLE.requiredField("action", e -> e.action),
            IntLoadable.FROM_ONE.requiredField("time", e -> e.time),
            FreezeFluidEffect::new);

    @Override
    public RecordLoadable<FreezeFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Entity context, FluidAction action) {
        Entity target = context.getTarget();
        if (!target.canFreeze()) {
            return 0;
        }
        if (this.action == TimeAction.ADD) {
            float value = level.value();
            if (action.execute()) {
                // ensure we have enough time to freeze after applying this
                target.setFrozenTicks(Math.max(target.getMinFreezeDamageTicks(), target.getFrozenTicks()) + Math.round(this.time * value));
                target.setFireTicks(0);
            }
            return value;
        } else {
            int freezeTicks = target.getMinFreezeDamageTicks();
            int frozen = target.getFrozenTicks();
            float existing = frozen < freezeTicks ? 0 : frozen / (float) this.time;
            float effective = level.effective(existing);
            if (action.execute()) {
                target.setFrozenTicks(freezeTicks + Math.round(this.time * effective));
                target.setFireTicks(0);
            }
            return effective - existing;
        }
    }
}
