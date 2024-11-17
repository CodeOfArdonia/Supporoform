package slimeknights.tconstruct.plugin.jsonthings.block;

import dev.gigaherz.jsonthings.things.blocks.FlexLiquidBlock;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Json Things version of {@link slimeknights.tconstruct.fluids.block.BurningLiquidBlock}
 */
public class FlexBurningLiquidBlock extends FlexLiquidBlock {
    private final int burnTime;
    private final float damage;

    public FlexBurningLiquidBlock(Properties properties, Map<Property<?>, Comparable<?>> propertyDefaultValues, Supplier<FlowableFluid> fluidSupplier, int burnTime, float damage) {
        super(properties, propertyDefaultValues, fluidSupplier);
        this.burnTime = burnTime;
        this.damage = damage;
    }

    @Override
    public void entityInside(BlockState state, World level, BlockPos pos, Entity entity) {
        if (!entity.isFireImmune() && entity.getFluidTypeHeight(getFluid().getFluidType()) > 0) {
            entity.setOnFireFor(this.burnTime);
            if (entity.damage(DamageSource.LAVA, this.damage)) {
                entity.playSound(SoundEvents.ENTITY_GENERIC_BURN, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
            }
        }
    }
}
