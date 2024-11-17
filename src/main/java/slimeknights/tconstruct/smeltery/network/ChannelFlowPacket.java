package slimeknights.tconstruct.smeltery.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/**
 * Packet for when the flowing state changes on a channel side
 */
public class ChannelFlowPacket implements IThreadsafePacket {
    private final BlockPos pos;
    private final Direction side;
    private final boolean flow;

    public ChannelFlowPacket(BlockPos pos, Direction side, boolean flow) {
        this.pos = pos;
        this.side = side;
        this.flow = flow;
    }

    public ChannelFlowPacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.side = buffer.readEnumConstant(Direction.class);
        this.flow = buffer.readBoolean();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeEnumConstant(this.side);
        buffer.writeBoolean(this.flow);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    private static class HandleClient {
        private static void handle(ChannelFlowPacket packet) {
            BlockEntityHelper.get(ChannelBlockEntity.class, MinecraftClient.getInstance().world, packet.pos).ifPresent(te -> te.setFlow(packet.side, packet.flow));
        }
    }
}
