package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;

/**
 * Effect to clear all effects using the given stack
 *
 * @param stack Stack used for curing, standard is milk bucket
 */
public record CureEffectsFluidEffect(ItemStack stack) implements FluidEffect<Entity> {
    public static final RecordLoadable<CureEffectsFluidEffect> LOADER = RecordLoadable.create(ItemStackLoadable.REQUIRED_ITEM.requiredField("item", e -> e.stack), CureEffectsFluidEffect::new);

    public CureEffectsFluidEffect(ItemConvertible item) {
        this(new ItemStack(item));
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
        LivingEntity target = context.getLivingTarget();
        if (target != null && level.isFull()) {
            // when simulating, search the effects list directly for curative effects
            // may still be wrong if the event cancels things though, no way to safely simulate it
            if (action.simulate()) {
                return target.getStatusEffects().stream().anyMatch(effect -> effect.isCurativeItem(this.stack)) ? 1 : 0;
            }
            return target.curePotionEffects(this.stack) ? 1 : 0;
        }
        return 0;
    }

    @Override
    public RecordLoadable<CureEffectsFluidEffect> getLoader() {
        return LOADER;
    }
}
