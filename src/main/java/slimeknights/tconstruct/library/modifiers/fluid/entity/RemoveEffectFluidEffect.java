package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;

/**
 * Spilling effect to remove a specific effect
 */
public record RemoveEffectFluidEffect(StatusEffect effect) implements FluidEffect<Entity> {
    public static final RecordLoadable<RemoveEffectFluidEffect> LOADER = RecordLoadable.create(Loadables.MOB_EFFECT.requiredField("effect", e -> e.effect), RemoveEffectFluidEffect::new);

    @Override
    public RecordLoadable<RemoveEffectFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
        LivingEntity living = context.getLivingTarget();
        if (living != null && level.isFull()) {
            if (action.simulate()) {
                return living.hasStatusEffect(this.effect) ? 1 : 0;
            }
            return living.removeStatusEffect(this.effect) ? 1 : 0;
        }
        return 0;
    }
}
