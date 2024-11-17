package slimeknights.tconstruct.tables.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;

public class UpdateStationScreenPacket implements IThreadsafePacket {
    public static final UpdateStationScreenPacket INSTANCE = new UpdateStationScreenPacket();

    private UpdateStationScreenPacket() {
    }

    @Override
    public void encode(PacketByteBuf packetBuffer) {
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle();
    }

    /**
     * Safely runs client side only code in a method only called on client
     */
    private static class HandleClient {
        private static void handle() {
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen != null) {
                if (screen instanceof BaseTabbedScreen) {
                    ((BaseTabbedScreen<?, ?>) screen).updateDisplay();
                }
            }
        }
    }
}
