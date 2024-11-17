package slimeknights.tconstruct.tools.modifiers.upgrades.melee;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

public class PiercingModifier extends Modifier implements ToolStatsModifierHook, MeleeHitModifierHook, TooltipModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.MELEE_HIT, ModifierHooks.TOOLTIP);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        ToolStats.ATTACK_DAMAGE.add(builder, -0.5f * modifier.getEffectiveLevel());
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        // deals 0.5 pierce damage per level, scaled, half of sharpness
        DamageSource source;
        PlayerEntity player = context.getPlayerAttacker();
        if (player != null) {
            source = DamageSource.playerAttack(player);
        } else {
            source = DamageSource.mobAttack(context.getAttacker());
        }
        source.bypassArmor();
        float secondaryDamage = (modifier.getEffectiveLevel() * tool.getMultiplier(ToolStats.ATTACK_DAMAGE)) * context.getCooldown();
        if (context.isCritical()) {
            secondaryDamage *= 1.5f;
        }
        ToolAttackUtil.attackEntitySecondary(source, secondaryDamage, context.getTarget(), context.getLivingTarget(), true);
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        TooltipModifierHook.addDamageBoost(tool, this, modifier.getEffectiveLevel(), tooltip);
    }
}
