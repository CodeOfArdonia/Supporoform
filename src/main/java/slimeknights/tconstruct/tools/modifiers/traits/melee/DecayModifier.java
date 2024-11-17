package slimeknights.tconstruct.tools.modifiers.traits.melee;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;

public class DecayModifier extends Modifier implements ProjectileLaunchModifierHook, ProjectileHitModifierHook, MeleeHitModifierHook {
    /* gets the effect for the given level, including a random time */
    private static StatusEffectInstance makeDecayEffect(int level) {
        // potions are 0 indexed instead of 1 indexed
        // wither skeletons apply 10 seconds of wither for comparison
        return new StatusEffectInstance(StatusEffects.WITHER, 20 * (5 + (RANDOM.nextInt(level * 3))), level - 1);
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_HIT, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.isFullyCharged()) {
            // note the time of each effect is calculated independently

            // 25% chance to poison yourself
            if (RANDOM.nextInt(3) == 0) {
                context.getAttacker().addEffect(makeDecayEffect(modifier.getLevel()));
            }

            // always poison the target, means it works twice as often as lacerating
            LivingEntity target = context.getLivingTarget();
            if (target != null && target.isAlive()) {
                target.addStatusEffect(makeDecayEffect(modifier.getLevel()));
            }
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (target != null && (!(projectile instanceof PersistentProjectileEntity arrow) || arrow.isCritical())) {
            // always poison the target, means it works twice as often as lacerating
            target.addStatusEffect(makeDecayEffect(modifier.getLevel()));
        }
        return false;
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary) {
        if (primary && (arrow == null || arrow.isCritical()) && RANDOM.nextInt(3) == 0) {
            // 25% chance to poison yourself
            shooter.addStatusEffect(makeDecayEffect(modifier.getLevel()));
        }
    }
}
