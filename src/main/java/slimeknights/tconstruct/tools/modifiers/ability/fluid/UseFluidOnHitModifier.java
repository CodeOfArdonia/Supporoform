package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.particle.FluidParticleData;

import static slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER;

/**
 * Modifier to handle spilling recipes onto self when attacked
 */
public abstract class UseFluidOnHitModifier extends Modifier {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
        hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(FluidType.BUCKET_VOLUME));
    }

    /**
     * Spawns particles at the given entity
     */
    public static void spawnParticles(Entity target, FluidStack fluid) {
        if (target.getWorld() instanceof ServerWorld) {
            ((ServerWorld) target.getWorld()).spawnParticles(new FluidParticleData(TinkerCommons.fluidParticle.get(), fluid), target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.1, 0.2, 0.1, 0.2);
        }
    }

    /**
     * Overridable method to create the attack context and spawn particles
     */
    public abstract FluidEffectContext.Entity createContext(LivingEntity self, @Nullable PlayerEntity player, @Nullable Entity attacker);

    /**
     * Logic for using the fluid
     */
    protected void useFluid(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source) {
        // 25% chance of working per level, 50% per level on shields
        float level = modifier.getEffectiveLevel();
        if (RANDOM.nextInt(slotType.getType() == Type.HAND ? 2 : 4) < level) {
            FluidStack fluid = TANK_HELPER.getFluid(tool);
            if (!fluid.isEmpty()) {
                LivingEntity self = context.getEntity();
                PlayerEntity player = self instanceof PlayerEntity p ? p : null;
                FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
                if (recipe.hasEffects()) {
                    FluidEffectContext.Entity fluidContext = this.createContext(self, player, source.getAttacker());
                    int consumed = recipe.applyToEntity(fluid, level, fluidContext, FluidAction.EXECUTE);
                    if (consumed > 0 && (player == null || !player.isCreative())) {
                        spawnParticles(fluidContext.getTarget(), fluid);
                        fluid.shrink(consumed);
                        TANK_HELPER.setFluid(tool, fluid);
                    }
                }
            }
        }
    }
}
