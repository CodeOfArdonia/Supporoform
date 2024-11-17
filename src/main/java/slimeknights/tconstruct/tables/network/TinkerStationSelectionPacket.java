package slimeknights.tconstruct.tables.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;

@RequiredArgsConstructor
public class TinkerStationSelectionPacket implements IThreadsafePacket {
    private final Identifier layoutName;

    public TinkerStationSelectionPacket(PacketByteBuf buffer) {
        this.layoutName = buffer.readIdentifier();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeIdentifier(this.layoutName);
    }

    @Override
    public void handleThreadsafe(Context context) {
        ServerPlayerEntity sender = context.getSender();
        if (sender != null) {
            ScreenHandler container = sender.currentScreenHandler;
            if (container instanceof TinkerStationContainerMenu tinker) {
                tinker.setToolSelection(StationSlotLayoutLoader.getInstance().get(this.layoutName));
            }
        }
    }
}
