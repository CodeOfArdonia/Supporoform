package slimeknights.tconstruct.library.client.book.content;

import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.content.ContentShowcase;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.element.BookElement;
import slimeknights.mantle.client.screen.book.element.ItemElement;
import slimeknights.mantle.client.screen.book.element.TextElement;
import slimeknights.tconstruct.TConstruct;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TooltipShowcaseContent extends ContentShowcase {
    public static final transient Identifier ID = TConstruct.getResource("showcase");

    public List<Text> tooltip = null;

    @Override
    public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
        int y = getTitleHeight();

        if (this.title == null || this.title.isEmpty()) {
            y = 0;
        } else {
            this.addTitle(list, this.title);
        }

        if (this.item != null && !this.item.getItems().isEmpty()) {
            ItemElement element = new ItemElement(BookScreen.PAGE_WIDTH / 2 - 15, y, 2.5f, this.item.getItems(), this.item.action);
            element.tooltip = this.tooltip;
            list.add(element);
            y += element.height;
        }

        if (this.text != null && this.text.length > 0) {
            list.add(new TextElement(0, y, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - y, this.text));
        }
    }
}
