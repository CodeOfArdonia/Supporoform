package slimeknights.tconstruct.tools.modifiers.traits.skull;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.DamageDealtModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

public class FrosttouchModifier extends NoLevelsModifier implements DamageDealtModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.DAMAGE_DEALT);
        hookBuilder.addModule(StrongBonesModifier.CALCIFIABLE_MODULE);
    }

    @Override
    public void onDamageDealt(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, LivingEntity target, DamageSource source, float amount, boolean isDirectDamage) {
        // must drink milk to melee slowness. Always can range slowness
        if (isDirectDamage) {
            boolean isCalcified = context.getEntity().hasEffect(TinkerModifiers.calcifiedEffect.get());
            if (isCalcified || source.isProjectile()) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, isCalcified ? 1 : 0));
            }
        }
    }
}
