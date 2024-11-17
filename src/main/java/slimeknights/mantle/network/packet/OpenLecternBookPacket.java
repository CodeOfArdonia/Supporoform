package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.item.ILecternBookItem;

/**
 * Packet to open a book on a lectern
 */
@AllArgsConstructor
public class OpenLecternBookPacket implements IThreadsafePacket {
    private final BlockPos pos;
    private final ItemStack book;

    public OpenLecternBookPacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.book = buffer.readItemStack();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeItemStack(this.book);
    }

    @Override
    public void handleThreadsafe(Context context) {
        if (this.book.getItem() instanceof ILecternBookItem) {
            ((ILecternBookItem) this.book.getItem()).openLecternScreenClient(this.pos, this.book);
        }
    }
}
