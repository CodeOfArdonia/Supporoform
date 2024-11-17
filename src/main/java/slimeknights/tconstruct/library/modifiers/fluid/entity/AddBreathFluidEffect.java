package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;

/**
 * Effect to increase or decrease the target's breath.
 *
 * @param amount Amount to restore
 */
public record AddBreathFluidEffect(int amount) implements FluidEffect<Entity> {
    public static final RecordLoadable<AddBreathFluidEffect> LOADER = RecordLoadable.create(
            IntLoadable.ANY_SHORT.requiredField("amount", e -> e.amount),
            AddBreathFluidEffect::new);

    @Override
    public RecordLoadable<AddBreathFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            int max = target.getMaxAir();
            int current = target.getAir();
            if (action.execute()) {
                target.setAir(MathHelper.clamp(current + Math.round(this.amount * level.value()), 0, max));
            }
            // based on whether we are increasing or decreasing breath, the max change varies
            // only consume fluid based on the air we got/lost
            int maxChange = this.amount > 0 ? max - current : current;
            return level.computeUsed(maxChange / (float) Math.abs(this.amount));
        }
        return 0;
    }
}
