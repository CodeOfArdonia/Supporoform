package slimeknights.tconstruct.tools.modifiers.traits.skull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class RevengeModifier extends NoLevelsModifier implements EquipmentChangeModifierHook, OnAttackedModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        // must be attacked by entity
        Entity trueSource = source.getAttacker();
        LivingEntity living = context.getEntity();
        if (trueSource != null && trueSource != living) { // no making yourself mad with slurping or self-destruct or alike
            StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.STRENGTH, 300);
            effect.getCurativeItems().clear();
            effect.getCurativeItems().add(new ItemStack(living.getEquippedStack(slotType).getItem()));
            living.addStatusEffect(effect);
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getChangedSlot() == EquipmentSlot.HEAD) {
            IToolStackView replacement = context.getReplacementTool();
            if (replacement == null || replacement.getModifierLevel(this) == 0) {
                // cure effects using the helmet
                context.getEntity().curePotionEffects(new ItemStack(tool.getItem()));
            }
        }
    }
}
