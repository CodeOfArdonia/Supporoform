package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import slimeknights.tconstruct.gadgets.TinkerGadgets;

import javax.annotation.Nonnull;

public class EFLNEntity extends ThrownItemEntity {
    public EFLNEntity(EntityType<? extends EFLNEntity> type, World level) {
        super(type, level);
    }

    public EFLNEntity(World level, LivingEntity thrower) {
        super(TinkerGadgets.eflnEntity.get(), thrower, level);
    }

    public EFLNEntity(World worldIn, double x, double y, double z) {
        super(TinkerGadgets.eflnEntity.get(), x, y, z, worldIn);
    }

    @Override
    protected Item getDefaultItem() {
        return TinkerGadgets.efln.get();
    }

    @Override
    protected void onCollision(HitResult result) {
        if (!this.getWorld().isClient) {
            // based on ServerLevel#explode
            EFLNExplosion explosion = new EFLNExplosion(this.getWorld(), this, null, null, this.getX(), this.getY(), this.getZ(), 4f, false, BlockInteraction.BREAK);
            if (!ForgeEventFactory.onExplosionStart(this.getWorld(), explosion)) {
                explosion.collectBlocksAndDamageEntities();
                explosion.affectWorld(false);
                if (this.getWorld() instanceof ServerWorld server) {
                    for (ServerPlayerEntity player : server.getPlayers()) {
                        if (player.squaredDistanceTo(this) < 4096.0D) {
                            player.networkHandler.sendPacket(new ExplosionS2CPacket(getX(), getY(), getZ(), 6, explosion.getAffectedBlocks(), explosion.getAffectedPlayers().get(player)));
                        }
                    }
                }
            }
            this.discard();
        }
    }

    @Override
    public void writeSpawnData(PacketByteBuf buffer) {
        buffer.writeItemStack(this.getItem());
    }

    @Override
    public void readSpawnData(PacketByteBuf additionalData) {
        this.setItem(additionalData.readItemStack());
    }
}
