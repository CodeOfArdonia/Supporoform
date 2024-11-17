package slimeknights.mantle.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fabricators_of_create.porting_lib.util.TierSortingRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.mantle.Mantle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Command to dump global loot modifiers
 */
public class HarvestTiersCommand {
    /**
     * Resource location of the global loot manager "tag"
     */
    protected static final Identifier HARVEST_TIERS = new Identifier("forge", "item_tier_ordering.json");
    /**
     * Path for saving the loot modifiers
     */
    private static final String HARVEST_TIER_PATH = HARVEST_TIERS.getNamespace() + "/" + HARVEST_TIERS.getPath();

    // loot modifiers
    private static final Text SUCCESS_LOG = Text.translatable("command.mantle.harvest_tiers.success_log");
    private static final Text EMPTY = Text.translatable("command.mantle.tag.empty");

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(sender -> sender.hasPermissionLevel(MantleCommand.PERMISSION_EDIT_SPAWN))
                .then(CommandManager.literal("save").executes(source -> run(source, true)))
                .then(CommandManager.literal("log").executes(source -> run(source, false)))
                .then(CommandManager.literal("list").executes(HarvestTiersCommand::list));
    }

    /**
     * Creates a clickable component for a block tag
     */
    private static Object getTagComponent(TagKey<Block> tag) {
        Identifier id = tag.id();
        return Text.literal(id.toString()).styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/mantle dump_tag " + RegistryKeys.BLOCK.getValue() + " " + id + " save")));
    }

    /**
     * Runs the command, dumping the tag
     */
    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        List<ToolMaterial> sortedTiers = TierSortingRegistry.getSortedTiers();

        // start building output message
        MutableText output = Text.translatable("command.mantle.harvest_tiers.success_list");
        // if no values, print empty
        if (sortedTiers.isEmpty()) {
            output.append("\n* ").append(EMPTY);
        } else {
            for (ToolMaterial tier : sortedTiers) {
                output.append("\n* ");
                TagKey<Block> tag = tier.getTag();
                Identifier id = TierSortingRegistry.getName(tier);
                if (tag != null) {
                    output.append(Text.translatable("command.mantle.harvest_tiers.tag", id, getTagComponent(tag)));
                } else {
                    output.append(Text.translatable("command.mantle.harvest_tiers.no_tag", id));
                }
            }
        }
        context.getSource().sendFeedback(() -> output, true);
        return sortedTiers.size();
    }

    /**
     * Runs the command, dumping the tag
     */
    private static int run(CommandContext<ServerCommandSource> context, boolean saveFile) throws CommandSyntaxException {
        List<ToolMaterial> sortedTiers = TierSortingRegistry.getSortedTiers();

        // save the list as JSON
        JsonArray entries = new JsonArray();
        for (ToolMaterial location : sortedTiers) {
            entries.add(Objects.requireNonNull(TierSortingRegistry.getName(location)).toString());
        }
        JsonObject json = new JsonObject();
        json.add("order", entries);

        // if requested, save
        if (saveFile) {
            // save file
            File output = new File(DumpAllTagsCommand.getOutputFile(context), HARVEST_TIER_PATH);
            Path path = output.toPath();
            try {
                Files.createDirectories(path.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(DumpTagCommand.GSON.toJson(json));
                }
            } catch (IOException ex) {
                Mantle.logger.error("Couldn't save harvests tiers to {}", path, ex);
            }
            context.getSource().sendFeedback(() -> Text.translatable("command.mantle.harvest_tiers.success_save", DumpAllTagsCommand.getOutputComponent(output)), true);
        } else {
            // print to console
            context.getSource().sendFeedback(() -> SUCCESS_LOG, true);
            Mantle.logger.info("Dump of harvests tiers:\n{}", DumpTagCommand.GSON.toJson(json));
        }
        // return a number to finish
        return sortedTiers.size();
    }
}
