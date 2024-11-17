package slimeknights.tconstruct.tools.modifiers.ability.tool;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import slimeknights.mantle.util.OffhandCooldownTracker;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class OffhandAttackModifier extends NoLevelsModifier implements EntityInteractionModifierHook, GeneralInteractionModifierHook, EquipmentChangeModifierHook, VolatileDataModifierHook {
    public static final Identifier DUEL_WIELDING = TConstruct.getResource("duel_wielding");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.ENTITY_INTERACT, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.VOLATILE_DATA);
        hookBuilder.addModule(ShowOffhandModule.DISALLOW_BROKEN);
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public boolean shouldDisplay(boolean advanced) {
        return false;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        volatileData.putBoolean(DUEL_WIELDING, true);
    }

    /**
     * If true, we can use the attack
     */
    protected boolean canAttack(IToolStackView tool, PlayerEntity player, Hand hand) {
        return !tool.isBroken() && hand == Hand.OFF_HAND && OffhandCooldownTracker.isAttackReady(player);
    }

    @Override
    public ActionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity target, Hand hand, InteractionSource source) {
        if (this.canAttack(tool, player, hand)) {
            if (!player.getWorld().isClient) {
                ToolAttackUtil.attackEntity(tool, player, Hand.OFF_HAND, target, ToolAttackUtil.getCooldownFunction(player, Hand.OFF_HAND), false, source.getSlot(hand));
            }
            // for armor, always assume attack speed is 4.0, we cannot change the attack speed of the main hand and we want them to match
            OffhandCooldownTracker.applyCooldown(player, source == InteractionSource.ARMOR ? 4 : tool.getStats().get(ToolStats.ATTACK_SPEED), 20);
            // we handle swinging the arm, return consume to prevent resetting cooldown
            OffhandCooldownTracker.swingHand(player, Hand.OFF_HAND, false);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (this.canAttack(tool, player, hand)) {
            // target done in onEntityInteract, this is just for cooldown cause you missed
            OffhandCooldownTracker.applyCooldown(player, source == InteractionSource.ARMOR ? 4 : tool.getStats().get(ToolStats.ATTACK_SPEED), 20);
            // we handle swinging the arm, return consume to prevent resetting cooldown
            OffhandCooldownTracker.swingHand(player, Hand.OFF_HAND, false);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (!tool.isBroken() && context.getChangedSlot() == EquipmentSlot.OFFHAND) {
            context.getEntity().getCapability(OffhandCooldownTracker.CAPABILITY).ifPresent(cap -> cap.setEnabled(true));
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (!tool.isBroken() && context.getChangedSlot() == EquipmentSlot.OFFHAND) {
            context.getEntity().getCapability(OffhandCooldownTracker.CAPABILITY).ifPresent(cap -> cap.setEnabled(false));
        }
    }
}
