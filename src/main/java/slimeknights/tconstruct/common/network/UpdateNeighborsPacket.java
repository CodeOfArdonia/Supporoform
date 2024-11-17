package slimeknights.tconstruct.common.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.GameData;
import slimeknights.mantle.network.packet.IThreadsafePacket;

/**
 * Packet to notify neighbors that a block changed, used when breaking blocks in weird contexts that vanilla suppresses updates in for some reason
 */
@RequiredArgsConstructor
public class UpdateNeighborsPacket implements IThreadsafePacket {
    private final BlockState state;
    private final BlockPos pos;

    public UpdateNeighborsPacket(PacketByteBuf buffer) {
        this.state = GameData.getBlockStateIDMap().byId(buffer.readVarInt());
        this.pos = buffer.readBlockPos();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeVarInt(Block.getRawIdFromState(this.state));
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    private static class HandleClient {
        private static void handle(UpdateNeighborsPacket packet) {
            World level = MinecraftClient.getInstance().world;
            if (level != null) {
                packet.state.updateNeighbors(level, packet.pos, Block.NOTIFY_LISTENERS, 511);
                packet.state.prepare(level, packet.pos, Block.NOTIFY_LISTENERS, 511);
            }
        }
    }
}
