package slimeknights.tconstruct.library.modifiers.fluid.block;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidMobEffect;

import java.util.List;

/**
 * Effect to create a lingering cloud at the hit block
 *
 * @see FluidMobEffect.Builder
 */
public record MobEffectCloudFluidEffect(List<FluidMobEffect> effects) implements FluidEffect<FluidEffectContext.Block> {
    public static final RecordLoadable<MobEffectCloudFluidEffect> LOADER = RecordLoadable.create(
            FluidMobEffect.LOADABLE.list(1).requiredField("effects", e -> e.effects),
            MobEffectCloudFluidEffect::new);

    @Override
    public RecordLoadable<MobEffectCloudFluidEffect> getLoader() {
        return LOADER;
    }

    /**
     * Makes a cloud for the given context and size
     */
    public static AreaEffectCloudEntity makeCloud(FluidEffectContext.Block context) {
        Vec3d location = context.getHitResult().getPos();
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(context.getLevel(), location.getX(), location.getY(), location.getZ());
        cloud.setOwner(context.getEntity());
        cloud.setRadius(1);
        cloud.setRadiusOnUse(-0.5f);
        cloud.setWaitTime(10);
        cloud.setRadiusGrowth(-cloud.getRadius() / cloud.getDuration());
        return cloud;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Block context, FluidAction action) {
        if (context.isOffsetReplaceable()) {
            float scale = level.value();
            if (action.execute()) {
                AreaEffectCloudEntity cloud = makeCloud(context);
                boolean hasEffects = false;
                for (FluidMobEffect effect : this.effects) {
                    int time = (int) (effect.time() * scale);
                    if (time > 10) {
                        cloud.addEffect(effect.effectWithTime(time));
                        hasEffects = true;
                    }
                }
                if (hasEffects) {
                    context.getLevel().spawnEntity(cloud);
                } else {
                    cloud.discard();
                    return 0;
                }
            }
            return scale;
        }
        return 0;
    }
}
