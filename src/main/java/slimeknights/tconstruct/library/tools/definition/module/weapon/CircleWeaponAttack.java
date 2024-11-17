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
 * Deals damage in a circle around the primary target
 */
public record CircleWeaponAttack(float diameter) implements MeleeHitToolHook, ToolModule {
    public static final RecordLoadable<CircleWeaponAttack> LOADER = RecordLoadable.create(FloatLoadable.ANY.defaultField("diameter", 0f, true, CircleWeaponAttack::diameter), CircleWeaponAttack::new);
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<CircleWeaponAttack>defaultHooks(ToolHooks.MELEE_HIT);

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<CircleWeaponAttack> getLoader() {
        return LOADER;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ToolAttackContext context, float damage) {
        // only need fully charged for scythe sweep, easier than sword sweep
        if (context.isFullyCharged()) {
            // basically sword sweep logic, just deals full damage to all entities
            double range = this.diameter + tool.getModifierLevel(TinkerModifiers.expanded.getId());
            // allow having no range until modified with range
            if (range > 0) {
                double rangeSq = range * range;
                LivingEntity attacker = context.getAttacker();
                Entity target = context.getTarget();
                for (LivingEntity aoeTarget : attacker.getWorld().getNonSpectatingEntities(LivingEntity.class, target.getBoundingBox().expand(range, 0.25D, range))) {
                    if (aoeTarget != attacker && aoeTarget != target && !attacker.isTeammate(aoeTarget)
                            && !(aoeTarget instanceof ArmorStandEntity stand && stand.isMarker()) && target.squaredDistanceTo(aoeTarget) < rangeSq) {
                        float angle = attacker.getYaw() * ((float) Math.PI / 180F);
                        aoeTarget.takeKnockback(0.4F, MathHelper.sin(angle), -MathHelper.cos(angle));
                        // TODO: do we want to bring back the behavior where circle returns success if any AOE target is hit?
                        ToolAttackUtil.extraEntityAttack(tool, attacker, context.getHand(), aoeTarget);
                    }
                }

                attacker.getWorld().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, attacker.getSoundCategory(), 1.0F, 1.0F);
                if (attacker instanceof PlayerEntity player) {
                    player.spawnSweepAttackParticles();
                }
            }
        }
    }
}
