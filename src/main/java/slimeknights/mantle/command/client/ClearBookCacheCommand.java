package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;

/**
 * Command that clears the cache of a book or all books, faster than resource pack reloading for book writing
 */
public class ClearBookCacheCommand {
    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(source -> source.getEntity() instanceof AbstractClientPlayerEntity)
                .then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(MantleClientCommand.REGISTERED_BOOKS)
                        .executes(ClearBookCacheCommand::runBook))
                .executes(ClearBookCacheCommand::runAll);
    }

    /**
     * Runs the book-test command for specific book
     *
     * @param context Command context
     * @return Integer return
     */
    private static int runBook(CommandContext<ServerCommandSource> context) {
        Identifier book = IdentifierArgumentType.getIdentifier(context, "id");
        clearBookCache(book);
        return 0;
    }

    /**
     * Runs the book-test command
     *
     * @param context Command context
     * @return Integer return
     */
    private static int runAll(CommandContext<ServerCommandSource> context) {
        clearBookCache(null);
        return 0;
    }

    private static void clearBookCache(@Nullable Identifier book) {
        if (book != null) {
            BookData bookData = BookLoader.getBook(book);
            if (bookData != null) {
                bookData.reset();
            } else {
                BookCommand.bookNotFound(book);
            }
        } else {
            BookLoader.resetAllBooks();
        }
    }
}
