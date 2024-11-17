package slimeknights.tconstruct.shared.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fabricators_of_create.porting_lib.command.ModIdArgument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.shared.command.argument.MaterialArgument;
import slimeknights.tconstruct.shared.network.GeneratePartTexturesPacket;
import slimeknights.tconstruct.shared.network.GeneratePartTexturesPacket.Operation;

/**
 * Command to generate tool textures using the palette logic
 */
public class GeneratePartTexturesCommand {
    private static final Text SUCCESS = TConstruct.makeTranslation("command", "generate_part_textures.start");

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(source -> source.getEntity() instanceof ServerPlayerEntity)
                // generate_part_textures all|missing [<mod_id>|<material>]
                .then(CommandManager.literal("all")
                        .executes(context -> run(context, Operation.ALL, "", ""))
                        .then(CommandManager.argument("mod_id", ModIdArgument.modIdArgument()).executes(context -> runModId(context, Operation.ALL)))
                        .then(CommandManager.argument("material", MaterialArgument.material()).executes(context -> runMaterial(context, Operation.ALL))))
                .then(CommandManager.literal("missing")
                        .executes(context -> run(context, Operation.MISSING, "", ""))
                        .then(CommandManager.argument("mod_id", ModIdArgument.modIdArgument()).executes(context -> runModId(context, Operation.MISSING)))
                        .then(CommandManager.argument("material", MaterialArgument.material()).executes(context -> runMaterial(context, Operation.MISSING))));
    }

    /**
     * Runs the command, filtered by a material
     */
    private static int runMaterial(CommandContext<ServerCommandSource> context, Operation filter) throws CommandSyntaxException {
        MaterialId material = MaterialArgument.getMaterial(context, "material").getIdentifier();
        return run(context, filter, material.getNamespace(), material.getPath());
    }

    /**
     * Runs the command, filtered by a mod ID
     */
    private static int runModId(CommandContext<ServerCommandSource> context, Operation filter) throws CommandSyntaxException {
        return run(context, filter, context.getArgument("mod_id", String.class), "");
    }

    /**
     * Runs the command
     */
    private static int run(CommandContext<ServerCommandSource> context, Operation filter, String modId, String materialName) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> SUCCESS, true);
        TinkerNetwork.getInstance().sendTo(new GeneratePartTexturesPacket(filter, modId, materialName), source.getPlayerOrThrow());
        return 0;
    }
}
