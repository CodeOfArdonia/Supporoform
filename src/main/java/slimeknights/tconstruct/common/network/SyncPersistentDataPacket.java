package slimeknights.tconstruct.common.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;

/**
 * Packet to sync player persistent data to the client
 */
@RequiredArgsConstructor
public class SyncPersistentDataPacket implements IThreadsafePacket {
    private final NbtCompound data;

    public SyncPersistentDataPacket(PacketByteBuf buffer) {
        this.data = buffer.readNbt();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeNbt(this.data);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    /**
     * Handles client side only code safely
     */
    private static class HandleClient {
        private static void handle(SyncPersistentDataPacket packet) {
            PlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.getCapability(PersistentDataCapability.CAPABILITY).ifPresent(data -> data.copyFrom(packet.data));
            }
        }
    }
}
