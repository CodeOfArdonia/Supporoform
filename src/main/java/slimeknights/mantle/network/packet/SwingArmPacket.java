package slimeknights.mantle.network.packet;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.util.OffhandCooldownTracker;

/**
 * Packet to tell a client to swing an entity arm, as the vanilla one resets cooldown
 */
public class SwingArmPacket implements IThreadsafePacket {
    private final int entityId;
    private final Hand hand;

    public SwingArmPacket(Entity entity, Hand hand) {
        this.entityId = entity.getId();
        this.hand = hand;
    }

    public SwingArmPacket(PacketByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.hand = buffer.readEnumConstant(Hand.class);
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeVarInt(this.entityId);
        buffer.writeEnumConstant(this.hand);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    private static class HandleClient {
        private static void handle(SwingArmPacket packet) {
            World world = MinecraftClient.getInstance().world;
            if (world != null) {
                Entity entity = world.getEntityById(packet.entityId);
                if (entity instanceof LivingEntity) {
                    OffhandCooldownTracker.swingHand((LivingEntity) entity, packet.hand, false);
                }
            }
        }
    }
}
