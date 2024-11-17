package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.client.book.BookHelper;
import slimeknights.mantle.util.BlockEntityHelper;

/**
 * Packet to update the book page in a lectern
 */
@AllArgsConstructor
public class UpdateLecternPagePacket implements IThreadsafePacket {
    private final BlockPos pos;
    private final String page;

    public UpdateLecternPagePacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.page = buffer.readString(100);
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeString(this.page);
    }

    @Override
    public void handleThreadsafe(Context context) {
        PlayerEntity player = context.getSender();
        if (player != null && this.page != null) {
            World world = player.getEntityWorld();
            BlockEntityHelper.get(LecternBlockEntity.class, world, this.pos).ifPresent(te -> {
                ItemStack stack = te.getBook();
                if (!stack.isEmpty()) {
                    BookHelper.writeSavedPageToBook(stack, this.page);
                }
            });
        }
    }
}
