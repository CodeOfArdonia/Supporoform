package slimeknights.mantle.client.book.transformer;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.data.content.ContentListing;
import slimeknights.mantle.client.book.data.content.ContentPadding.ContentRightPadding;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.TextDataRenderer;

import java.util.List;

/**
 * Extended version of {@link ContentListingSectionTransformer} which supports putting entries in subgroups
 */
public class ContentGroupingSectionTransformer extends SectionTransformer {
    private static final Identifier INDEX_EXTRA_DATA = Mantle.getResource("index");

    private final Boolean largeTitle;
    private final Boolean centerTitle;

    public ContentGroupingSectionTransformer(String sectionName, @Nullable Boolean largeTitle, @Nullable Boolean centerTitle) {
        super(sectionName);
        this.largeTitle = largeTitle;
        this.centerTitle = centerTitle;
    }

    public ContentGroupingSectionTransformer(String sectionName) {
        this(sectionName, null, null);
    }

    @Override
    public void transform(BookData book, SectionData data) {
        String title = book.translate(this.sectionName);
        String subtextKey = this.sectionName + ".subtext";
        String subText = null;
        if (book.strings.containsKey(subtextKey)) {
            subText = book.translate(subtextKey);
        }

        // start building the listing
        GroupingBuilder builder = new GroupingBuilder(data, title, subText, this.largeTitle, this.centerTitle);
        data.pages.removeIf(sectionPage -> !this.processPage(book, builder, sectionPage));

        // create pages for each listing if any exist
        if (builder.hasEntries()) {
            // add padding page to keep indexes stretching over two pages together
            int i = 0;
            List<ContentListing> finishedListings = builder.getFinishedListings();
            if (finishedListings.size() % 2 == 0) {
                PageData padding = new PageData(true);
                padding.source = data.source;
                padding.parent = data;
                padding.content = new ContentRightPadding();
                padding.load();
                // hack: add padding to the previous section so section links start at the index
                int sectionIndex = data.parent.sections.indexOf(data);
                if (sectionIndex > 0) {
                    data.parent.sections.get(sectionIndex - 1).pages.add(padding);
                } else {
                    data.pages.add(padding);
                    i++;
                }
            }

            // add a page for each finished listing
            for (ContentListing listing : finishedListings) {
                PageData listingPage = new PageData(true);
                listingPage.name = this.sectionName;
                listingPage.source = data.source;
                listingPage.parent = data;
                listingPage.content = listing;
                listingPage.load();
                data.pages.add(i, listingPage);
                i++;
            }
        }
    }

    /**
     * Overridable method to process a single page
     */
    protected boolean processPage(BookData book, GroupingBuilder builder, PageData page) {
        if (!IndexTransformer.isPageHidden(page) && !page.name.equals("hidden")) {
            builder.addPage(page.getTitle(), page);
        }
        return true;
    }

    /**
     * Builder to create a all content listing pages
     */
    public static class GroupingBuilder {
        private static final int COLUMN_WIDTH = BookScreen.PAGE_WIDTH / 3;

        /**
         * Section containing this grouping
         */
        private final SectionData section;
        /**
         * Number of columns on the page, makes a new page if its 3
         */
        private int columns = 1;
        /**
         * Number of entries in the current column, when too big a new column starts
         */
        private int entriesInColumn = 0;
        /**
         * Max number of entries in a column
         */
        private int maxInColumn;
        /**
         * Listing that is currently being built
         */
        private ContentListing currentListing = new ContentListing();
        /**
         * All listings to include in the book
         */
        @Getter
        private final List<ContentListing> finishedListings = Lists.newArrayList(this.currentListing);

        public GroupingBuilder(SectionData section, @Nullable String title, @Nullable String subText, @Nullable Boolean largeTitle, @Nullable Boolean centerTitle) {
            this.section = section;
            this.currentListing.title = title;
            this.currentListing.subText = subText;
            this.currentListing.setLargeTitle(largeTitle);
            this.currentListing.setCenterTitle(centerTitle);
            this.maxInColumn = this.currentListing.getEntriesInColumn(section);
        }

        /**
         * If true, entries were added
         */
        public boolean hasEntries() {
            return this.entriesInColumn > 0; // should be sufficient, only 0 when nothing is added
        }

        /**
         * Increases the number of entries in the column
         */
        private void incrementColumns(String text, boolean bold) {
            // entriesInColumn += section.parent.fontRenderer.wordWrapHeight(text, COLUMN_WIDTH) / 9;
            if (bold) {
                this.entriesInColumn += TextDataRenderer.getLinesForString(text, Formatting.BOLD.toString(), COLUMN_WIDTH, "", this.section.parent.fontRenderer);
            } else {
                this.entriesInColumn += TextDataRenderer.getLinesForString(text, "", COLUMN_WIDTH, "- ", this.section.parent.fontRenderer);
            }
        }

        /**
         * Starts a new column
         */
        private void startNewColumn(boolean forceBreak) {
            // already have 3 columns? start a new one
            if (this.columns == 3) {
                this.currentListing = new ContentListing();
                ContentListing firstListing = this.finishedListings.get(0);
                this.currentListing.title = firstListing.title;
                this.currentListing.setLargeTitle(firstListing.getLargeTitle());
                this.currentListing.setCenterTitle(firstListing.getCenterTitle());
                this.maxInColumn = this.currentListing.getEntriesInColumn(this.section);
                this.finishedListings.add(this.currentListing);
                this.columns = 1;
            } else {
                // 1 or 2 columns? force break
                this.columns++;
                if (forceBreak) {
                    this.currentListing.addColumnBreak();
                }
            }
            this.entriesInColumn = 0;
        }

        /**
         * Adds a group to this listing
         */
        public void addGroup(String name, @Nullable PageData data) {
            // if a group already exists, start a new column
            if (this.entriesInColumn != 0) {
                this.startNewColumn(true);
            }

            // add the title entry to the column
            this.incrementColumns(name, true);
            this.currentListing.addEntry(name, data, true);
        }

        /**
         * Adds a page to the current group in the listing
         */
        public void addPage(String name, PageData data) {
            if (this.entriesInColumn == this.maxInColumn) {
                this.startNewColumn(false);
            }
            this.incrementColumns(name, false);
            this.currentListing.addEntry(name, data, false);
        }
    }
}
