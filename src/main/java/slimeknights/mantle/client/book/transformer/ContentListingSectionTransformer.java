package slimeknights.mantle.client.book.transformer;

import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.data.content.ContentListing;

/**
 * Transformer to create a simple list of elements
 */
public class ContentListingSectionTransformer extends SectionTransformer {
    private final Boolean largeTitle;
    private final Boolean centerTitle;

    public ContentListingSectionTransformer(String sectionName, @Nullable Boolean largeTitle, @Nullable Boolean centerTitle) {
        super(sectionName);
        this.largeTitle = largeTitle;
        this.centerTitle = centerTitle;
    }

    public ContentListingSectionTransformer(String sectionName) {
        this(sectionName, null, null);
    }

    @Override
    public void transform(BookData book, SectionData data) {
        ContentListing listing = new ContentListing();
        listing.setLargeTitle(this.largeTitle);
        listing.setCenterTitle(this.centerTitle);
        listing.title = book.translate(this.sectionName);
        String subtextKey = this.sectionName + ".subtext";
        if (book.strings.containsKey(subtextKey)) {
            listing.subText = book.translate(subtextKey);
        }

        PageData listingPage = new PageData(true);
        listingPage.name = this.sectionName;
        listingPage.source = data.source;
        listingPage.parent = data;
        listingPage.content = listing;

        data.pages.removeIf(sectionPage -> !this.processPage(book, listing, sectionPage));
        if (listing.hasEntries()) {
            listingPage.load();
            data.pages.add(0, listingPage);
        }
    }

    /**
     * Builds the listing
     *
     * @return true if the page should be removed
     */
    protected boolean processPage(BookData book, ContentListing listing, PageData page) {
        if (!IndexTransformer.isPageHidden(page) && !page.name.equals("hidden")) {
            listing.addEntry(page.getTitle(), page);
        }
        return true;
    }
}
