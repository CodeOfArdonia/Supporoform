package slimeknights.mantle.command;

import com.google.common.collect.Maps;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagManagerLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.mantle.util.JsonHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Dumps all tags to a folder
 */
public class DumpAllTagsCommand {
    private static final String TAG_DUMP_PATH = "./mantle_data_dump";

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(sender -> sender.hasPermissionLevel(MantleCommand.PERMISSION_EDIT_SPAWN))
                .executes(DumpAllTagsCommand::runAll)
                .then(CommandManager.argument("type", RegistryArgument.registry()).suggests(MantleCommand.REGISTRY)
                        .executes(DumpAllTagsCommand::runType));
    }

    /**
     * Gets the path for the output
     */
    protected static File getOutputFile(CommandContext<ServerCommandSource> context) {
        return context.getSource().getServer().getFile(TAG_DUMP_PATH);
    }

    /**
     * Makes a clickable text component for the output folder
     *
     * @param file File
     * @return Clickable text component
     */
    protected static Text getOutputComponent(File file) {
        return Text.literal(file.getAbsolutePath()).styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent(Action.OPEN_FILE, file.getAbsolutePath())));
    }

    /**
     * Dumps all tags to the game directory
     */
    private static int runAll(CommandContext<ServerCommandSource> context) {
        File output = getOutputFile(context);
        int tagsDumped = context.getSource().getRegistryManager().streamAllRegistries().mapToInt(r -> runForFolder(context, r.key(), output)).sum();
        // print the output path
        context.getSource().sendFeedback(() -> Text.translatable("command.mantle.dump_all_tags.success", getOutputComponent(output)), true);
        return tagsDumped;
    }

    /**
     * Dumps a single type of tags to the game directory
     */
    private static int runType(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        File output = getOutputFile(context);
        Registry<?> registry = RegistryArgument.getResult(context, "type");
        int result = runForFolder(context, registry.getKey(), output);
        // print result
        context.getSource().sendFeedback(() -> Text.translatable("command.mantle.dump_all_tags.type_success", registry.getKey().getValue(), getOutputComponent(output)), true);
        return result;
    }

    /**
     * Runs the view-tag command
     *
     * @param context Tag context
     * @return Integer return
     */
    private static int runForFolder(CommandContext<ServerCommandSource> context, RegistryKey<? extends Registry<?>> key, File output) {
        Map<Identifier, List<TagGroupLoader.TrackedEntry>> foundTags = Maps.newHashMap();
        MinecraftServer server = context.getSource().getServer();
        ResourceManager manager = server.getResourceManager();
        Identifier tagType = key.getValue();

        // iterate all tags from the datapack
        String dataPackFolder = TagManagerLoader.getPath(key);
        for (Entry<Identifier, List<Resource>> entry : manager.findAllResources(dataPackFolder, fileName -> fileName.getPath().endsWith(".json")).entrySet()) {
            Identifier resourcePath = entry.getKey();
            Identifier tagId = JsonHelper.localize(resourcePath, dataPackFolder, ".json");
            DumpTagCommand.parseTag(entry.getValue(), foundTags.computeIfAbsent(resourcePath, id -> new ArrayList<>()), tagType, tagId, resourcePath);
        }

        // save all tags
        for (Entry<Identifier, List<TagGroupLoader.TrackedEntry>> entry : foundTags.entrySet()) {
            Identifier location = entry.getKey();
            Path path = output.toPath().resolve(location.getNamespace() + "/" + location.getPath());
            // TODO: is it worth including the sources anywhere in the dump?
            DumpTagCommand.saveTag(entry.getValue(), path);
        }

        return foundTags.size();
    }
}
