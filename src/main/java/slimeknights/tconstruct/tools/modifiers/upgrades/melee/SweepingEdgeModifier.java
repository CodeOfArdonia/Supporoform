package slimeknights.tconstruct.tools.modifiers.upgrades.melee;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;

public class SweepingEdgeModifier extends Modifier implements TooltipModifierHook {
    private static final Text SWEEPING_BONUS = TConstruct.makeTranslation("modifier", "sweeping_edge.attack_damage");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    /**
     * Gets the damage dealt by this tool, boosted properly by sweeping
     */
    public float getSweepingDamage(IToolStackView toolStack, float baseDamage) {
        float level = toolStack.getModifier(this).getEffectiveLevel();
        float sweepingDamage = 1;
        if (level > 4) {
            sweepingDamage = baseDamage;
        } else if (level > 0) {
            // gives 25% per level, cap at base damage
            sweepingDamage = Math.min(baseDamage, level * 0.25f * baseDamage + 1);
        }
        return sweepingDamage;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        float amount = modifier.getEffectiveLevel() * 0.25f;
        tooltip.add(this.applyStyle(Text.literal(Util.PERCENT_FORMAT.format(amount)).append(" ").append(SWEEPING_BONUS)));
    }
}
