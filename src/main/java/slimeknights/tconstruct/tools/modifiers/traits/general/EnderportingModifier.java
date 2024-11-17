package slimeknights.tconstruct.tools.modifiers.traits.general;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.events.teleport.EnderportingTeleportEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.PlantHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;

import java.util.Set;

public class EnderportingModifier extends NoLevelsModifier implements PlantHarvestModifierHook, ProjectileHitModifierHook, ProjectileLaunchModifierHook, BlockHarvestModifierHook, MeleeHitModifierHook {
    private static final Identifier PRIMARY_ARROW = TConstruct.getResource("enderporting_primary");
    private static final Set<RelativeArgument> PACKET_FLAGS = ImmutableSet.of(RelativeArgument.X, RelativeArgument.Y, RelativeArgument.Z);

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PLANT_HARVEST, ModifierHooks.PROJECTILE_HIT, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.BLOCK_HARVEST, ModifierHooks.MELEE_HIT);
    }

    @Override
    public int getPriority() {
        return 45;
    }

    /**
     * Attempts to teleport to the given location
     */
    private static boolean tryTeleport(LivingEntity living, double x, double y, double z) {
        World world = living.getEntityWorld();
        // should never happen with the hooks, but just in case
        if (world.isClient) {
            return false;
        }
        // this logic is cloned from suffocation damage logic
        float scaledWidth = living.getWidth() * 0.8F;
        float eyeHeight = living.getStandingEyeHeight();
        Box aabb = Box.of(new Vec3d(x, y + (eyeHeight / 2), z), scaledWidth, eyeHeight, scaledWidth);

        boolean didCollide = world.getBlockCollisions(living, aabb).iterator().hasNext();

        // if we collided, try again 1 block down, means mining the top of 2 blocks is valid
        if (didCollide && living.getHeight() > 1) {
            // try again 1 block down
            aabb = aabb.offset(0, -1, 0);
            didCollide = world.getBlockCollisions(living, aabb).iterator().hasNext();
            y -= 1;
        }

        // as long as no collision now, we can teleport
        if (!didCollide) {
            // actual teleport
            EnderportingTeleportEvent event = new EnderportingTeleportEvent(living, x, y, z);
            MinecraftForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                // this logic only runs serverside, so need to use the server controller logic to move the player
                if (living instanceof ServerPlayerEntity playerMP) {
                    playerMP.networkHandler.requestTeleport(x, y, z, playerMP.getYaw(), playerMP.getPitch(), PACKET_FLAGS);
                } else {
                    living.setPosition(event.getTargetX(), event.getTargetY(), event.getTargetZ());
                }
                // particles must be sent on a server
                if (world instanceof ServerWorld serverWorld) {
                    for (int i = 0; i < 32; ++i) {
                        serverWorld.spawnParticles(ParticleTypes.PORTAL, living.getX(), living.getY() + world.random.nextDouble() * 2.0D, living.getZ(), 1, world.random.nextGaussian(), 0.0D, world.random.nextGaussian(), 0);
                    }
                }
                world.playSound(null, living.getX(), living.getY(), living.getZ(), Sounds.ENDERPORTING.getSound(), living.getSoundCategory(), 1f, 1f);
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!context.isExtraAttack()) {
            LivingEntity target = context.getLivingTarget();
            // if the entity is dead now
            if (target != null) {
                LivingEntity attacker = context.getAttacker();
                Vec3d oldPosition = attacker.getPos();
                if (tryTeleport(attacker, target.getX(), target.getY(), target.getZ())) {
                    tryTeleport(target, oldPosition.x, oldPosition.y, oldPosition.z);
                    ToolDamageUtil.damageAnimated(tool, 2, attacker, context.getSlotType());
                }
            }
        }
    }

    @Override
    public void finishHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, boolean didHarvest) {
        if (didHarvest && context.canHarvest()) {
            BlockPos pos = context.getPos();
            LivingEntity living = context.getLiving();
            if (tryTeleport(living, pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f)) {
                ToolDamageUtil.damageAnimated(tool, 2, living);
            }
        }
    }

    @Override
    public void afterHarvest(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, ServerWorld world, BlockState state, BlockPos pos) {
        // only teleport to the center block
        if (context.getBlockPos().equals(pos)) {
            LivingEntity living = context.getPlayer();
            if (living != null && tryTeleport(living, pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f)) {
                ToolDamageUtil.damageAnimated(tool, 2, living, context.getHand());
            }
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker != null && attacker != target && persistentData.getBoolean(PRIMARY_ARROW)) {
            Entity hitEntity = hit.getEntity();
            Vec3d oldPosition = attacker.getPos();
            if (attacker.world == projectile.world && tryTeleport(attacker, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ()) && target != null) {
                tryTeleport(target, oldPosition.x, oldPosition.y, oldPosition.z);
            }
        }
        return false;
    }

    @Override
    public boolean onProjectileHitBlock(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, BlockHitResult hit, @Nullable LivingEntity attacker) {
        if (attacker != null && persistentData.getBoolean(PRIMARY_ARROW)) {
            BlockPos target = hit.getBlockPos().offset(hit.getSide());
            if (attacker.world == projectile.world && tryTeleport(attacker, target.getX() + 0.5f, target.getY(), target.getZ() + 0.5f)) {
                projectile.discard();
            }
        }
        return false;
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary) {
        if (primary) {
            // damage on shoot as we won't have tool context once the arrow lands
            ToolDamageUtil.damageAnimated(tool, 10, shooter, shooter.getActiveHand());
            persistentData.putBoolean(PRIMARY_ARROW, true);
        }
    }
}
