package slimeknights.tconstruct.tools.modifiers.ability.armor;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.KeybindInteractModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class ZoomModifier extends NoLevelsModifier implements KeybindInteractModifierHook, GeneralInteractionModifierHook, EquipmentChangeModifierHook {
    private static final Identifier ZOOM = TConstruct.getResource("zoom");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ARMOR_INTERACT, ModifierHooks.GENERAL_INTERACT, ModifierHooks.EQUIPMENT_CHANGE);
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getEntity().getWorld().isClient) {
            IToolStackView replacement = context.getReplacementTool();
            if (replacement == null || replacement.getModifierLevel(this) == 0) {
                context.getTinkerData().ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(ZOOM));
            }
        }
    }

    @Override
    public boolean startInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot slot, TooltipKey keyModifier) {
        if (player.isSneaking()) {
            player.playSound(SoundEvents.ITEM_SPYGLASS_USE, 1.0F, 1.0F);
            if (player.getWorld().isClient) {
                player.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(ZOOM, 0.1f));
            }
            return true;
        }
        return false;
    }

    @Override
    public void stopInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot slot) {
        player.playSound(SoundEvents.ITEM_SPYGLASS_STOP_USING, 1.0F, 1.0F);
        if (player.getWorld().isClient) {
            player.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(ZOOM));
        }
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK) {
            player.playSound(SoundEvents.ITEM_SPYGLASS_USE, 1.0F, 1.0F);
            if (player.getWorld().isClient) {
                player.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(ZOOM, 0.1f));
            }
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    @Override
    public UseAction getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAction.SPYGLASS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 1200;
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        entity.playSound(SoundEvents.ITEM_SPYGLASS_STOP_USING, 1.0F, 1.0F);
        if (entity.getWorld().isClient) {
            entity.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(ZOOM));
        }
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        this.onFinishUsing(tool, modifier, entity);
    }
}
