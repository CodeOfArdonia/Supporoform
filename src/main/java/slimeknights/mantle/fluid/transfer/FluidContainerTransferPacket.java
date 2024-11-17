package slimeknights.mantle.fluid.transfer;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;

import java.util.Set;

/**
 * Packet to sync fluid container transfer
 */
@RequiredArgsConstructor
public class FluidContainerTransferPacket implements IThreadsafePacket {
    private final Set<Item> items;

    public FluidContainerTransferPacket(PacketByteBuf buffer) {
        ImmutableSet.Builder<Item> builder = ImmutableSet.builder();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            builder.add(buffer.readRegistryValue(Registries.ITEM));
        }
        this.items = builder.build();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeVarInt(this.items.size());
        for (Item item : this.items) {
            buffer.writeRegistryValue(Registries.ITEM, item);
        }
    }

    @Override
    public void handleThreadsafe(Context context) {
        FluidContainerTransferManager.INSTANCE.setContainerItems(this.items);
    }
}
