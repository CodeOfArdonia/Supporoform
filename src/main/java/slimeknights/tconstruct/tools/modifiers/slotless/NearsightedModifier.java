package slimeknights.tconstruct.tools.modifiers.slotless;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Arrays;
import java.util.Comparator;

public class NearsightedModifier extends Modifier implements EquipmentChangeModifierHook {
    private final Identifier[] SLOT_KEYS = Arrays.stream(EquipmentSlot.values())
            .sorted(Comparator.comparing(EquipmentSlot::getArmorStandSlotId))
            .map(slot -> TConstruct.getResource("nearsighted_" + slot.getName()))
            .toArray(Identifier[]::new);

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (!tool.isBroken()) {
            Identifier key = this.SLOT_KEYS[context.getChangedSlot().getArmorStandSlotId()];
            context.getTinkerData().ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(key, 1 + 0.05f * modifier.getLevel()));
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (!tool.isBroken()) {
            Identifier key = this.SLOT_KEYS[context.getChangedSlot().getArmorStandSlotId()];
            context.getTinkerData().ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(key));
        }
    }
}
