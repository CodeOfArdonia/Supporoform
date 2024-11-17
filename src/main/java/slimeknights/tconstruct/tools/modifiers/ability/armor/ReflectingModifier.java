package slimeknights.tconstruct.tools.modifiers.ability.armor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.logic.InteractionHandler;

public class ReflectingModifier extends Modifier {
    public ReflectingModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::projectileImpact);
    }

    private void projectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        // first, need a projectile that is hitting a living entity
        if (!entity.getWorld().isClient) {
            ProjectileEntity projectile = event.getProjectile();

            // handle blacklist for projectiles
            // living entity must be using one of our shields
            HitResult hit = event.getRayTraceResult();
            if (!RegistryHelper.contains(TinkerTags.EntityTypes.REFLECTING_BLACKLIST, projectile.getType())
                    && hit.getType() == Type.ENTITY && ((EntityHitResult) hit).getEntity() instanceof LivingEntity living && living.isUsingItem() && living != projectile.getOwner()) {
                ItemStack stack = living.getActiveItem();
                if (stack.isIn(TinkerTags.Items.SHIELDS)) {
                    ToolStack tool = ToolStack.from(stack);
                    // make sure we actually have the modifier
                    int reflectingLevel = tool.getModifierLevel(this);
                    if (reflectingLevel > 0) {
                        ModifierEntry activeModifier = GeneralInteractionModifierHook.getActiveModifier(tool);
                        if (activeModifier != ModifierEntry.EMPTY) {
                            GeneralInteractionModifierHook hook = activeModifier.getHook(ModifierHooks.GENERAL_INTERACT);
                            int time = hook.getUseDuration(tool, activeModifier) - living.getItemUseTimeLeft();
                            // must be blocking, started blocking within the last 2*level seconds, and be within the block angle
                            if (hook.getUseAction(tool, activeModifier) == UseAction.BLOCK
                                    && (time >= 5 && time < 40 * reflectingLevel)
                                    && InteractionHandler.canBlock(living, projectile.getPos(), tool)) {

                                // time to actually reflect, this code is strongly based on code from the Parry mod
                                // take ownership of the projectile so it counts as a player kill, except in the case of fishing bobbers
                                if (!RegistryHelper.contains(TinkerTags.EntityTypes.REFLECTING_PRESERVE_OWNER, projectile.getType())) {
                                    // arrows are dumb and mutate their pickup status when owner is set, so disagree and set it back
                                    if (projectile instanceof PersistentProjectileEntity arrow) {
                                        PickupPermission pickup = arrow.pickupType;
                                        arrow.setOwner(living);
                                        arrow.pickupType = pickup;
                                    } else {
                                        projectile.setOwner(living);
                                    }
                                    projectile.leftOwner = true;
                                }

                                Vec3d reboundAngle = living.getRotationVector();
                                // use the shield accuracy and velocity stats when reflecting
                                float velocity = ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.VELOCITY) * 1.1f;
                                projectile.setVelocity(reboundAngle.x, reboundAngle.y, reboundAngle.z, velocity, ModifierUtil.getInaccuracy(tool, living));
                                if (projectile instanceof ExplosiveProjectileEntity hurting) {
                                    hurting.powerX = reboundAngle.x * 0.1;
                                    hurting.powerY = reboundAngle.y * 0.1;
                                    hurting.powerZ = reboundAngle.z * 0.1;
                                }
                                if (living.getType() == EntityType.PLAYER) {
                                    TinkerNetwork.getInstance().sendVanillaPacket(new EntityVelocityUpdateS2CPacket(projectile), living);
                                }
                                living.getWorld().playSound(null, living.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.5F + living.getWorld().random.nextFloat() * 0.4F);
                                event.setCanceled(true);
                            }
                        }
                    }
                }
            }
        }
    }
}
