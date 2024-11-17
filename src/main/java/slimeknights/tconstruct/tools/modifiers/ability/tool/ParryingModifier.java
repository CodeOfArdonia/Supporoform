package slimeknights.tconstruct.tools.modifiers.ability.tool;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class ParryingModifier extends OffhandAttackModifier implements ToolActionModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_ACTION);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean shouldDisplay(boolean advanced) {
        return true;
    }

    @Override
    public ActionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity target, Hand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK) {
            ActionResult result = super.beforeEntityUse(tool, modifier, player, target, hand, source);
            if (result.isAccepted()) {
                GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            }
            return result;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && hand == Hand.OFF_HAND) {
            ActionResult result = super.onToolUse(tool, modifier, player, hand, source);
            // also allow just blocking when used in main hand
            if (result.isAccepted()) {
                GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(tool.getItem(), (int) (20 / tool.getStats().get(ToolStats.ATTACK_SPEED)));
        }
    }

    @Override
    public UseAction getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAction.BLOCK;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 20;
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        return toolAction == ToolActions.SHIELD_BLOCK;
    }
}
