package slimeknights.tconstruct.shared.command.subcommand;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.RequiredArgsConstructor;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import slimeknights.mantle.command.MantleCommand;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.shared.command.HeldModifiableItemIterator;
import slimeknights.tconstruct.shared.command.argument.ToolStatArgument;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.slotless.StatOverrideModifier;

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

/**
 * Command to modify a tool's stats
 */
public class StatsCommand {
    private static final String SUCCESS_KEY_PREFIX = TConstruct.makeTranslationKey("command", "stats.success.");
    private static final String RESET_ALL_SINGLE = TConstruct.makeTranslationKey("command", "stats.success.reset.all.single");
    private static final String RESET_ALL_MULTIPLE = TConstruct.makeTranslationKey("command", "stats.success.reset.all.multiple");
    private static final String RESET_STAT_SINGLE = TConstruct.makeTranslationKey("command", "stats.success.reset.stat.single");
    private static final String RESET_STAT_MULTIPLE = TConstruct.makeTranslationKey("command", "stats.success.reset.stat.multiple");
    private static final SimpleCommandExceptionType INVALID_ADD = new SimpleCommandExceptionType(TConstruct.makeTranslation("command", "stats.failure.invalid_add"));
    private static final SimpleCommandExceptionType INVALID_MULTIPLY = new SimpleCommandExceptionType(TConstruct.makeTranslation("command", "stats.failure.invalid_multiply"));
    private static final Dynamic2CommandExceptionType FAILED_TO_PARSE = new Dynamic2CommandExceptionType((stat, tag) -> TConstruct.makeTranslation("command", "stats.success.bonus.set.parse_fail", stat, tag));
    private static final DynamicCommandExceptionType MODIFIER_ERROR = new DynamicCommandExceptionType(error -> (Text) error);

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(sender -> sender.hasPermissionLevel(MantleCommand.PERMISSION_GAME_COMMANDS))
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                        // stats <target> bonus add|set <stat_type> <value>
                        .then(CommandManager.literal("bonus")
                                .then(CommandManager.literal("add")
                                        // TODO: is there a way we can use this to set max stats? would require a way to parse the stat (we have)
                                        .then(CommandManager.argument("stat_type", ToolStatArgument.stat(INumericToolStat.class))
                                                .then(CommandManager.argument("value", FloatArgumentType.floatArg())
                                                        .executes(context -> update(context, Type.BONUS, Operation.MODIFY)))))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("stat_type", ToolStatArgument.stat())
                                                .then(CommandManager.argument("value", NbtElementArgumentType.nbtElement())
                                                        .executes(context -> update(context, Type.BONUS, Operation.SET))))))
                        // stats <target> multiplier multiply|set <float_stat> <value>
                        .then(CommandManager.literal("multiplier")
                                .then(CommandManager.literal("multiply")
                                        .then(CommandManager.argument("float_stat", ToolStatArgument.stat(INumericToolStat.class))
                                                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0))
                                                        .executes(context -> update(context, Type.MULTIPLY, Operation.MODIFY)))))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("float_stat", ToolStatArgument.stat(INumericToolStat.class))
                                                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0))
                                                        .executes(context -> update(context, Type.MULTIPLY, Operation.SET))))))
                        // stats <target> reset [<stat_type>]
                        .then(CommandManager.literal("reset")
                                .executes(StatsCommand::resetAll)
                                .then(CommandManager.argument("stat_type", ToolStatArgument.stat())
                                        .executes(StatsCommand::resetStat))));
    }

    /**
     * Updates entities using the given operation
     */
    private static List<LivingEntity> updateEntities(CommandContext<ServerCommandSource> context, BiPredicate<IToolStackView, StatOverrideModifier> updateAction) throws CommandSyntaxException {
        return HeldModifiableItemIterator.apply(context, (living, stack) -> {
            ToolStack tool = ToolStack.copyFrom(stack);

            // apply the proper operation
            StatOverrideModifier stats = TinkerModifiers.statOverride.get();
            boolean needsModifier = updateAction.test(tool, stats);

            // ensure the modifier is present if needed/not present if not needed
            int level = tool.getUpgrades().getLevel(stats.getId());
            boolean hasModifier = level > 0;
            if (needsModifier && !hasModifier) {
                tool.addModifier(stats.getId(), 1);
            } else if (hasModifier && !needsModifier) {
                tool.removeModifier(stats.getId(), level);
            } else {
                tool.rebuildStats();
            }

            // ensure the tool is still valid
            Text validated = tool.tryValidate();
            if (validated != null) {
                throw MODIFIER_ERROR.create(validated);
            }

            // if successful, update held item
            living.setStackInHand(Hand.MAIN_HAND, tool.createStack(stack.getCount()));
            return true;
        });
    }

    /**
     * Sets the given stat using NBT
     */
    private static <T> List<LivingEntity> setStat(CommandContext<ServerCommandSource> context, IToolStat<T> stat, NbtElement tag) throws CommandSyntaxException {
        T value = stat.read(tag);
        if (value == null) {
            throw FAILED_TO_PARSE.create(stat.getPrefix(), tag);
        }
        return updateEntities(context, (tool, stats) -> stats.set(tool, stat, value));
    }

    /**
     * Modifies a tool stat with the given operation
     */
    private static int update(CommandContext<ServerCommandSource> context, Type type, Operation op) throws CommandSyntaxException {
        // simplifies later operations if we skip operations that do nothing
        List<LivingEntity> successes;
        IToolStat<?> stat = ToolStatArgument.getStat(context, type.stat);
        Object display;
        if (op == Operation.SET && type == Type.BONUS) {
            NbtElement tag = NbtElementArgumentType.getNbtElement(context, "value");
            successes = setStat(context, stat, tag);
            display = tag;
        } else {
            float value = FloatArgumentType.getFloat(context, "value");
            if (op == Operation.MODIFY) {
                if (value == 0 && type == Type.BONUS) {
                    throw INVALID_ADD.create();
                }
                if (value == 1 && type == Type.MULTIPLY) {
                    throw INVALID_MULTIPLY.create();
                }
            }
            INumericToolStat<?> numeric = (INumericToolStat<?>) stat;
            if (type == Type.BONUS) {
                successes = updateEntities(context, (tool, stats) -> stats.addBonus(tool, numeric, value));
            } else if (op == Operation.SET) {
                successes = updateEntities(context, (tool, stats) -> stats.setMultiplier(tool, numeric, value));
            } else {
                successes = updateEntities(context, (tool, stats) -> stats.multiply(tool, numeric, value));
            }
            display = value;
        }

        // success message
        ServerCommandSource source = context.getSource();
        int size = successes.size();
        String successKey = SUCCESS_KEY_PREFIX + type.key + "." + op.key + ".";
        if (size == 1) {
            source.sendFeedback(() -> Text.translatable(successKey + "single", stat.getPrefix(), display, successes.get(0).getDisplayName()), true);
        } else {
            source.sendFeedback(() -> Text.translatable(successKey + "multiple", stat.getPrefix(), display, size), true);
        }
        return size;
    }

    /**
     * Resets all stats to default
     */
    private static int resetStat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        IToolStat<?> stat = ToolStatArgument.getStat(context, "stat_type");
        List<LivingEntity> successes = updateEntities(context, (tool, stats) -> {
            if (stat instanceof INumericToolStat<?> numeric) {
                stats.setMultiplier(tool, numeric, 1);
            }
            return stats.remove(tool, stat);
        });

        // success message
        ServerCommandSource source = context.getSource();
        int size = successes.size();
        if (size == 1) {
            source.sendFeedback(() -> Text.translatable(RESET_STAT_SINGLE, stat.getPrefix(), successes.get(0).getDisplayName()), true);
        } else {
            source.sendFeedback(() -> Text.translatable(RESET_STAT_MULTIPLE, stat.getPrefix(), size), true);
        }
        return size;
    }

    /**
     * Resets all stats to default
     */
    private static int resetAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        List<LivingEntity> successes = HeldModifiableItemIterator.apply(context, (living, stack) -> {
            // remove modifier
            ToolStack tool = ToolStack.from(stack);
            StatOverrideModifier stats = TinkerModifiers.statOverride.get();
            int level = tool.getModifierLevel(stats);
            if (level > 0) {
                tool = tool.copy();
                tool.removeModifier(stats.getId(), level);

                // ensure the tool is still valid
                Text error = tool.tryValidate();
                if (error != null) {
                    throw MODIFIER_ERROR.create(error);
                }
                error = stats.getHook(ModifierHooks.REMOVE).onRemoved(tool, stats);
                if (error != null) {
                    throw MODIFIER_ERROR.create(error);
                }

                // if successful, update held item
                living.setStackInHand(Hand.MAIN_HAND, tool.createStack(stack.getCount()));
            }
            return true;
        });

        // success message
        ServerCommandSource source = context.getSource();
        int size = successes.size();
        if (size == 1) {
            source.sendFeedback(() -> Text.translatable(RESET_ALL_SINGLE, successes.get(0).getDisplayName()), true);
        } else {
            source.sendFeedback(() -> Text.translatable(RESET_ALL_MULTIPLE, size), true);
        }
        return size;
    }

    @RequiredArgsConstructor
    private enum Type {
        BONUS("stat_type"),
        MULTIPLY("float_stat");
        private final String key = name().toLowerCase(Locale.US);
        private final String stat;
    }

    private enum Operation {
        MODIFY, SET;
        private final String key = name().toLowerCase(Locale.US);
    }
}
