package slimeknights.mantle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.helper.TagPreference;

/**
 * Command to test tag preference behavior
 */
public class TagPreferenceCommand {
    private static final String EMPTY_TAG = "command.mantle.tag_preference.empty_tag";
    private static final String PREFERENCE = "command.mantle.tag_preference.preference";

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(sender -> sender.hasPermissionLevel(MantleCommand.PERMISSION_EDIT_SPAWN))
                .then(CommandManager.argument("type", RegistryArgument.registry()).suggests(MantleCommand.REGISTRY)
                        .then(CommandManager.argument("name", IdentifierArgumentType.identifier()).suggests(MantleCommand.VALID_TAGS)
                                .executes(TagPreferenceCommand::run)));
    }

    /**
     * Runs the command
     *
     * @param context Tag context
     * @return Integer return
     * @throws CommandSyntaxException If invalid values are passed
     */
    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return runGeneric(context, RegistryArgument.getResult(context, "type"));
    }

    /**
     * Runs the command, fixing issues with generics
     *
     * @param context Tag context
     * @return Integer return
     */
    private static <T> int runGeneric(CommandContext<ServerCommandSource> context, Registry<T> registry) {
        Identifier name = context.getArgument("name", Identifier.class);
        TagKey<T> tag = TagKey.of(registry.getKey(), name);
        T preference = TagPreference.getPreference(tag).orElse(null);
        if (preference == null) {
            context.getSource().sendFeedback(() -> Text.translatable(EMPTY_TAG, registry.getKey().getValue(), name), true);
            return 0;
        } else {
            context.getSource().sendFeedback(() -> Text.translatable(PREFERENCE, registry.getKey().getValue(), name, registry.getId(preference)), true);
            return 1;
        }
    }
}
