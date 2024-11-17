package slimeknights.tconstruct.library.modifiers.hook.display;

import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

/**
 * Hook for modifiers to add tooltip information
 */
public interface TooltipModifierHook {
    /**
     * Adds additional information from the modifier to the tooltip. Shown when holding shift on a tool, or in the stats area of the tinker station
     *
     * @param tool        Tool instance
     * @param modifier    Tool level
     * @param player      Player holding this tool
     * @param tooltip     Tooltip
     * @param tooltipKey  Shows if the player is holding shift, control, or neither
     * @param tooltipFlag Flag determining tooltip type
     */
    void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag);

    /**
     * Merger that runs all hooks
     */
    record AllMerger(Collection<TooltipModifierHook> modules) implements TooltipModifierHook {
        @Override
        public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
            for (TooltipModifierHook module : this.modules) {
                module.addTooltip(tool, modifier, player, tooltip, tooltipKey, tooltipFlag);
            }
        }
    }


    /* Helpers */

    /**
     * Gets the name of the stat to display, uses a translation key built from the tool and the stat
     */
    static Text statName(Modifier modifier, IToolStat<?> stat) {
        return Text.translatable(modifier.getTranslationKey() + "." + stat.getName().getPath());
    }

    /**
     * Adds a flat bonus tooltip
     */
    static void addFlatBoost(Modifier modifier, Text name, double bonus, List<Text> tooltip) {
        tooltip.add(modifier.applyStyle(Text.literal(Util.BONUS_FORMAT.format(bonus) + " ").append(name)));
    }

    /**
     * Adds a percentage boost tooltip
     */
    static void addPercentBoost(Modifier modifier, Text name, double bonus, List<Text> tooltip) {
        tooltip.add(modifier.applyStyle(Text.literal(Util.PERCENT_BOOST_FORMAT.format(bonus) + " ").append(name)));
    }

    /**
     * Adds a tooltip showing a bonus stat
     *
     * @param tool      Tool instance
     * @param modifier  Modifier for style
     * @param stat      Stat added
     * @param condition Condition to show the tooltip
     * @param amount    Amount to show, before scaling by the tool's modifier
     * @param tooltip   Tooltip list
     */
    static void addStatBoost(IToolStackView tool, Modifier modifier, FloatToolStat stat, TagKey<Item> condition, float amount, List<Text> tooltip) {
        if (tool.hasTag(condition)) {
            addFlatBoost(modifier, statName(modifier, stat), amount * tool.getMultiplier(stat), tooltip);
        }
    }

    /**
     * Adds a tooltip showing the bonus damage and the type of damage
     *
     * @param tool     Tool instance
     * @param modifier Modifier for style
     * @param amount   Damage amount
     * @param tooltip  Tooltip
     */
    static void addDamageBoost(IToolStackView tool, Modifier modifier, float amount, List<Text> tooltip) {
        addStatBoost(tool, modifier, ToolStats.ATTACK_DAMAGE, TinkerTags.Items.MELEE, amount, tooltip);
    }

    /**
     * Adds a tooltip showing the bonus damage and the type of damage dded
     *
     * @param tool        Tool instance
     * @param modifier    Modifier and level
     * @param levelAmount Bonus per level
     * @param tooltip     Tooltip
     */
    static void addDamageBoost(IToolStackView tool, ModifierEntry modifier, float levelAmount, List<Text> tooltip) {
        addDamageBoost(tool, modifier.getModifier(), modifier.getEffectiveLevel() * levelAmount, tooltip);
    }
}
