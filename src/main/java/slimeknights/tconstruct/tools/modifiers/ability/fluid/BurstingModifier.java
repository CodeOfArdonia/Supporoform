package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import static slimeknights.tconstruct.library.tools.helper.ModifierUtil.asLiving;

/**
 * Modifier to handle spilling recipes
 */
public class BurstingModifier extends UseFluidOnHitModifier implements OnAttackedModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public FluidEffectContext.Entity createContext(LivingEntity self, @Nullable PlayerEntity player, @Nullable Entity attacker) {
        assert attacker != null;
        return new FluidEffectContext.Entity(self.getWorld(), self, player, null, attacker, asLiving(attacker));
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (source.getAttacker() != null && isDirectDamage) {
            this.useFluid(tool, modifier, context, slotType, source);
        }
    }
}
