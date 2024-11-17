package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkEvent;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.client.BookCommand;

@AllArgsConstructor
public class OpenNamedBookPacket implements IThreadsafePacket {
    private final Identifier book;

    public OpenNamedBookPacket(PacketByteBuf buffer) {
        this.book = buffer.readIdentifier();
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(this.book);
    }

    @Override
    public void handleThreadsafe(NetworkEvent.Context context) {
        BookData bookData = BookLoader.getBook(this.book);
        if (bookData != null) {
            bookData.openGui(Text.literal("Book"), "", null, null);
        } else {
            ClientOnly.errorStatus(this.book);
        }
    }

    static class ClientOnly {
        static void errorStatus(Identifier book) {
            BookCommand.bookNotFound(book);
        }
    }
}
