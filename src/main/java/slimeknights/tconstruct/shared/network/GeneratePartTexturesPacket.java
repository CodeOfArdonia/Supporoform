package slimeknights.tconstruct.shared.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.shared.client.ClientGeneratePartTexturesCommand;

/**
 * Packet to tell the client to generate tool textures
 */
@RequiredArgsConstructor
public class GeneratePartTexturesPacket implements IThreadsafePacket {
    private final Operation operation;
    private final String modId;
    private final String materialPath;

    public GeneratePartTexturesPacket(PacketByteBuf buffer) {
        this.operation = buffer.readEnumConstant(Operation.class);
        this.modId = buffer.readString(Short.MAX_VALUE);
        this.materialPath = buffer.readString(Short.MAX_VALUE);
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeEnumConstant(this.operation);
        buffer.writeString(this.modId);
        buffer.writeString(this.materialPath);
    }

    @Override
    public void handleThreadsafe(Context context) {
        context.enqueueWork(() -> ClientGeneratePartTexturesCommand.generateTextures(this.operation, this.modId, this.materialPath));
    }

    public enum Operation {ALL, MISSING}
}
