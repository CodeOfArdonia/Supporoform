package slimeknights.tconstruct.library.modifiers.fluid.block;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.recipe.TagPredicate;

import java.util.List;

/**
 * Effect to create a lingering cloud at the hit block
 */
public record PotionCloudFluidEffect(float scale,
                                     TagPredicate predicate) implements FluidEffect<FluidEffectContext.Block> {
    public static final RecordLoadable<PotionCloudFluidEffect> LOADER = RecordLoadable.create(
            FloatLoadable.FROM_ZERO.requiredField("scale", e -> e.scale),
            TagPredicate.LOADABLE.defaultField("nbt", TagPredicate.ANY, e -> e.predicate),
            PotionCloudFluidEffect::new);

    @Override
    public RecordLoadable<PotionCloudFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Block context, FluidAction action) {
        NbtCompound tag = fluid.getTag();
        if (this.predicate.test(tag) && context.isOffsetReplaceable()) {
            Potion potion = PotionUtil.getPotion(fluid.getTag());
            List<StatusEffectInstance> effects = potion.getEffects();
            if (!effects.isEmpty()) {
                float scale = level.value();
                if (action.execute()) {
                    AreaEffectCloudEntity cloud = MobEffectCloudFluidEffect.makeCloud(context);
                    // not using set potion as we want to change the effect duration outself
                    float effectScale = this.scale * scale;
                    // keep track of how many effects are actually added
                    boolean used = false;
                    for (StatusEffectInstance instance : effects) {
                        if (instance.getEffectType().isInstant()) {
                            // only thing we have to scale on instant effects is the amplifier, though clouds automatically half instant effects for us
                            int amplifier = (int) ((instance.getAmplifier() + 1) * effectScale * 2) - 1;
                            if (amplifier >= 0) {
                                cloud.addEffect(new StatusEffectInstance(instance.getEffectType(), instance.getDuration(), amplifier, instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
                                used = true;
                            }
                        } else {
                            int duration = (int) (instance.getDuration() * effectScale);
                            if (duration > 10) {
                                cloud.addEffect(new StatusEffectInstance(instance.getEffectType(), duration, instance.getAmplifier(), instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
                                used = true;
                            }
                        }
                    }
                    // TODO: custom effects from potion NBT?
                    // TODO: custom color from potion NBT?
                    if (used) {
                        context.getLevel().spawnEntity(cloud);
                    } else {
                        cloud.discard();
                        return 0;
                    }
                }
                return scale;
            }
        }
        return 0;
    }
}
