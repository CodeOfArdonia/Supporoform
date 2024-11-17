package slimeknights.tconstruct.world.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import slimeknights.tconstruct.world.TinkerWorld;

/**
 * Clay based slime cube
 */
public class TerracubeEntity extends ArmoredSlimeEntity {
    public TerracubeEntity(EntityType<? extends TerracubeEntity> type, World worldIn) {
        super(type, worldIn);
    }

    /**
     * Checks if a slime can spawn at the given location
     */
    public static boolean canSpawnHere(EntityType<? extends SlimeEntity> entityType, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (reason == SpawnReason.SPAWNER) {
            return true;
        }
        BlockPos down = pos.down();
        if (world.getFluidState(pos).isIn(FluidTags.WATER) && world.getFluidState(down).isIn(FluidTags.WATER)) {
            return true;
        }
        return world.getBlockState(down).allowsSpawning(world, down, entityType) && HostileEntity.isSpawnDark(world, pos, random);
    }

    @Override
    protected float getJumpVelocity() {
        return 0.5f * this.getJumpVelocityMultiplier();
    }

    @Override
    protected float getDamageAmount() {
        return (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) + 2;
    }

    @Override
    protected ParticleEffect getParticles() {
        return TinkerWorld.terracubeParticle.get();
    }

    @Override
    protected int computeFallDamage(float distance, float damageMultiplier) {
        return 0;
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty difficulty) {
        // earth slime spawns with vanilla armor, but unlike zombies turtle shells are fair game
        // vanilla logic but simplified down to just helmets
        float multiplier = difficulty.getClampedLocalDifficulty();
        if (this.random.nextFloat() < 0.15F * multiplier) {
            int armorQuality = this.random.nextInt(3);
            if (this.random.nextFloat() < 0.25F) {
                ++armorQuality;
            }
            if (this.random.nextFloat() < 0.25F) {
                ++armorQuality;
            }
            if (this.random.nextFloat() < 0.25F) {
                ++armorQuality;
            }

            ItemStack current = this.getEquippedStack(EquipmentSlot.HEAD);
            if (current.isEmpty()) {
                Item item = armorQuality == 5 ? Items.TURTLE_HELMET : getEquipmentForSlot(EquipmentSlot.HEAD, armorQuality);
                if (item != null) {
                    this.equipStack(EquipmentSlot.HEAD, new ItemStack(item));
                    this.enchantEquipment(random, multiplier, EquipmentSlot.HEAD);
                }
            }
        }
    }
}
