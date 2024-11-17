package slimeknights.mantle.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet interface to add common methods for registration
 */
public interface ISimplePacket {
    /**
     * Encodes a packet for the buffer
     *
     * @param buf Buffer instance
     */
    void encode(PacketByteBuf buf);

    /**
     * Handles receiving the packet
     *
     * @param context Packet context
     */
    void handle(Supplier<NetworkEvent.Context> context);
}
