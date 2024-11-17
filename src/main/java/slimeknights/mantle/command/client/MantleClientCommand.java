package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;

import java.util.function.Consumer;

/**
 * Root command for all commands in mantle
 */
public class MantleClientCommand {
    /**
     * Suggestion provider that lists registered book ids
     **/
    public static SuggestionProvider<ServerCommandSource> REGISTERED_BOOKS;

    /**
     * Registers all Mantle client command related content
     */
    public static void init() {
        // register arguments
        REGISTERED_BOOKS = SuggestionProviders.register(Mantle.getResource("registered_books"), (context, builder) ->
                CommandSource.suggestIdentifiers(BookLoader.getRegisteredBooks(), builder));

        // add command listener
        MinecraftForge.EVENT_BUS.addListener(MantleClientCommand::registerCommand);
    }

    /**
     * Registers a sub command for the root Mantle client command
     */
    private static void register(LiteralArgumentBuilder<ServerCommandSource> root, String name, Consumer<LiteralArgumentBuilder<ServerCommandSource>> consumer) {
        LiteralArgumentBuilder<ServerCommandSource> subCommand = CommandManager.literal(name);
        consumer.accept(subCommand);
        root.then(subCommand);
    }

    /**
     * Event listener to register the Mantle client command
     */
    private static void registerCommand(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("mantle");

        // sub commands
        register(builder, "book", BookCommand::register);
        register(builder, "clear_book_cache", ClearBookCacheCommand::register);

        // register final command
        event.getDispatcher().register(builder);
    }
}
