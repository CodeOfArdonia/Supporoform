package slimeknights.mantle.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.Mantle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command to dump global loot modifiers
 */
public class DumpLootModifiers {
    /**
     * Resource location of the global loot manager "tag"
     */
    protected static final Identifier GLOBAL_LOOT_MODIFIERS = new Identifier("forge", "loot_modifiers/global_loot_modifiers.json");
    /**
     * Path for saving the loot modifiers
     */
    private static final String LOOT_MODIFIER_PATH = GLOBAL_LOOT_MODIFIERS.getNamespace() + "/" + GLOBAL_LOOT_MODIFIERS.getPath();

    // loot modifiers
    private static final Text LOOT_MODIFIER_SUCCESS_LOG = Text.translatable("command.mantle.dump_loot_modifiers.success_log");
    protected static final SimpleCommandExceptionType ERROR_READING_LOOT_MODIFIERS = new SimpleCommandExceptionType(Text.translatable("command.mantle.dump_loot_modifiers.read_error", GLOBAL_LOOT_MODIFIERS));

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(sender -> sender.hasPermissionLevel(MantleCommand.PERMISSION_EDIT_SPAWN))
                .then(CommandManager.literal("save").executes(source -> run(source, true)))
                .then(CommandManager.literal("log").executes(source -> run(source, false)));
    }


    /**
     * Runs the command, dumping the tag
     */
    private static int run(CommandContext<ServerCommandSource> context, boolean saveFile) throws CommandSyntaxException {
        List<Identifier> finalLocations = new ArrayList<>();
        ResourceManager manager = context.getSource().getServer().getResourceManager();
        // logic based on forge logic for reading loot managers
        for (Resource resource : manager.getAllResources(GLOBAL_LOOT_MODIFIERS)) {
            try (Reader reader = resource.getReader()) {
                JsonObject json = JsonHelper.deserialize(DumpTagCommand.GSON, reader, JsonObject.class);
                if (json == null) {
                    // no json
                    Mantle.logger.error("Couldn't load global loot modifiers from {} in data pack {} as it is empty or null", GLOBAL_LOOT_MODIFIERS, resource.getResourcePackName());
                } else {
                    // replace: remove all lower
                    if (JsonHelper.getBoolean(json, "replace", false)) {
                        finalLocations.clear();
                    }
                    JsonArray entryList = JsonHelper.getArray(json, "entries");
                    for (JsonElement entry : entryList) {
                        Identifier res = Identifier.tryParse(JsonHelper.asString(entry, "entry"));
                        if (res != null) {
                            finalLocations.remove(res);
                            finalLocations.add(res);
                        }
                    }
                }
            } catch (RuntimeException | IOException ex) {
                Mantle.logger.error("Couldn't read global loot modifier list {} in data pack {}", GLOBAL_LOOT_MODIFIERS, resource.getResourcePackName(), ex);
            }
        }

        // save the list as JSON
        JsonArray entries = new JsonArray();
        for (Identifier location : finalLocations) {
            entries.add(location.toString());
        }
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        json.add("entries", entries);

        // if requested, save
        if (saveFile) {
            // save file
            File output = new File(DumpAllTagsCommand.getOutputFile(context), LOOT_MODIFIER_PATH);
            Path path = output.toPath();
            try {
                Files.createDirectories(path.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(DumpTagCommand.GSON.toJson(json));
                }
            } catch (IOException ex) {
                Mantle.logger.error("Couldn't save global loot manager to {}", path, ex);
            }
            context.getSource().sendFeedback(() -> Text.translatable("command.mantle.dump_loot_modifiers.success_save", DumpAllTagsCommand.getOutputComponent(output)), true);
        } else {
            // print to console
            context.getSource().sendFeedback(() -> LOOT_MODIFIER_SUCCESS_LOG, true);
            Mantle.logger.info("Dump of global loot modifiers:\n{}", DumpTagCommand.GSON.toJson(json));
        }
        // return a number to finish
        return finalLocations.size();
    }
}
