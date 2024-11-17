package slimeknights.tconstruct.fluids.block;

import com.iafenvoy.uranus.object.DamageUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Liquid block setting the entity on fire
 */
public class BurningLiquidBlock extends FluidBlock {
    /**
     * Burn time in seconds. Lava uses 15
     */
    private final int burnTime;
    /**
     * Damage from being in the fluid, lava uses 4
     */
    private final float damage;

    public BurningLiquidBlock(Supplier<? extends FlowableFluid> supplier, Settings properties, int burnTime, float damage) {
        super(supplier, properties);
        this.burnTime = burnTime;
        this.damage = damage;
    }

    @Override
    public void onEntityCollision(BlockState state, World level, BlockPos pos, Entity entity) {
        if (!entity.isFireImmune() && entity.getFluidTypeHeight(getFluid().getFluidType()) > 0) {
            entity.setOnFireFor(this.burnTime);
            if (entity.damage(DamageUtil.build(entity, DamageTypes.LAVA), this.damage)) {
                entity.playSound(SoundEvents.ENTITY_GENERIC_BURN, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
            }
        }
    }

    /**
     * Creates a new block supplier
     */
    public static Function<Supplier<? extends FlowableFluid>, FluidBlock> createBurning(int lightLevel, int burnTime, float damage) {
        return fluid -> new BurningLiquidBlock(fluid, Settings.create().emissiveLighting(state -> lightLevel).noCollission().strength(100f).noLootTable(), burnTime, damage);
    }
}
