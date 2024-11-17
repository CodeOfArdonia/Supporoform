package slimeknights.tconstruct.tools.modifiers.traits.melee;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;

/**
 * Deals a percentage boost in damage when on fire
 */
public class ConductingModifier extends Modifier implements ConditionalStatModifierHook, TooltipModifierHook, MeleeDamageModifierHook {
    private static final Text BOOST = TConstruct.makeTranslation("modifier", "conducting.boost");
    private static final int MAX_BONUS_TICKS = 15 * 20; // time from lava
    private static final float PERCENT_PER_LEVEL = 0.15f; // 15% bonus when in lava essentially

    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.CONDITIONAL_STAT, ModifierHooks.TOOLTIP, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public int getPriority() {
        return 90;
    }

    /**
     * Gets the bonus damage for the given entity and level
     */
    public static float bonusScale(LivingEntity living) {
        int fire = living.getFireTicks();
        if (fire > 0) {
            float bonus = 1;
            // if less than 15 seconds of fire, smaller boost
            if (fire < MAX_BONUS_TICKS) {
                bonus *= (float) (fire) / MAX_BONUS_TICKS;
            }
            // half boost if not on fire
            if (living.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                bonus /= 2;
            }
            return bonus;
        }
        return 0;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        float bonus = bonusScale(context.getAttacker()) * modifier.getEffectiveLevel() * PERCENT_PER_LEVEL;
        if (bonus > 0) {
            damage *= 1 + bonus;
        }
        return damage;
    }

    @Override
    public float modifyStat(IToolStackView tool, ModifierEntry modifier, LivingEntity living, FloatToolStat stat, float baseValue, float multiplier) {
        if (stat == ToolStats.PROJECTILE_DAMAGE) {
            float bonus = bonusScale(living) * modifier.getEffectiveLevel() * PERCENT_PER_LEVEL;
            if (bonus > 0) {
                baseValue *= 1 + bonus;
            }
        }
        return baseValue;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey key, TooltipContext tooltipFlag) {
        if (tool.hasTag(TinkerTags.Items.RANGED) || tool.hasTag(TinkerTags.Items.MELEE)) {
            float bonus = (PERCENT_PER_LEVEL) * modifier.getLevel();
            // client only knows if the player is on fire or not, not the amount of fire, so just show full if on fire
            if (player != null && key == TooltipKey.SHIFT && player.getFireTicks() == 0) {
                bonus = 0;
            }
            tooltip.add(this.applyStyle(Text.literal(Util.PERCENT_BOOST_FORMAT.format(bonus) + " ").append(BOOST)));
        }
    }
}
