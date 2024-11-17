package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom explosion logic for EFLNs, more spherical and less random, plus works underwater
 */
public class EFLNExplosion extends Explosion {
    public EFLNExplosion(World world, @Nullable Entity entity, @Nullable DamageSource damage, @Nullable ExplosionBehavior context, double x, double y, double z, float size, boolean causesFire, DestructionType mode) {
        super(world, entity, damage, context, x, y, z, size, causesFire, mode);
    }

    @Override
    public void collectBlocksAndDamageEntities() {
        this.world.emitGameEvent(this.getEntity(), GameEvent.EXPLODE, new Vec3d(this.x, this.y, this.z));

        // we do a sphere of a certain radius, and check if the blockpos is inside the radius
        float radius = this.power * this.power;
        int range = (int) radius + 1;

        Set<BlockPos> set = new HashSet<>();
        for (int x = -range; x < range; ++x) {
            for (int y = -range; y < range; ++y) {
                for (int z = -range; z < range; ++z) {
                    int distance = x * x + y * y + z * z;
                    // inside the sphere?
                    if (distance <= radius) {
                        BlockPos blockpos = new BlockPos(x, y, z).add(BlockPos.ofFloored(this.x, this.y, this.z));
                        // no air blocks
                        if (this.world.isAir(blockpos)) {
                            continue;
                        }

                        // explosion "strength" at the current position
                        float f = this.power * (1f - distance / (radius));
                        BlockState blockstate = this.world.getBlockState(blockpos);

                        FluidState fluid = this.world.getFluidState(blockpos);
                        float f2 = Math.max(blockstate.getBlock().getBlastResistance(), fluid.getBlastResistance());
                        if (this.getEntity() != null) {
                            f2 = this.getEntity().getEffectiveExplosionResistance(this, this.world, blockpos, blockstate, fluid, f2);
                        }

                        f -= (f2 + 0.3F) * 0.3F;

                        if (f > 0.0F && (this.getEntity() == null || this.getEntity().canExplosionDestroyBlock(this, this.world, blockpos, blockstate, f))) {
                            set.add(blockpos);
                        }
                    }
                }
            }
        }
        this.getAffectedBlocks().addAll(set);

        // damage and blast back entities
        float diameter = this.power * 2;
        List<Entity> list = this.world.getOtherEntities(
                this.getEntity(),
                new Box(Math.floor(this.x - diameter - 1),
                        Math.floor(this.y - diameter - 1),
                        Math.floor(this.z - diameter - 1),
                        Math.floor(this.x + diameter + 1),
                        Math.floor(this.y + diameter + 1),
                        Math.floor(this.z + diameter + 1)),
                entity -> entity != null && !entity.isImmuneToExplosion() && !entity.isSpectator() && entity.isAlive());
        ForgeEventFactory.onExplosionDetonate(this.world, this, list, diameter);

        // start pushing entities
        Vec3d center = new Vec3d(this.x, this.y, this.z);
        for (Entity entity : list) {
            Vec3d dir = entity.getPos().subtract(center);
            double length = dir.length();
            double distance = length / diameter;
            if (distance <= 1) {
                // non-TNT uses eye height for explosion direction
                if (!(entity instanceof TntEntity)) {
                    dir = dir.add(0, entity.getEyeY() - entity.getY(), 0);
                    length = dir.length();
                }
                if (length > 1.0E-4D) {
                    double strength = (1.0D - distance) * getExposure(center, entity);
                    entity.damage(this.getDamageSource(), (int) ((strength * strength + strength) / 2 * diameter + 1));

                    // apply enchantment
                    // TODO 1.19.4, this was broke, reportably fixed in 1.19.4+
                    double reducedStrength = strength;
                    if (entity instanceof LivingEntity living) {
                        reducedStrength = ProtectionEnchantment.transformExplosionKnockback(living, strength);
                    }
                    entity.setVelocity(entity.getVelocity().add(dir.multiply(reducedStrength / length)));
                    if (entity instanceof PlayerEntity player) {
                        if (!player.isCreative() || !player.getAbilities().flying) {
                            // TODO 1.19.4: shouldn't this be reducedStrength? just copied vanilla here
                            this.getAffectedPlayers().put(player, dir.multiply(strength / length));
                        }
                    }
                }
            }
        }
    }
}
