package slimeknights.tconstruct.fluids.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Liquid block setting the entity on fire
 */
public class MobEffectLiquidBlock extends FluidBlock {
    private final Supplier<StatusEffectInstance> effect;

    public MobEffectLiquidBlock(Supplier<? extends FlowableFluid> supplier, Settings properties, Supplier<StatusEffectInstance> effect) {
        super(supplier, properties);
        this.effect = effect;
    }

    @Override
    public void onEntityCollision(BlockState state, World level, BlockPos pos, Entity entity) {
        if (entity.getFluidTypeHeight(getFluid().getFluidType()) > 0 && entity instanceof LivingEntity living) {
            StatusEffectInstance effect = this.effect.get();
            effect.setCurativeItems(new ArrayList<>());
            living.addStatusEffect(effect);
        }
    }

    /**
     * Creates a new block supplier
     */
    public static Function<Supplier<? extends FlowableFluid>, FluidBlock> createEffect(Material material, int lightLevel, Supplier<StatusEffectInstance> effect) {
        return fluid -> new MobEffectLiquidBlock(fluid, Settings.create(material).lightLevel(state -> lightLevel).noCollission().strength(100f).noLootTable(), effect);
    }

    /**
     * Creates a new block supplier
     */
    public static Function<Supplier<? extends FlowableFluid>, FluidBlock> createEffect(int lightLevel, Supplier<StatusEffectInstance> effect) {
        return createEffect(Material.WATER, lightLevel, effect);
    }
}
