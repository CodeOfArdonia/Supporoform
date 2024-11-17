package slimeknights.tconstruct.tables.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;

/**
 * Packet to send to the server to update the name in the UI
 */
@RequiredArgsConstructor
public class TinkerStationRenamePacket implements IThreadsafePacket {
    private final String name;

    public TinkerStationRenamePacket(PacketByteBuf buf) {
        this.name = buf.readString(Short.MAX_VALUE);
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeString(this.name);
    }

    @Override
    public void handleThreadsafe(Context context) {
        ServerPlayerEntity sender = context.getSender();
        if (sender != null && sender.currentScreenHandler instanceof TinkerStationContainerMenu station) {
            TinkerStationBlockEntity tile = station.getTile();
            if (tile != null) {
                station.getTile().setItemName(this.name);
            }
        }
    }
}
