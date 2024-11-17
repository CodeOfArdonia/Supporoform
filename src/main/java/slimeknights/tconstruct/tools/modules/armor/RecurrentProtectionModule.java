package slimeknights.tconstruct.tools.modules.armor;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.stats.ToolType;

import java.util.List;

/**
 * Module for boosting protection after taking damage
 */
public record RecurrentProtectionModule(
        LevelingValue amount) implements ModifierModule, ProtectionModifierHook, ModifyDamageModifierHook, TooltipModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<RecurrentProtectionModule>defaultHooks(ModifierHooks.PROTECTION, ModifierHooks.MODIFY_DAMAGE, ModifierHooks.TOOLTIP);
    public static final RecordLoadable<RecurrentProtectionModule> LOADER = RecordLoadable.create(
            LevelingValue.LOADABLE.directField(RecurrentProtectionModule::amount),
            RecurrentProtectionModule::new);

    @Override
    public RecordLoadable<RecurrentProtectionModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public float getProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float modifierValue) {
        if (DamageSourcePredicate.CAN_PROTECT.matches(source) && context.getEntity().hasEffect(TinkerModifiers.momentumEffect.get(ToolType.ARMOR))) {
            modifierValue += this.amount.compute(modifier.getEffectiveLevel());
        }
        return modifierValue;
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        // does not hurt to add multiple copies
        if (source.getAttacker() != null) {
            TinkerModifiers.momentumEffect.get(ToolType.ARMOR).apply(context.getEntity(), 5 * 20, 0, true);
        }
        return amount;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        float bonus = this.amount.compute(modifier.getEffectiveLevel());
        if (bonus > 0 && (player == null || tooltipKey != TooltipKey.SHIFT || player.hasStatusEffect(TinkerModifiers.momentumEffect.get(ToolType.ARMOR)))) {
            ProtectionModule.addResistanceTooltip(tool, modifier.getModifier(), bonus, player, tooltip);
        }
    }
}
