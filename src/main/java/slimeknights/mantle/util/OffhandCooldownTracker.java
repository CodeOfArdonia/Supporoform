package slimeknights.mantle.util;

import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullFunction;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.network.packet.SwingArmPacket;

import javax.annotation.Nonnull;

/**
 * Logic to handle offhand having its own cooldown
 */
@RequiredArgsConstructor
public class OffhandCooldownTracker implements ICapabilityProvider {
    public static final Identifier KEY = Mantle.getResource("offhand_cooldown");
    public static final NonNullFunction<OffhandCooldownTracker, Float> COOLDOWN_TRACKER = OffhandCooldownTracker::getCooldown;
    private static final NonNullFunction<OffhandCooldownTracker, Boolean> ATTACK_READY = OffhandCooldownTracker::isAttackReady;

    /**
     * Capability instance for offhand cooldown
     */
    public static final Capability<OffhandCooldownTracker> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    /**
     * Registers the capability and subscribes to event listeners
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, OffhandCooldownTracker::attachCapability);
    }

    /**
     * Registers the capability with the event bus
     */
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(OffhandCooldownTracker.class);
    }

    /**
     * Called to add the capability handler to all players
     *
     * @param event Event
     */
    private static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof PlayerEntity player) {
            event.addCapability(KEY, new OffhandCooldownTracker(player));
        }
    }

    /**
     * Lazy optional of self for capability requirements
     */
    private final LazyOptional<OffhandCooldownTracker> capabilityInstance = LazyOptional.of(() -> this);
    /**
     * Player receiving cooldowns
     */
    @Nullable
    private final PlayerEntity player;
    /**
     * Scale of the last cooldown
     */
    private int lastCooldown = 0;
    /**
     * Time in ticks when the player can next attack for full power
     */
    private int attackReady = 0;

    /**
     * Enables the cooldown tracker if above 0. Intended to be set in equipment change events, not serialized
     */
    private int enabled = 0;

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? this.capabilityInstance.cast() : LazyOptional.empty();
    }

    /**
     * Null safe way to get the player's ticks existed
     */
    private int getTicksExisted() {
        if (this.player == null) {
            return 0;
        }
        return this.player.age;
    }

    /**
     * If true, the tracker is enabled despite a cooldown item not being held
     */
    public boolean isEnabled() {
        return this.enabled > 0;
    }

    /**
     * Call this method when your item causing offhand cooldown to be needed is enabled and disabled. If multiple placces call this, the tracker will automatically keep enabled until all places disable
     *
     * @param enable If true, enable. If false, disable
     */
    public void setEnabled(boolean enable) {
        if (enable) {
            this.enabled++;
        } else {
            this.enabled--;
        }
    }

    /**
     * Applies the given amount of cooldown
     *
     * @param cooldown Coolddown amount
     */
    public void applyCooldown(int cooldown) {
        this.lastCooldown = cooldown;
        this.attackReady = this.getTicksExisted() + cooldown;
    }

    /**
     * Returns a number from 0 to 1 denoting the current cooldown amount, akin to {@link PlayerEntity#getAttackCooldownProgress(float)}
     *
     * @return number from 0 to 1, with 1 being no cooldown
     */
    public float getCooldown() {
        int ticksExisted = this.getTicksExisted();
        if (ticksExisted > this.attackReady || this.lastCooldown == 0) {
            return 1.0f;
        }
        return MathHelper.clamp((this.lastCooldown + ticksExisted - this.attackReady) / (float) this.lastCooldown, 0f, 1f);
    }

    /**
     * Checks if we can perform another attack yet.
     * This counteracts rapid attacks via click macros, in a similar way to vanilla by limiting to once every 10 ticks
     */
    public boolean isAttackReady() {
        return this.getTicksExisted() + this.lastCooldown > this.attackReady;
    }


    /* Helpers */

    /**
     * Gets the offhand cooldown for the given player
     *
     * @param player Player
     * @return Offhand cooldown
     */
    public static float getCooldown(PlayerEntity player) {
        return player.getCapability(CAPABILITY).map(COOLDOWN_TRACKER).orElse(1.0f);
    }

    /**
     * Applies cooldown to the given player
     *
     * @param player   Player
     * @param cooldown Cooldown to apply
     */
    public static void applyCooldown(PlayerEntity player, int cooldown) {
        player.getCapability(CAPABILITY).ifPresent(cap -> cap.applyCooldown(cooldown));
    }

    /**
     * Applies cooldown to the given player
     *
     * @param player Player
     */
    public static boolean isAttackReady(PlayerEntity player) {
        return player.getCapability(CAPABILITY).map(ATTACK_READY).orElse(true);
    }

    /**
     * Applies cooldown using attack speed
     *
     * @param attackSpeed  Attack speed of the held item
     * @param cooldownTime Relative cooldown time for the given source, 20 is vanilla
     */
    public static void applyCooldown(PlayerEntity player, float attackSpeed, int cooldownTime) {
        applyCooldown(player, Math.round(cooldownTime / attackSpeed));
    }

    /**
     * Swings the entities hand without resetting cooldown
     */
    public static void swingHand(LivingEntity entity, Hand hand, boolean updateSelf) {
        if (!entity.handSwinging || entity.handSwingTicks >= entity.getHandSwingDuration() / 2 || entity.handSwingTicks < 0) {
            entity.handSwingTicks = -1;
            entity.handSwinging = true;
            entity.preferredHand = hand;
            if (!entity.method_48926().isClient) {
                SwingArmPacket packet = new SwingArmPacket(entity, hand);
                if (updateSelf) {
                    MantleNetwork.INSTANCE.sendToTrackingAndSelf(packet, entity);
                } else {
                    MantleNetwork.INSTANCE.sendToTracking(packet, entity);
                }
            }
        }
    }
}
