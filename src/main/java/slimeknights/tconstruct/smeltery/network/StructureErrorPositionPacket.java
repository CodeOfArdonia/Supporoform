package slimeknights.tconstruct.smeltery.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * Packet to tell a multiblock to render a specific position as the cause of the error
 */
@RequiredArgsConstructor
public class StructureErrorPositionPacket implements IThreadsafePacket {
    private final BlockPos controllerPos;
    @Nullable
    private final BlockPos errorPos;

    public StructureErrorPositionPacket(PacketByteBuf buffer) {
        this.controllerPos = buffer.readBlockPos();
        if (buffer.readBoolean()) {
            this.errorPos = buffer.readBlockPos();
        } else {
            this.errorPos = null;
        }
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.controllerPos);
        if (this.errorPos != null) {
            buffer.writeBoolean(true);
            buffer.writeBlockPos(this.errorPos);
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    private static class HandleClient {
        private static void handle(StructureErrorPositionPacket packet) {
            BlockEntityHelper.get(HeatingStructureBlockEntity.class, MinecraftClient.getInstance().world, packet.controllerPos)
                    .ifPresent(te -> te.setErrorPos(packet.errorPos));
        }
    }
}
