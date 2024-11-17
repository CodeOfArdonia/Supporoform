package slimeknights.tconstruct.library.utils;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import slimeknights.tconstruct.common.Sounds;

import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

/**
 * Logic for entities bouncing
 */
public class SlimeBounceHandler {
    private SlimeBounceHandler() {
    }

    private static final IdentityHashMap<Entity, BounceInfo> BOUNCING_ENTITIES = new IdentityHashMap<>();

    /**
     * Registers event handlers
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(SlimeBounceHandler::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(SlimeBounceHandler::serverStopping);
    }

    /**
     * Preserves entity air momentum
     *
     * @param entity Entity to bounce
     */
    public static void addBounceHandler(LivingEntity entity) {
        addBounceHandler(entity, null);
    }

    /**
     * Causes the entity to bounce, needed because the fall event will reset motion afterwards
     *
     * @param entity Entity to bounce
     * @param bounce Bounce amount
     */
    public static void addBounceHandler(LivingEntity entity, @Nullable Vec3d bounce) {
        // no fake players PlayerTick event
        if (entity instanceof FakePlayer) {
            return;
        }
        // update bounce info
        BounceInfo info = BOUNCING_ENTITIES.get(entity);
        if (info == null) {
            BOUNCING_ENTITIES.put(entity, new BounceInfo(entity, bounce));
        } else if (bounce != null) {
            // updated bounce if needed
            info.bounce = bounce;
            // add one to the tick as there is a 1 tick delay between falling and ticking for many entities
            info.bounceTick = entity.age + 1;
            Vec3d motion = entity.getVelocity();
            info.lastMagSq = motion.x * motion.x + motion.z * motion.z;
            info.lastAngle = MathHelper.atan2(motion.z, motion.x);
        }
    }

    /**
     * Called on living tick to preserve momentum and bounce
     */
    private static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        BounceInfo info = BOUNCING_ENTITIES.get(entity);

        // if we have info for this entity, time to work
        if (info != null) {
            // if flying, nothing to do
            if (entity.isFallFlying() || entity.isSpectator()) {
                BOUNCING_ENTITIES.remove(entity);
                return;
            }

            // if its the bounce tick, time to bounce. This is to circumvent the logic that resets y motion after landing
            if (entity.age == info.bounceTick) {
                if (info.bounce != null) {
                    entity.setVelocity(info.bounce);
                    info.bounce = null;
                }
                info.bounceTick = 0;
            }

            boolean isInAir = !entity.isOnGround() && !entity.isTouchingWater() && !entity.isClimbing();

            // preserve motion
            if (isInAir && info.lastMagSq > 0) {
                // figure out how much motion has reduced
                Vec3d motion = entity.getVelocity();
                double motionSq = motion.x * motion.x + motion.z * motion.z;
                // if not moving, cancel velocity preserving in 5 ticks
                if (motionSq == 0) {
                    if (info.stopMagTick == 0) {
                        info.stopMagTick = entity.age + 5;
                    } else if (entity.age > info.stopMagTick) {
                        info.lastMagSq = 0;
                    }
                } else if (motionSq < info.lastMagSq) {
                    info.stopMagTick = 0;
                    // preserve 95% of former speed
                    double boost = Math.sqrt(info.lastMagSq / motionSq) * 0.975f;
                    if (boost > 1) {
                        entity.setVelocity(motion.x * boost, motion.y, motion.z * boost);
                        entity.velocityDirty = true;
                        info.lastMagSq = info.lastMagSq * 0.975f * 0.975f;
                        // play sound if we had a big angle change
                        double newAngle = MathHelper.atan2(motion.z, motion.x);
                        if (Math.abs(newAngle - info.lastAngle) > 1) {
                            entity.playSound(Sounds.SLIMY_BOUNCE.getSound(), 1.0f, 1.0f);
                        }
                        info.lastAngle = newAngle;
                    } else {
                        info.lastMagSq = motionSq;
                        info.lastAngle = MathHelper.atan2(motion.z, motion.x);
                    }
                }
            }

            // timing the effect out
            if (info.wasInAir && !isInAir) {
                if (info.endHandler == 0) {
                    info.endHandler = entity.age + 5;
                } else if (entity.age > info.endHandler) {
                    BOUNCING_ENTITIES.remove(entity);
                }
            } else {
                info.endHandler = 0;
                info.wasInAir = true;
            }
        }
    }

    /**
     * Called on server shutdown to prevent memory leaks
     */
    private static void serverStopping(ServerStoppingEvent event) {
        BOUNCING_ENTITIES.clear();
    }

    /**
     * Data class to keep track of bouncing info for an entity
     */
    private static class BounceInfo {
        /**
         * Velocity the entity should have, unused if null
         */
        @Nullable
        private Vec3d bounce;
        /**
         * Time to update the entities velocity
         */
        private int bounceTick;
        /**
         * Tick to stop entity magnitude changes
         */
        private int stopMagTick;
        /**
         * Magnitude of the X/Z motion last tick
         */
        private double lastMagSq;
        /**
         * If true, the entity was in air last tick
         */
        private boolean wasInAir = false;
        /**
         * Time when motion should stop
         */
        private int endHandler = 0;
        /**
         * Last angle of motion, used for sound effects
         */
        private double lastAngle;

        public BounceInfo(LivingEntity entity, @Nullable Vec3d bounce) {
            this.bounce = bounce;
            if (bounce != null) {
                // add one to the tick as there is a 1 tick delay between falling and ticking for many entities
                this.bounceTick = entity.age + 1;
            } else {
                this.bounceTick = 0;
            }
            Vec3d motion = entity.getVelocity();
            this.lastMagSq = motion.x * motion.x + motion.z * motion.z;
        }
    }
}
