package slimeknights.tconstruct.smeltery.network;

import lombok.AllArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

/**
 * Packet sent when a fluid is clicked in the smeltery UI
 */
@AllArgsConstructor
public class SmelteryFluidClickedPacket implements IThreadsafePacket {
    private final int index;

    public SmelteryFluidClickedPacket(PacketByteBuf buffer) {
        this.index = buffer.readVarInt();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeVarInt(this.index);
    }

    @Override
    public void handleThreadsafe(Context context) {
        ServerPlayerEntity sender = context.getSender();
        if (sender != null && !sender.isSpectator()) {
            ScreenHandler container = sender.currentScreenHandler;
            if (container instanceof BaseContainerMenu<?> base && base.getTile() instanceof ISmelteryTankHandler tank) {
                tank.getTank().moveFluidToBottom(this.index);
            }
        }
    }
}
