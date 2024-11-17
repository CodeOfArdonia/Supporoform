package slimeknights.tconstruct.world.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.events.teleport.EnderSlimeTeleportEvent;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TeleportHelper;
import slimeknights.tconstruct.library.utils.TeleportHelper.ITeleportEventFactory;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.world.TinkerWorld;

public class EnderSlimeEntity extends ArmoredSlimeEntity {
    /**
     * Predicate for this ender slime to allow teleporting
     */
    private final ITeleportEventFactory teleportPredicate = (entity, x, y, z) -> new EnderSlimeTeleportEvent(entity, x, y, z, this);

    public EnderSlimeEntity(EntityType<? extends EnderSlimeEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected ParticleEffect getParticles() {
        return TinkerWorld.enderSlimeParticle.get();
    }

    @Override
    public void applyDamageEffects(LivingEntity slime, Entity target) {
        super.applyDamageEffects(slime, target);
        if (target instanceof LivingEntity) {
            TeleportHelper.randomNearbyTeleport((LivingEntity) target, this.teleportPredicate);
        }
    }

    @Override
    protected void applyDamage(DamageSource damageSrc, float damageAmount) {
        float oldHealth = this.getHealth();
        super.applyDamage(damageSrc, damageAmount);
        if (this.isAlive() && this.getHealth() < oldHealth) {
            TeleportHelper.randomNearbyTeleport(this, this.teleportPredicate);
        }
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty difficulty) {
        // ender slime spawns with slimeskulls with a random material
        // vanilla logic but simplified down to just helmets
        float multiplier = difficulty.getClampedLocalDifficulty();
        if (this.random.nextFloat() < 0.15F * difficulty.getClampedLocalDifficulty()) {
            // 2.5% chance of plate
            ItemStack helmet = new ItemStack(TinkerTools.slimesuit.get(ArmorSlotType.HELMET));
            // just init stats, will set random material
            ToolStack.from(helmet).ensureHasData();
            // finally, give the slime the helmet
            this.equipStack(EquipmentSlot.HEAD, helmet);
        }
    }
}
