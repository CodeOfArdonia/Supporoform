package slimeknights.tconstruct.library.modifiers.hook.behavior;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;

/**
 * Modifier hook for adding attributes to a tool when in the correct slot.
 */
public interface AttributesModifierHook {
    /**
     * UUIDs for armor attributes on held tools
     */
    UUID[] HELD_ARMOR_UUID = new UUID[]{UUID.fromString("00a1a5fe-43b5-4849-8660-de9aa497736a"), UUID.fromString("6776fd7e-4b22-4cdf-a0bc-bb8d2ad1f0bf")};

    /**
     * Adds attributes from this modifier's effect. Called whenever the item stack refreshes attributes, typically on equipping and unequipping.
     * It is important that you return the same list when equipping and unequipping the item.
     * <br>
     * Alternatives:
     * <ul>
     *   <li>{@link ToolStatsModifierHook}: Limited context, but can affect durability, mining level, and mining speed.</li>
     * </ul>
     *
     * @param tool     Current tool instance
     * @param modifier Modifier level
     * @param slot     Slot for the attributes
     * @param consumer Attribute consumer
     */
    void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<EntityAttribute, EntityAttributeModifier> consumer);

    /**
     * Gets attribute modifiers for a weapon with melee capability
     *
     * @param tool Tool instance
     * @param slot Held slot
     * @return Map of attribute modifiers
     */
    static Multimap<EntityAttribute, EntityAttributeModifier> getHeldAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        if (!tool.isBroken()) {
            // base stats
            StatsNBT statsNBT = tool.getStats();
            if (slot == EquipmentSlot.MAINHAND && tool.hasTag(TinkerTags.Items.MELEE_WEAPON)) {
                builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(Item.ATTACK_DAMAGE_MODIFIER_ID, "tconstruct.tool.attack_damage", statsNBT.get(ToolStats.ATTACK_DAMAGE), EntityAttributeModifier.Operation.ADDITION));
                // base attack speed is 4, but our numbers start from 4
                builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(Item.ATTACK_SPEED_MODIFIER_ID, "tconstruct.tool.attack_speed", statsNBT.get(ToolStats.ATTACK_SPEED) - 4d, EntityAttributeModifier.Operation.ADDITION));
            }

            if (slot.getType() == Type.HAND) {
                // shields and slimestaffs can get armor
                if (tool.hasTag(TinkerTags.Items.ARMOR)) {
                    UUID uuid = HELD_ARMOR_UUID[slot.getEntitySlotId()];
                    double value = statsNBT.get(ToolStats.ARMOR);
                    if (value != 0) {
                        builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, "tconstruct.held.armor", value, EntityAttributeModifier.Operation.ADDITION));
                    }
                    value = statsNBT.get(ToolStats.ARMOR_TOUGHNESS);
                    if (value != 0) {
                        builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, "tconstruct.held.toughness", value, EntityAttributeModifier.Operation.ADDITION));
                    }
                    value = statsNBT.get(ToolStats.KNOCKBACK_RESISTANCE);
                    if (value != 0) {
                        builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(uuid, "tconstruct.held.knockback_resistance", value, EntityAttributeModifier.Operation.ADDITION));
                    }
                }

                // grab attributes from modifiers, only do for hands (other slots would just be weird)
                BiConsumer<EntityAttribute, EntityAttributeModifier> attributeConsumer = builder::put;
                for (ModifierEntry entry : tool.getModifierList()) {
                    entry.getHook(ModifierHooks.ATTRIBUTES).addAttributes(tool, entry, slot, attributeConsumer);
                }
            }
        }
        return builder.build();
    }

    /**
     * Merger that runs all hooks
     */
    record AllMerger(Collection<AttributesModifierHook> modules) implements AttributesModifierHook {
        @Override
        public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<EntityAttribute, EntityAttributeModifier> consumer) {
            for (AttributesModifierHook module : this.modules) {
                module.addAttributes(tool, modifier, slot, consumer);
            }
        }
    }
}
