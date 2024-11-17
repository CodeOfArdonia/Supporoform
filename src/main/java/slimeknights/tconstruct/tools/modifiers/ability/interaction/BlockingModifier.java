package slimeknights.tconstruct.tools.modifiers.ability.interaction;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class BlockingModifier extends NoLevelsModifier implements GeneralInteractionModifierHook, ToolActionModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_ACTION);
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken()) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAction.BLOCK;
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry entry, ToolAction toolAction) {
        return toolAction == ToolActions.SHIELD_BLOCK;
    }

    /**
     * Makes the tool use the blocking animation if the blocking modifier is installed, falling back to the given animation.
     * Allows your tool to block while charging up.
     */
    public static UseAction blockWhileCharging(IToolStackView tool, UseAction fallback) {
        return ModifierUtil.canPerformAction(tool, ToolActions.SHIELD_BLOCK) ? UseAction.BLOCK : fallback;
    }
}
