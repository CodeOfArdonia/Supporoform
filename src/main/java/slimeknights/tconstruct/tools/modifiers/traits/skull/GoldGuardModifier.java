package slimeknights.tconstruct.tools.modifiers.traits.skull;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;
import java.util.UUID;

public class GoldGuardModifier extends NoLevelsModifier implements EquipmentChangeModifierHook, TooltipModifierHook {
    private static final UUID GOLD_GUARD_UUID = UUID.fromString("fbae11f1-b547-47e8-ae0c-f2cf24a46d93");
    private static final ComputableDataKey<GoldGuardGold> TOTAL_GOLD = TConstruct.createKey("gold_guard", GoldGuardGold::new);

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.TOOLTIP);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        // adding a helmet? activate bonus
        if (context.getChangedSlot() == EquipmentSlot.HEAD) {
            context.getTinkerData().ifPresent(data -> {
                GoldGuardGold gold = data.get(TOTAL_GOLD);
                if (gold == null) {
                    data.computeIfAbsent(TOTAL_GOLD).initialize(context);
                } else {
                    gold.setGold(EquipmentSlot.HEAD, tool.getVolatileData().getBoolean(ModifiableArmorItem.PIGLIN_NEUTRAL), context.getEntity());
                }
            });
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getChangedSlot() == EquipmentSlot.HEAD) {
            IToolStackView newTool = context.getReplacementTool();
            // when replacing with a helmet that lacks this modifier, remove bonus
            if (newTool == null || newTool.getModifierLevel(this) == 0) {
                context.getTinkerData().ifPresent(data -> data.remove(TOTAL_GOLD));
                EntityAttributeInstance instance = context.getEntity().getAttribute(EntityAttributes.GENERIC_MAX_HEALTH);
                if (instance != null) {
                    instance.removeModifier(GOLD_GUARD_UUID);
                }
            }
        }
    }

    @Override
    public void onEquipmentChange(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context, EquipmentSlot slotType) {
        // adding a helmet? activate bonus
        EquipmentSlot changed = context.getChangedSlot();
        if (slotType == EquipmentSlot.HEAD && changed.getType() == Type.ARMOR) {
            LivingEntity living = context.getEntity();
            boolean hasGold = ChrysophiliteModifier.hasGold(context, changed);
            context.getTinkerData().ifPresent(data -> data.computeIfAbsent(TOTAL_GOLD).setGold(changed, hasGold, living));
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry entry, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        if (player != null && tooltipKey == TooltipKey.SHIFT) {
            EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (instance != null) {
                EntityAttributeModifier modifier = instance.getModifier(GOLD_GUARD_UUID);
                if (modifier != null) {
                    tooltip.add(this.applyStyle(Text.literal(Util.BONUS_FORMAT.format(modifier.getValue()) + " ")
                            .append(Text.translatable(this.getTranslationKey() + "." + "health"))));
                }
            }
        }
    }

    /**
     * Internal logic to update gold on the player
     */
    private static class GoldGuardGold extends ChrysophiliteModifier.TotalGold {
        /**
         * Adds the health boost to the player
         */
        private void updateAttribute(LivingEntity living) {
            // update attribute
            EntityAttributeInstance instance = living.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (instance != null) {
                if (instance.getModifier(GOLD_GUARD_UUID) != null) {
                    instance.removeModifier(GOLD_GUARD_UUID);
                }
                // +2 hearts per level, and a bonus of 2 for having the modifier
                instance.addTemporaryModifier(new EntityAttributeModifier(GOLD_GUARD_UUID, "tconstruct.gold_guard", this.getTotalGold() * 4, Operation.ADDITION));
            }
        }

        /**
         * Sets the slot to having gold or not and updates the attribute
         */
        public void setGold(EquipmentSlot slotType, boolean value, LivingEntity living) {
            if (this.setGold(slotType, value)) {
                this.updateAttribute(living);
            }
        }

        @Override
        public void initialize(EquipmentChangeContext context) {
            super.initialize(context);
            this.updateAttribute(context.getEntity());
        }
    }
}
