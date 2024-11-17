package slimeknights.mantle.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagFile;
import net.minecraft.registry.tag.TagGroupLoader.TrackedEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.tag.TagManagerLoader;
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
 * Command that dumps a tag into a JSON object
 */
public class DumpTagCommand {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Dynamic2CommandExceptionType ERROR_READING_TAG = new Dynamic2CommandExceptionType((type, name) -> Text.translatable("command.mantle.dump_tag.read_error", type, name));
    private static final Text SUCCESS_LOG = Text.translatable("command.mantle.dump_tag.success_log");

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(sender -> sender.hasPermissionLevel(MantleCommand.PERMISSION_EDIT_SPAWN))
                .then(CommandManager.argument("type", RegistryArgument.registry()).suggests(MantleCommand.REGISTRY)
                        .then(CommandManager.argument("name", IdentifierArgumentType.identifier()).suggests(MantleCommand.VALID_TAGS)
                                .executes(context -> run(context, Action.LOG))
                                .then(CommandManager.literal("log").executes(context -> run(context, Action.LOG)))
                                .then(CommandManager.literal("save").executes(context -> run(context, Action.SAVE)))
                                .then(CommandManager.literal("sources").executes(context -> run(context, Action.SOURCES)))));
    }

    /**
     * Runs the view-tag command
     *
     * @param context Tag context
     * @return Integer return
     * @throws CommandSyntaxException If invalid values are passed
     */
    private static int run(CommandContext<ServerCommandSource> context, Action action) throws CommandSyntaxException {
        return runGeneric(context, RegistryArgument.getResult(context, "type"), action);
    }

    /**
     * Parses a tag from the resource list
     */
    public static void parseTag(List<Resource> resources, List<TrackedEntry> list, Identifier regName, Identifier tagName, Identifier path) {
        for (Resource resource : resources) {
            String packId = resource.getResourcePackName();
            try (Reader reader = resource.getReader()) {
                JsonObject json = JsonHelper.deserialize(GSON, reader, JsonObject.class);
                TagFile tagfile = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, json)).getOrThrow(false, Mantle.logger::error);
                if (tagfile.replace()) {
                    list.clear();
                }
                tagfile.entries().forEach(tag -> list.add(new TrackedEntry(tag, packId)));
            } catch (RuntimeException | IOException ex) {
                // failed to parse
                Mantle.logger.error("Couldn't read {} tag list {} from {} in data pack {}", regName, tagName, path, packId, ex);
            }
        }
    }

    /**
     * Converts the given entry list to a string tag file
     */
    public static String tagToJson(List<TrackedEntry> entries) {
        return GSON.toJson(
                TagFile.CODEC.encodeStart(
                        JsonOps.INSTANCE,
                        new TagFile(entries.stream().map(TrackedEntry::entry).toList(), true)
                ).getOrThrow(false, Mantle.logger::error));
    }

    /**
     * Saves the tag to the given path
     */
    public static void saveTag(List<TrackedEntry> entries, Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(tagToJson(entries));
            }
        } catch (IOException ex) {
            Mantle.logger.error("Couldn't save tag to {}", path, ex);
        }
    }

    private enum Action {SAVE, LOG, SOURCES}

    /**
     * Runs the view-tag command, with the generic for the registry so those don't get mad
     *
     * @param context  Tag context
     * @param registry Registry
     * @return Integer return
     * @throws CommandSyntaxException If invalid values are passed
     */
    private static <T> int runGeneric(CommandContext<ServerCommandSource> context, Registry<T> registry, Action action) throws CommandSyntaxException {
        Identifier regName = registry.getKey().getValue();
        Identifier name = context.getArgument("name", Identifier.class);
        ResourceManager manager = context.getSource().getServer().getResourceManager();

        Identifier path = new Identifier(name.getNamespace(), TagManagerLoader.getPath(registry.getKey()) + "/" + name.getPath() + ".json");

        // if the tag file does not exist, only error if the tag is unknown
        List<Resource> resources = manager.getAllResources(path);
        // if the tag does not exist in the collection, probably an invalid tag name
        if (resources.isEmpty() && registry.getEntryList(TagKey.of(registry.getKey(), name)).isEmpty()) {
            throw ViewTagCommand.TAG_NOT_FOUND.create(regName, name);
        }

        // simply create a tag builder
        List<TrackedEntry> list = new ArrayList<>();
        parseTag(resources, list, regName, name, path);

        // builder done, ready to dump
        // if requested, save
        switch (action) {
            case SAVE -> {
                // save creates a file in the data dump location of the tag at the proper path
                File output = new File(DumpAllTagsCommand.getOutputFile(context), path.getNamespace() + "/" + path.getPath());
                saveTag(list, output.toPath());
                context.getSource().sendFeedback(() -> Text.translatable("command.mantle.dump_tag.success_log", regName, name, DumpAllTagsCommand.getOutputComponent(output)), true);
            }
            case LOG -> {
                // log writes the merged JSON to the console
                Text message = Text.translatable("command.mantle.dump_tag.success", regName, name);
                context.getSource().sendFeedback(() -> message, true);
                Mantle.logger.info("Tag dump of {} tag '{}':\n{}", regName, name, tagToJson(list));
            }
            case SOURCES -> {
                // sources prints a list of each entry and the source of the entry
                Text message = Text.translatable("command.mantle.dump_tag.success", regName, name);
                context.getSource().sendFeedback(() -> message, true);
                StringBuilder builder = new StringBuilder();
                builder.append("Tag list dump of ").append(regName).append(" tag ").append(name).append(" with sources:");
                for (TrackedEntry entry : list) {
                    builder.append("\n* '").append(entry.entry()).append("' from '").append(entry.source()).append('\'');
                }
                Mantle.logger.info(builder.toString());
            }
        }
        return resources.size();
    }
}
