package slimeknights.tconstruct.library;

import net.minecraft.util.Identifier;
import slimeknights.mantle.command.BookTestCommand;
import slimeknights.tconstruct.TConstruct;

/**
 * This class can safely be accessed serverside for book IDs
 */
public class TinkerBookIDs {
    public static final Identifier MATERIALS_BOOK_ID = TConstruct.getResource("materials_and_you");
    public static final Identifier MIGHTY_SMELTING_ID = TConstruct.getResource("mighty_smelting");
    public static final Identifier PUNY_SMELTING_ID = TConstruct.getResource("puny_smelting");
    public static final Identifier TINKERS_GADGETRY_ID = TConstruct.getResource("tinkers_gadgetry");
    public static final Identifier FANTASTIC_FOUNDRY_ID = TConstruct.getResource("fantastic_foundry");
    public static final Identifier ENCYCLOPEDIA_ID = TConstruct.getResource("encyclopedia");

    /**
     * Regsiters suggestions with the mantle command
     */
    public static void registerCommandSuggestion() {
        BookTestCommand.addBookSuggestion(MATERIALS_BOOK_ID);
        BookTestCommand.addBookSuggestion(MIGHTY_SMELTING_ID);
        BookTestCommand.addBookSuggestion(PUNY_SMELTING_ID);
        BookTestCommand.addBookSuggestion(TINKERS_GADGETRY_ID);
        BookTestCommand.addBookSuggestion(FANTASTIC_FOUNDRY_ID);
        BookTestCommand.addBookSuggestion(ENCYCLOPEDIA_ID);
    }
}
