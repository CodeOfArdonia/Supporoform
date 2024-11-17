package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Modifier to handle spilling recipes onto self when attacked
 */
public class WettingModifier extends UseFluidOnHitModifier implements ModifyDamageModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
    }

    @Override
    public FluidEffectContext.Entity createContext(LivingEntity self, @Nullable PlayerEntity player, @Nullable Entity attacker) {
        return new FluidEffectContext.Entity(self.getWorld(), self, player, null, self, self);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (!source.isIn(DamageTypeTags.BYPASSES_SHIELD) && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.useFluid(tool, modifier, context, slotType, source);
        }
        return amount;
    }
}
