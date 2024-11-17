package slimeknights.tconstruct.library.modifiers.fluid.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;

/**
 * Effect to award a stat to a player
 *
 * @param stat   Stat to award
 * @param amount Amount to reward, can be negative
 */
public record AwardStatFluidEffect(Identifier stat, int amount) implements FluidEffect<Entity> {
    public static final RecordLoadable<AwardStatFluidEffect> LOADER = RecordLoadable.create(
            TinkerLoadables.CUSTOM_STAT.requiredField("stat", e -> e.stat),
            IntLoadable.ANY_SHORT.requiredField("amount", e -> e.amount),
            AwardStatFluidEffect::new);

    @Override
    public RecordLoadable<AwardStatFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
        if (context.getLivingTarget() instanceof PlayerEntity player) {
            float value = level.value();
            if (action.execute()) {
                player.increaseStat(Stats.CUSTOM.getOrCreateStat(this.stat), Math.round(this.amount * value));
            }
            return value;
        }
        return 0;
    }
}
