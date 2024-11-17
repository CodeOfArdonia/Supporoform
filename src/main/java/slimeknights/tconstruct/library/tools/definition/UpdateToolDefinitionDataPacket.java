package slimeknights.tconstruct.library.tools.definition;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Packet to sync tool definitions to the client
 */
@RequiredArgsConstructor
public class UpdateToolDefinitionDataPacket implements IThreadsafePacket {
    @Getter(AccessLevel.PROTECTED)
    private final Map<Identifier, ToolDefinitionData> dataMap;

    public UpdateToolDefinitionDataPacket(PacketByteBuf buffer) {
        int size = buffer.readVarInt();
        ImmutableMap.Builder<Identifier, ToolDefinitionData> builder = ImmutableMap.builder();
        for (int i = 0; i < size; i++) {
            Identifier name = buffer.readIdentifier();
            ToolDefinitionData data = ToolDefinitionData.read(buffer);
            builder.put(name, data);
        }
        this.dataMap = builder.build();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeVarInt(this.dataMap.size());
        for (Entry<Identifier, ToolDefinitionData> entry : this.dataMap.entrySet()) {
            buffer.writeIdentifier(entry.getKey());
            entry.getValue().write(buffer);
        }
    }

    @Override
    public void handleThreadsafe(Context context) {
        ToolDefinitionLoader.getInstance().updateDataFromServer(this.dataMap);
    }
}
