package slimeknights.tconstruct.tools.modifiers.traits.harvest;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockBreakModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.stats.ToolType;

import java.util.List;

public class MomentumModifier extends Modifier implements ProjectileLaunchModifierHook, ConditionalStatModifierHook, BlockBreakModifierHook, BreakSpeedModifierHook, TooltipModifierHook {
    private static final Text SPEED = TConstruct.makeTranslation("modifier", "momentum.speed");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.CONDITIONAL_STAT, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.BLOCK_BREAK, ModifierHooks.BREAK_SPEED, ModifierHooks.TOOLTIP);
    }

    @Override
    public int getPriority() {
        // run this last as we boost original speed, adds to existing boosts
        return 75;
    }

    /**
     * Gets the bonus for the modifier
     */
    private static float getBonus(LivingEntity living, ToolType type, ModifierEntry modifier) {
        return modifier.getEffectiveLevel() * (TinkerModifiers.momentumEffect.get(type).getLevel(living) + 1);
    }

    /**
     * Applies the effect to the target
     */
    private static void applyEffect(LivingEntity living, ToolType type, int duration, int maxLevel) {
        TinkerEffect effect = TinkerModifiers.momentumEffect.get(type);
        effect.apply(living, duration, Math.min(maxLevel, effect.getLevel(living) + 1), true);
    }

    @Override
    public void onBreakSpeed(IToolStackView tool, ModifierEntry modifier, BreakSpeed event, Direction sideHit, boolean isEffective, float miningSpeedModifier) {
        if (isEffective) {
            // 25% boost per level at max
            event.setNewSpeed(event.getNewSpeed() * (1 + getBonus(event.getEntity(), ToolType.HARVEST, modifier) / 128f));
        }
    }

    @Override
    public void afterBlockBreak(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        if (context.canHarvest() && context.isEffective() && !context.isAOE()) {
            // funny duration formula from 1.12, guess it makes faster tools have a slightly shorter effect
            int duration = (int) ((10f / tool.getStats().get(ToolStats.MINING_SPEED)) * 1.5f * 20f);
            // 32 blocks gets you to max, effect is stronger at higher levels
            applyEffect(context.getLiving(), ToolType.HARVEST, duration, 31);
        }
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary) {
        if (primary && (arrow == null || arrow.isCritical())) {
            // 16 arrows gets you to max
            applyEffect(shooter, ToolType.RANGED, 5 * 20, 15);
        }
    }

    @Override
    public float modifyStat(IToolStackView tool, ModifierEntry modifier, LivingEntity living, FloatToolStat stat, float baseValue, float multiplier) {
        if (stat == ToolStats.DRAW_SPEED) {
            return baseValue * (1 + getBonus(living, ToolType.RANGED, modifier) / 64f);
        }
        return baseValue;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey key, TooltipContext tooltipFlag) {
        ToolType type = ToolType.from(tool.getItem(), ToolType.NO_MELEE);
        if (type != null) {
            float bonus;
            if (player != null && key == TooltipKey.SHIFT) {
                bonus = getBonus(player, type, modifier) / (switch (type) {
                    case RANGED -> 64;
                    case ARMOR -> 4;
                    default -> 128;
                });
            } else {
                bonus = modifier.getEffectiveLevel();
                if (type != ToolType.ARMOR) {
                    bonus *= 0.25f;
                }
            }
            if (bonus > 0) {
                if (type == ToolType.ARMOR) {
                    ProtectionModule.addResistanceTooltip(tool, this, bonus * 2.5f, player, tooltip);
                } else {
                    TooltipModifierHook.addPercentBoost(this, SPEED, bonus, tooltip);
                }
            }
        }
    }
}
