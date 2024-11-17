package slimeknights.tconstruct.tools.modifiers.ability.sling;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;

/**
 * Add velocity to the target away from yourself
 */
public class BonkingModifier extends SlingModifier implements MeleeHitModifierHook, MeleeDamageModifierHook {
    private static final float RANGE = 5F;
    /**
     * If true, bonking is in progress, suppresses knockback and boosts damage
     */
    private static boolean isBonking = false;

    @Override
    protected void registerHooks(Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            // melee tools use attack speed for bonk, since this is also an attack
            float speed;
            if (tool.hasTag(TinkerTags.Items.MELEE_WEAPON)) {
                speed = tool.getStats().get(ToolStats.ATTACK_SPEED);
            } else {
                speed = ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.DRAW_SPEED);
            }
            tool.getPersistentData().putInt(GeneralInteractionModifierHook.KEY_DRAWTIME, (int) Math.ceil(30f / speed));
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        if (isBonking) {
            knockback = 0;
        }
        return knockback;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (isBonking) {
            damage *= 1.5f;
        }
        return damage;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        super.onStoppedUsing(tool, modifier, entity, timeLeft);
        if (!entity.getWorld().isClient && (entity instanceof PlayerEntity player)) {
            float f = this.getForce(tool, modifier, player, timeLeft, true);
            if (f > 0) {
                Vec3d start = player.getCameraPosVec(1F);
                Vec3d look = player.getRotationVector();
                Vec3d direction = start.add(look.x * RANGE, look.y * RANGE, look.z * RANGE);
                Box bb = player.getBoundingBox().stretch(look.x * RANGE, look.y * RANGE, look.z * RANGE).stretch(1, 1, 1);

                EntityHitResult hit = ProjectileUtil.getEntityCollision(player.getWorld(), player, start, direction, bb, (e) -> e instanceof LivingEntity);
                if (hit != null) {
                    LivingEntity target = (LivingEntity) hit.getEntity();
                    double targetDist = start.squaredDistanceTo(target.getCameraPosVec(1F));

                    // cancel if there's a block in the way
                    BlockHitResult mop = ModifiableItem.blockRayTrace(player.getWorld(), player, RaycastContext.FluidHandling.NONE);
                    if (mop.getType() != HitResult.Type.BLOCK || targetDist < mop.getBlockPos().getSquaredDistance(start)) {
                        // melee tools also do damage as a treat
                        if (tool.hasTag(TinkerTags.Items.MELEE)) {
                            isBonking = true;
                            Hand hand = player.getActiveHand();
                            ToolAttackUtil.attackEntity(tool, entity, hand, target, () -> Math.min(1, f), true);
                            isBonking = false;
                        }

                        // send it flying
                        float inaccuracy = ModifierUtil.getInaccuracy(tool, player) * 0.0075f;
                        Random random = player.getRandom();
                        target.takeKnockback(f * 3, -look.x + random.nextGaussian() * inaccuracy, -look.z + random.nextGaussian() * inaccuracy);

                        // spawn the bonk particle
                        ToolAttackUtil.spawnAttackParticle(TinkerTools.bonkAttackParticle.get(), player, 0.6d);
                        if (player instanceof ServerPlayerEntity playerMP) {
                            TinkerNetwork.getInstance().sendVanillaPacket(new EntityVelocityUpdateS2CPacket(player), playerMP);
                        }

                        // cooldowns and stuff
                        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.BONK.getSound(), player.getSoundCategory(), 1, 1);
                        player.addExhaustion(0.2F);
                        player.getItemCooldownManager().set(tool.getItem(), 3);
                        ToolDamageUtil.damageAnimated(tool, 1, entity);
                        return;
                    }
                }
            }
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.BONK.getSound(), player.getSoundCategory(), 1, 0.5f);
        }
    }
}
