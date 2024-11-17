package slimeknights.mantle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Objects;

/**
 * Command that lists all values in a tag
 */
public class ViewTagCommand {
    /**
     * Tag has no values
     */
    private static final Text EMPTY = Text.translatable("command.mantle.tag.empty");
    /**
     * Tag type cannot be found
     */
    protected static final Dynamic2CommandExceptionType TAG_NOT_FOUND = new Dynamic2CommandExceptionType((type, name) -> Text.translatable("command.mantle.tag.not_found", type, name));

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(source -> MantleCommand.requiresDebugInfoOrOp(source, MantleCommand.PERMISSION_GAME_COMMANDS))
                .then(CommandManager.argument("type", RegistryArgument.registry()).suggests(MantleCommand.REGISTRY)
                        .then(CommandManager.argument("name", IdentifierArgumentType.identifier()).suggests(MantleCommand.VALID_TAGS)
                                .executes(ViewTagCommand::run)));
    }

    /**
     * Runs the view-tag command with the generic registry type, done to make generics happy
     *
     * @param context Tag context
     * @return Integer return
     * @throws CommandSyntaxException If invalid values are passed
     */
    private static <T> int runGeneric(CommandContext<ServerCommandSource> context, Registry<T> registry) throws CommandSyntaxException {
        Identifier name = context.getArgument("name", Identifier.class);
        RegistryEntryList.Named<T> holder = registry.getEntryList(TagKey.of(registry.getKey(), name)).orElse(null);
        if (holder != null) {
            // start building output message
            MutableText output = Text.translatable("command.mantle.view_tag.success", registry.getKey().getValue(), name);
            Collection<T> values = holder.stream().filter(RegistryEntry::hasKeyAndValue).map(RegistryEntry::value).toList();

            // if no values, print empty
            if (values.isEmpty()) {
                output.append("\n* ").append(EMPTY);
            } else {
                values.stream()
                        .map(registry::getId)
                        .sorted((a, b) -> Objects.requireNonNull(a).compareNamespaced(Objects.requireNonNull(b)))
                        .forEach(value -> output.append("\n* " + Objects.requireNonNull(value)));
            }
            context.getSource().sendFeedback(() -> output, true);
            return values.size();
        }
        throw TAG_NOT_FOUND.create(registry.getKey().getValue(), name);
    }

    /**
     * Runs the view-tag command
     *
     * @param context Tag context
     * @return Integer return
     * @throws CommandSyntaxException If invalid values are passed
     */
    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return runGeneric(context, RegistryArgument.getResult(context, "type"));
    }
}
