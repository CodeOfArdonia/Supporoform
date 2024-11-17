package slimeknights.mantle.network.packet;

import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.client.book.BookHelper;

/**
 * Packet to update the page in a book in the players hand
 */
@RequiredArgsConstructor
public class UpdateHeldPagePacket implements IThreadsafePacket {
    private final Hand hand;
    private final String page;

    public UpdateHeldPagePacket(PacketByteBuf buffer) {
        this.hand = buffer.readEnumConstant(Hand.class);
        this.page = buffer.readString(100);
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeEnumConstant(this.hand);
        buf.writeString(this.page);
    }

    @Override
    public void handleThreadsafe(Context context) {
        PlayerEntity player = context.getSender();
        if (player != null && this.page != null) {
            ItemStack stack = player.getStackInHand(this.hand);
            if (!stack.isEmpty()) {
                BookHelper.writeSavedPageToBook(stack, this.page);
            }
        }
    }
}
