package slimeknights.tconstruct.plugin.jsonthings.block;

import dev.gigaherz.jsonthings.things.blocks.FlexLiquidBlock;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Json Things version of {@link slimeknights.tconstruct.fluids.block.BurningLiquidBlock}
 */
public class FlexMobEffectLiquidBlock extends FlexLiquidBlock {
    private final Supplier<StatusEffectInstance> effect;

    public FlexMobEffectLiquidBlock(Properties properties, Map<Property<?>, Comparable<?>> propertyDefaultValues, Supplier<FlowableFluid> fluidSupplier, Supplier<StatusEffectInstance> effect) {
        super(properties, propertyDefaultValues, fluidSupplier);
        this.effect = effect;
    }

    @Override
    public void entityInside(BlockState state, World level, BlockPos pos, Entity entity) {
        if (entity.getFluidTypeHeight(getFluid().getFluidType()) > 0 && entity instanceof LivingEntity living) {
            StatusEffectInstance effect = this.effect.get();
            effect.setCurativeItems(new ArrayList<>());
            living.addStatusEffect(effect);
        }
    }
}
