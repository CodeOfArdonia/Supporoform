package slimeknights.tconstruct.library.tools.definition.module.weapon;

import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

/**
 * Attack logic for a sweep attack, similar to a sword
 */
public record SweepWeaponAttack(float range) implements MeleeHitToolHook, ToolModule {
    public static final RecordLoadable<SweepWeaponAttack> LOADER = RecordLoadable.create(FloatLoadable.FROM_ZERO.defaultField("range", 0f, true, SweepWeaponAttack::range), SweepWeaponAttack::new);
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<SweepWeaponAttack>defaultHooks(ToolHooks.MELEE_HIT);

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<SweepWeaponAttack> getLoader() {
        return LOADER;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ToolAttackContext context, float damage) {
        // sweep code from Player#attack(Entity)
        // basically: no crit, no sprinting and has to stand on the ground for sweep. Also has to move regularly slowly
        LivingEntity attacker = context.getAttacker();
        if (context.isFullyCharged() && !attacker.isSprinting() && !context.isCritical() && attacker.isOnGround() && (attacker.horizontalSpeed - attacker.prevHorizontalSpeed) < attacker.getMovementSpeed()) {
            // loop through all nearby entities
            double range = this.range + tool.getModifierLevel(TinkerModifiers.expanded.getId());
            double rangeSq = (2 + range); // TODO: why do we add 2 here? should that not be defined in the datagen?
            rangeSq *= rangeSq;
            // if the modifier is missing, sweeping damage will be 0, so easiest to let it fully control this
            float sweepDamage = TinkerModifiers.sweeping.get().getSweepingDamage(tool, damage);
            Entity target = context.getTarget();
            for (LivingEntity aoeTarget : attacker.getWorld().getNonSpectatingEntities(LivingEntity.class, target.getBoundingBox().expand(range, 0.25D, range))) {
                if (aoeTarget != attacker && aoeTarget != target && !attacker.isTeammate(aoeTarget)
                        && !(aoeTarget instanceof ArmorStandEntity armorStand && armorStand.isMarker()) && attacker.squaredDistanceTo(aoeTarget) < rangeSq) {
                    float angle = attacker.getYaw() * ((float) Math.PI / 180F);
                    aoeTarget.takeKnockback(0.4F, MathHelper.sin(angle), -MathHelper.cos(angle));
                    ToolAttackUtil.dealDefaultDamage(attacker, aoeTarget, sweepDamage);
                }
            }

            attacker.getWorld().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, attacker.getSoundCategory(), 1.0F, 1.0F);
            if (attacker instanceof PlayerEntity player) {
                player.spawnSweepAttackParticles();
            }
        }
    }
}
