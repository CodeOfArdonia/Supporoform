package slimeknights.tconstruct.tools.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;

public class EntityMovementChangePacket implements IThreadsafePacket {
    private final int entityID;
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;

    public EntityMovementChangePacket(Entity entity) {
        this.entityID = entity.getId();
        this.x = entity.getVelocity().x;
        this.y = entity.getVelocity().y;
        this.z = entity.getVelocity().z;
        this.yRot = entity.getYaw();
        this.xRot = entity.getPitch();
    }

    public EntityMovementChangePacket(PacketByteBuf buffer) {
        this.entityID = buffer.readInt();
        this.x = buffer.readDouble();
        this.y = buffer.readDouble();
        this.z = buffer.readDouble();
        this.yRot = buffer.readFloat();
        this.xRot = buffer.readFloat();
    }

    @Override
    public void encode(PacketByteBuf packetBuffer) {
        packetBuffer.writeInt(this.entityID);
        packetBuffer.writeDouble(this.x);
        packetBuffer.writeDouble(this.y);
        packetBuffer.writeDouble(this.z);
        packetBuffer.writeFloat(this.yRot);
        packetBuffer.writeFloat(this.xRot);
    }

    @Override
    public void handleThreadsafe(Context context) {
        if (context.getSender() != null) {
            HandleClient.handle(this);
        }
    }

    /**
     * Safely runs client side only code in a method only called on client
     */
    private static class HandleClient {
        private static void handle(EntityMovementChangePacket packet) {
            assert MinecraftClient.getInstance().world != null;
            Entity entity = MinecraftClient.getInstance().world.getEntityById(packet.entityID);
            if (entity != null) {
                entity.setVelocity(packet.x, packet.y, packet.z);
                entity.setYaw(packet.yRot);
                entity.setPitch(packet.xRot);
            }
        }
    }
}
