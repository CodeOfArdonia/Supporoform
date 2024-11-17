package slimeknights.tconstruct.tools.modifiers.traits.melee;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;

public class EnderferenceModifier extends Modifier implements ProjectileHitModifierHook, MeleeHitModifierHook, OnAttackedModifierHook {
    private static final DamageSource FALLBACK = new DamageSource("arrow");

    public EnderferenceModifier() {
        MinecraftForge.EVENT_BUS.addListener(EnderferenceModifier::onTeleport);
    }

    private static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasStatusEffect(TinkerModifiers.enderferenceEffect.get())) {
            event.setCanceled(true);
        }
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT, ModifierHooks.MELEE_HIT, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public int getPriority() {
        return 50; // run later so other hooks can run before we cancel it all
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        LivingEntity entity = context.getLivingTarget();
        if (entity != null) {
            // hack: do not want them teleporting from this hit
            TinkerModifiers.enderferenceEffect.get().apply(entity, 1, 0, true);
        }
        return knockback;
    }

    @Override
    public void failedMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageAttempted) {
        LivingEntity entity = context.getLivingTarget();
        if (entity != null) {
            entity.removeStatusEffect(TinkerModifiers.enderferenceEffect.get());
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity entity = context.getLivingTarget();
        if (entity != null) {
            // 5 seconds of interference per level, affect all entities as players may teleport too
            entity.addStatusEffect(new StatusEffectInstance(TinkerModifiers.enderferenceEffect.get(), modifier.getLevel() * 100, 0, false, true, true));
        }
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        // this works like vanilla, damage is capped due to the hurt immunity mechanics, so if multiple pieces apply thorns between us and vanilla, damage is capped at 4
        if (isDirectDamage && source.getAttacker() instanceof LivingEntity attacker) {
            // 15% chance of working per level, doubled bonus on shields
            int level = modifier.getLevel();
            if (slotType.getType() == Type.HAND) {
                level *= 2;
            }
            if (RANDOM.nextFloat() < (level * 0.25f)) {
                attacker.addStatusEffect(new StatusEffectInstance(TinkerModifiers.enderferenceEffect.get(), modifier.getLevel() * 100, 0, false, true, true));
            }
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(TinkerModifiers.enderferenceEffect.get(), modifier.getLevel() * 100, 0, false, true, true));

            // endermen are hardcoded to not take arrow damage, so disagree by reimplementing arrow damage right here
            if (target.getType() == EntityType.ENDERMAN && projectile instanceof PersistentProjectileEntity arrow) {
                // first, give up if we reached pierce capacity, and ensure list are created
                if (arrow.getPierceLevel() > 0) {
                    if (arrow.piercedEntities == null) {
                        arrow.piercedEntities = new IntOpenHashSet(5);
                    }
                    if (arrow.piercingKilledEntities == null) {
                        arrow.piercingKilledEntities = Lists.newArrayListWithCapacity(5);
                    }
                    if (arrow.piercedEntities.size() >= arrow.getPierceLevel() + 1) {
                        arrow.discard();
                        return true;
                    }
                    arrow.piercedEntities.add(target.getId());
                }

                // calculate damage, bonus on crit
                int damage = MathHelper.ceil(MathHelper.clamp(arrow.getVelocity().length() * arrow.getDamage(), 0.0D, Integer.MAX_VALUE));
                if (arrow.isCritical()) {
                    damage = (int) Math.min(RANDOM.nextInt(damage / 2 + 2) + (long) damage, Integer.MAX_VALUE);
                }

                // create damage source, don't use projectile sources as that makes endermen ignore it
                Entity owner = arrow.getOwner();
                DamageSource damageSource;
                if (attacker instanceof PlayerEntity player) {
                    damageSource = DamageSource.playerAttack(player);
                } else if (attacker != null) {
                    damageSource = DamageSource.mobAttack(attacker);
                } else {
                    damageSource = FALLBACK;
                }
                if (attacker != null) {
                    attacker.onAttacking(target);
                }

                // handle fire
                int remainingFire = target.getFireTicks();
                if (arrow.isOnFire()) {
                    target.setOnFireFor(5);
                }

                if (target.damage(damageSource, (float) damage)) {
                    if (!arrow.getWorld().isClient && arrow.getPierceLevel() <= 0) {
                        target.setStuckArrowCount(target.getStuckArrowCount() + 1);
                    }

                    // knockback from punch
                    int knockback = arrow.getPunch();
                    if (knockback > 0) {
                        Vec3d knockbackVec = arrow.getVelocity().multiply(1.0D, 0.0D, 1.0D).normalize().multiply(knockback * 0.6D);
                        if (knockbackVec.lengthSquared() > 0.0D) {
                            target.addVelocity(knockbackVec.x, 0.1D, knockbackVec.z);
                        }
                    }

                    if (!arrow.getWorld().isClient && attacker != null) {
                        EnchantmentHelper.onUserDamaged(target, attacker);
                        EnchantmentHelper.onTargetDamaged(attacker, target);
                    }

                    arrow.onHit(target);

                    if (!target.isAlive() && arrow.piercingKilledEntities != null) {
                        arrow.piercingKilledEntities.add(target);
                    }

                    if (!arrow.getWorld().isClient && arrow.isShotFromCrossbow() && owner instanceof ServerPlayerEntity player) {
                        if (arrow.piercingKilledEntities != null) {
                            Criteria.KILLED_BY_CROSSBOW.trigger(player, arrow.piercingKilledEntities);
                        } else if (!target.isAlive()) {
                            Criteria.KILLED_BY_CROSSBOW.trigger(player, List.of(target));
                        }
                    }

                    arrow.playSound(arrow.sound, 1.0F, 1.2F / (RANDOM.nextFloat() * 0.2F + 0.9F));
                    if (arrow.getPierceLevel() <= 0) {
                        arrow.discard();
                    }
                } else {
                    // reset fire and drop the arrow
                    target.setFireTicks(remainingFire);
                    arrow.setVelocity(arrow.getVelocity().multiply(-0.1D));
                    arrow.setYaw(arrow.getYaw() + 180.0F);
                    arrow.prevYaw += 180.0F;
                    if (!arrow.getWorld().isClient && arrow.getVelocity().lengthSquared() < 1.0E-7D) {
                        if (arrow.pickupType == PersistentProjectileEntity.PickupPermission.ALLOWED) {
                            arrow.dropStack(arrow.asItemStack(), 0.1F);
                        }

                        arrow.discard();
                    }
                }

                return true;
            }
        }
        return false;
    }
}
