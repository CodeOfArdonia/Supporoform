package slimeknights.tconstruct.tools.modifiers.traits.melee;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;

public class NecroticModifier extends Modifier implements ProjectileHitModifierHook, MeleeHitModifierHook, TooltipModifierHook {
    private static final Text LIFE_STEAL = TConstruct.makeTranslation("modifier", "necrotic.lifesteal");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT, ModifierHooks.MELEE_HIT, ModifierHooks.TOOLTIP);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.isFullyCharged() && context.isCritical() && damageDealt > 0) {
            // heals a percentage of damage dealt, using same rate as reinforced
            int level = modifier.getLevel();
            float percent = 0.05f * level;
            if (percent > 0) {
                LivingEntity attacker = context.getAttacker();
                attacker.heal(percent * damageDealt);
                attacker.world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), Sounds.NECROTIC_HEAL.getSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                // take a bit of extra damage to heal
                ToolDamageUtil.damageAnimated(tool, level, attacker, context.getSlotType());
            }
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (target != null && attacker != null) {
            float percent = 0.05f * modifier.getLevel();
            if (percent > 0) {
                if (projectile instanceof PersistentProjectileEntity arrow && arrow.isCritical()) {
                    // we don't actually know how much damage will be dealt, so just guess by using the standard formula
                    // to prevent healing too much, limit by the target's health. Will let you life steal ignoring armor, but eh, only so much we can do efficiently
                    attacker.heal((float) (percent * Math.min(target.getHealth(), arrow.getDamage() * arrow.getVelocity().length())));
                    attacker.world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), Sounds.NECROTIC_HEAL.getSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }
        }
        return false;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        float lifesteal = 0.05f * modifier.getLevel();
        if (lifesteal > 0) {
            tooltip.add(this.applyStyle(Text.literal(Util.PERCENT_FORMAT.format(lifesteal) + " ").append(LIFE_STEAL)));
        }
    }
}
