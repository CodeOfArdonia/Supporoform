package slimeknights.tconstruct.tools.modifiers.ability.sling;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.ability.interaction.BlockingModifier;
import slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier;

/**
 * Shared logic for all slinging modifiers
 */
public abstract class SlingModifier extends NoLevelsModifier implements GeneralInteractionModifierHook {
    @Override
    protected void registerHooks(Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, 1.5f);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }


    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        ScopeModifier.scopingUsingTick(tool, entity, this.getUseDuration(tool, modifier) - timeLeft);
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        ScopeModifier.stopScoping(entity);
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return BlockingModifier.blockWhileCharging(tool, UseAction.BOW);
    }

    /**
     * Gets the force to launch the sling at, considers drawspeed and velocity
     */
    protected float getForce(IToolStackView tool, ModifierEntry entry, LivingEntity living, int timeLeft, boolean applyKnockback) {
        int chargeTime = this.getUseDuration(tool, entry) - timeLeft;
        if (chargeTime < 0) {
            return 0;
        }
        // if using knockback, boost projectile damage by 0.5 per level, that is a 25% boost, same as power
        // TODO: this is pretty hardcoded, is there a good way to softcode this?
        float knockback = 0;
        if (applyKnockback) {
            knockback = tool.getModifierLevel(TinkerModifiers.knockback.getId()) / 2f;
        }
        float force = GeneralInteractionModifierHook.getToolCharge(tool, chargeTime)
                * (ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.PROJECTILE_DAMAGE) + knockback) / 2f
                * ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.VELOCITY);
        // knockback also means we should apply padded, divide per level
        if (applyKnockback) {
            force /= Math.pow(2, tool.getModifierLevel(TinkerModifiers.padded.getId()));
        }
        return force;
    }
}
