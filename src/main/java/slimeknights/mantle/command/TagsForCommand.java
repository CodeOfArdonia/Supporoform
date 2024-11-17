package slimeknights.mantle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import io.github.fabricators_of_create.porting_lib.attributes.PortingLibAttributes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Command to list all tags for an entry
 */
public class TagsForCommand {
    /**
     * Tag type cannot be found
     */
    protected static final Dynamic2CommandExceptionType VALUE_NOT_FOUND = new Dynamic2CommandExceptionType((type, name) -> Text.translatable("command.mantle.tags_for.not_found", type, name));

    /* Missing target errors */
    private static final Text NO_HELD_BLOCK = Text.translatable("command.mantle.tags_for.no_held_block");
    private static final Text NO_HELD_ENTITY = Text.translatable("command.mantle.tags_for.no_held_entity");
    private static final Text NO_HELD_POTION = Text.translatable("command.mantle.tags_for.no_held_potion");
    private static final Text NO_HELD_FLUID = Text.translatable("command.mantle.tags_for.no_held_fluid");
    private static final Text NO_HELD_ENCHANTMENT = Text.translatable("command.mantle.tags_for.no_held_enchantment");
    private static final Text NO_TARGETED_ENTITY = Text.translatable("command.mantle.tags_for.no_targeted_entity");
    private static final Text NO_TARGETED_BLOCK_ENTITY = Text.translatable("command.mantle.tags_for.no_targeted_block_entity");
    /**
     * Value has no tags
     */
    private static final Text NO_TAGS = Text.translatable("command.mantle.tags_for.no_tags");

    /**
     * Registers this sub command with the root command
     *
     * @param subCommand Command builder
     */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> subCommand) {
        subCommand.requires(source -> MantleCommand.requiresDebugInfoOrOp(source, MantleCommand.PERMISSION_GAME_COMMANDS))
                // by registry ID
                .then(CommandManager.literal("id")
                        .then(CommandManager.argument("type", RegistryArgument.registry()).suggests(MantleCommand.REGISTRY)
                                .then(CommandManager.argument("name", IdentifierArgumentType.identifier()).suggests(MantleCommand.REGISTRY_VALUES)
                                        .executes(TagsForCommand::runForId))))
                // held item
                .then(CommandManager.literal("held")
                        .then(CommandManager.literal("item").executes(TagsForCommand::heldItem))
                        .then(CommandManager.literal("block").executes(TagsForCommand::heldBlock))
                        .then(CommandManager.literal("enchantment").executes(TagsForCommand::heldEnchantments))
                        .then(CommandManager.literal("fluid").executes(TagsForCommand::heldFluid))
                        .then(CommandManager.literal("entity").executes(TagsForCommand::heldEntity))
                        .then(CommandManager.literal("potion").executes(TagsForCommand::heldPotion)))
                // targeted
                .then(CommandManager.literal("targeted")
                        .then(CommandManager.literal("block_entity").executes(TagsForCommand::targetedTileEntity))
                        .then(CommandManager.literal("entity").executes(TagsForCommand::targetedEntity)));
    }

    /**
     * Prints the final list of owning tags
     *
     * @param context  Command context
     * @param registry Registry to output
     * @param value    Value to print
     * @param <T>      Collection type
     * @return Number of tags printed
     */
    private static <T> int printOwningTags(CommandContext<ServerCommandSource> context, Registry<T> registry, T value) {
        MutableText output = Text.translatable("command.mantle.tags_for.success", registry.getKey().getValue(), registry.getId(value));
        List<Identifier> tags = registry.getEntry(registry.getRawId(value)).stream().flatMap(RegistryEntry::streamTags).map(TagKey::id).toList();
        if (tags.isEmpty()) {
            output.append("\n* ").append(NO_TAGS);
        } else {
            tags.stream()
                    .sorted(Identifier::compareNamespaced)
                    .forEach(tag -> output.append("\n* " + tag));
        }
        context.getSource().sendFeedback(() -> output, true);
        return tags.size();
    }


    /* Standard way: by ID */

    /**
     * Runs the registry ID subcommand making generics happy
     */
    private static <T> int runForResult(CommandContext<ServerCommandSource> context, Registry<T> registry) throws CommandSyntaxException {
        Identifier name = context.getArgument("name", Identifier.class);
        // first, fetch value
        T value = registry.get(name);
        if (value == null) {
            throw VALUE_NOT_FOUND.create(registry.getKey().getValue(), name);
        }
        return printOwningTags(context, registry, value);
    }

    /**
     * Run the registry ID subcommand
     */
    private static int runForId(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Registry<?> result = RegistryArgument.getResult(context, "type");
        return runForResult(context, result);
    }


    /* Held item, can extract some data from the stack */

    /**
     * Item tags for held item
     */
    private static int heldItem(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = context.getSource().getPlayerOrThrow().getMainHandStack().getItem();
        return printOwningTags(context, Registries.ITEM, item);
    }

    /**
     * Block tags for held item
     */
    private static int heldBlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Item item = source.getPlayerOrThrow().getMainHandStack().getItem();
        Block block = Block.getBlockFromItem(item);
        if (block != Blocks.AIR) {
            return printOwningTags(context, Registries.BLOCK, block);
        }
        source.sendFeedback(() -> NO_HELD_BLOCK, true);
        return 0;
    }

    /**
     * Fluid tags for held item
     */
    private static int heldFluid(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ItemStack stack = source.getPlayerOrThrow().getMainHandStack();
        LazyOptional<IFluidHandlerItem> capability = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (capability.isPresent()) {
            IFluidHandler handler = capability.map(h -> (IFluidHandler) h).orElse(EmptyFluidHandler.INSTANCE);
            if (handler.getTanks() > 0) {
                FluidStack fluidStack = handler.getFluidInTank(0);
                if (!fluidStack.isEmpty()) {
                    Fluid fluid = fluidStack.getFluid();
                    return printOwningTags(context, Registries.FLUID, fluid);
                }
            }
        }
        source.sendFeedback(() -> NO_HELD_FLUID, true);
        return 0;
    }

    /**
     * Potion tags for held item
     */
    private static int heldPotion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ItemStack stack = source.getPlayerOrThrow().getMainHandStack();
        Potion potion = PotionUtil.getPotion(stack);
        if (potion != Potions.EMPTY) {
            return printOwningTags(context, Registries.POTION, potion);
        }
        source.sendFeedback(() -> NO_HELD_POTION, true);
        return 0;
    }

    /**
     * Block tags for held item
     */
    private static int heldEnchantments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ItemStack stack = source.getPlayerOrThrow().getMainHandStack();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        if (!enchantments.isEmpty()) {
            int totalTags = 0;
            // print tags for each contained enchantment
            for (Enchantment enchantment : enchantments.keySet()) {
                totalTags += printOwningTags(context, Registries.ENCHANTMENT, enchantment);
            }
            return totalTags;
        }
        source.sendFeedback(() -> NO_HELD_ENCHANTMENT, true);
        return 0;
    }

    /**
     * Entity tags for held item
     */
    private static int heldEntity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ItemStack stack = source.getPlayerOrThrow().getMainHandStack();
        if (stack.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> type = egg.getEntityType(stack.getNbt());
            return printOwningTags(context, Registries.ENTITY_TYPE, type);
        }
        source.sendFeedback(() -> NO_HELD_ENTITY, true);
        return 0;
    }


    /* Targeted, based on look vector. Leaves out anything on the debug screen */

    /**
     * Gets the tags for the fluid being looked at
     *
     * @param context Context
     * @return Tags for the looked at block or entity
     * @throws CommandSyntaxException For command errors
     */
    private static int targetedTileEntity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        PlayerEntity player = source.getPlayerOrThrow();
        World level = source.getWorld();
        BlockHitResult blockTrace = Item.raycast(level, player, RaycastContext.FluidHandling.NONE);
        if (blockTrace.getType() == HitResult.Type.BLOCK) {
            BlockEntity be = level.getBlockEntity(blockTrace.getBlockPos());
            if (be != null) {
                BlockEntityType<?> type = be.getType();
                return printOwningTags(context, Registries.BLOCK_ENTITY_TYPE, type);
            }
        }
        // failed
        source.sendFeedback(() -> NO_TARGETED_BLOCK_ENTITY, true);
        return 0;
    }

    /**
     * Gets the tags for the entity being looked at
     *
     * @param context Context
     * @return Tags for the looked at block or entity
     * @throws CommandSyntaxException For command errors
     */
    private static int targetedEntity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        PlayerEntity player = source.getPlayerOrThrow();
        Vec3d start = player.getCameraPosVec(1F);
        Vec3d look = player.getRotationVector();
        double range = Objects.requireNonNull(player.getAttributeInstance(PortingLibAttributes.ENTITY_REACH)).getValue();
        Vec3d direction = start.add(look.x * range, look.y * range, look.z * range);
        Box bb = player.getBoundingBox().stretch(look.x * range, look.y * range, look.z * range).stretch(1, 1, 1);
        EntityHitResult entityTrace = ProjectileUtil.getEntityCollision(source.getWorld(), player, start, direction, bb, e -> true);
        if (entityTrace != null) {
            EntityType<?> target = entityTrace.getEntity().getType();
            return printOwningTags(context, Registries.ENTITY_TYPE, target);
        }
        // failed
        source.sendFeedback(() -> NO_TARGETED_ENTITY, true);
        return 0;
    }
}
