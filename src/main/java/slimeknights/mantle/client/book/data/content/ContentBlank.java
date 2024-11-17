package slimeknights.mantle.client.book.data.content;

import net.minecraft.util.Identifier;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.element.BookElement;

import java.util.ArrayList;

public class ContentBlank extends PageContent {
    public static final Identifier ID = Mantle.getResource("blank");

    @Override
    public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    }
}
