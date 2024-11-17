package slimeknights.tconstruct.library.tools;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.tools.TinkerTools;

/**
 * Item entity that will never die
 */
public class IndestructibleItemEntity extends ItemEntity {
    public IndestructibleItemEntity(EntityType<? extends IndestructibleItemEntity> entityType, World world) {
        super(entityType, world);
        // using setUnlimitedLifetime() makes the item no longer spin, dumb design
        // since age is a short, this value should never be reachable so the item will never despawn
        this.setNeverDespawn();
    }

    public IndestructibleItemEntity(World worldIn, double x, double y, double z, ItemStack stack) {
        this(TinkerTools.indestructibleItem.get(), worldIn);
        this.setPosition(x, y, z);
        this.setYaw(this.random.nextFloat() * 360.0F);
        this.setVelocity(this.random.nextDouble() * 0.2D - 0.1D, 0.2D, this.random.nextDouble() * 0.2D - 0.1D);
        this.setStack(stack);
    }

    /**
     * Copies the pickup delay from another entity
     */
    public void setPickupDelayFrom(Entity reference) {
        if (reference instanceof ItemEntity) {
            short pickupDelay = this.getPickupDelay((ItemEntity) reference);
            this.setPickupDelay(pickupDelay);
        }
        setVelocity(reference.getVelocity());
    }

    /**
     * workaround for private access on pickup delay. We simply read it from the items NBT representation ;)
     */
    private short getPickupDelay(ItemEntity reference) {
        NbtCompound tag = new NbtCompound();
        reference.writeCustomDataToNbt(tag);
        return tag.getShort("PickupDelay");
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // prevent any damage besides out of world
        return source.isOf(DamageTypes.OUT_OF_WORLD);
    }

    /**
     * Checks if the given stack has a custom entity
     */
    public static boolean hasCustomEntity(ItemStack stack) {
        return ModifierUtil.checkVolatileFlag(stack, IModifiable.INDESTRUCTIBLE_ENTITY);
    }

    /**
     * Creates an indestructible item entity from the given item stack (if needed). Intended to be called in {@link net.minecraftforge.common.extensions.IForgeItem#createEntity(Level, Entity, ItemStack)}
     *
     * @param world    World instance
     * @param original Original entity
     * @param stack    Stack to drop
     * @return indestructible entity, or null if the stack is not marked indestructible
     */
    @Nullable
    public static Entity createFrom(World world, Entity original, ItemStack stack) {
        if (ModifierUtil.checkVolatileFlag(stack, IModifiable.INDESTRUCTIBLE_ENTITY)) {
            IndestructibleItemEntity entity = new IndestructibleItemEntity(world, original.getX(), original.getY(), original.getZ(), stack);
            entity.setPickupDelayFrom(original);
            return entity;
        }
        return null;
    }
}
