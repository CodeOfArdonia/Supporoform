package slimeknights.tconstruct.library.tools.helper;

import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolActions;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.mining.HarvestEnchantmentsModifierHook;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.BlockSideHitListener;

import java.util.Collections;
import java.util.Objects;

/**
 * External logic for the ToolCore that handles mining calculations and breaking blocks.
 * TODO: needs big updates
 */
public class ToolHarvestLogic {
    private ToolHarvestLogic() {
    }

    /**
     * Gets the amount of damage this tool should take for the given block state
     *
     * @param tool  Tool to check
     * @param state State to check
     * @return Damage to deal
     */
    public static int getDamage(ToolStack tool, World world, BlockPos pos, BlockState state) {
        if (state.getHardness(world, pos) == 0 || !tool.hasTag(TinkerTags.Items.HARVEST)) {
            // tools that can shear take damage from instant break for non-fire
            return (!state.isIn(BlockTags.FIRE) && ModifierUtil.canPerformAction(tool, ToolActions.SHEARS_DIG)) ? 1 : 0;
        }
        // if it lacks the harvest tag, it takes double damage (swords for instance)
        return tool.hasTag(TinkerTags.Items.HARVEST_PRIMARY) ? 1 : 2;
    }

    /**
     * Actually removes a block from the world. Cloned from {@link net.minecraft.server.network.ServerPlayerInteractionManager}
     *
     * @param tool    Tool used in breaking
     * @param context Harvest context
     * @return True if the block was removed
     */
    private static boolean removeBlock(IToolStackView tool, ToolHarvestContext context) {
        Boolean removed = null;
        if (!tool.isBroken()) {
            for (ModifierEntry entry : tool.getModifierList()) {
                removed = entry.getHook(ModifierHooks.REMOVE_BLOCK).removeBlock(tool, entry, context);
                if (removed != null) {
                    break;
                }
            }
        }
        // if not removed by any modifier, remove with normal forge hook
        BlockState state = context.getState();
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getPos();
        if (removed == null) {
            removed = state.onDestroyedByPlayer(world, pos, context.getPlayer(), context.canHarvest(), world.getFluidState(pos));
        }
        // if removed by anything, finally destroy it
        if (removed) {
            state.getBlock().onBroken(world, pos, state);
        }
        return removed;
    }

    /**
     * Called to break a block using this tool
     *
     * @param tool    Tool instance
     * @param stack   Stack instance for vanilla functions
     * @param context Harvest context
     * @return True if broken
     */
    protected static boolean breakBlock(ToolStack tool, ItemStack stack, ToolHarvestContext context) {
        // have to rerun the event to get the EXP, also ensures extra blocks broken get EXP properly
        ServerPlayerEntity player = Objects.requireNonNull(context.getPlayer());
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getPos();
        GameMode type = player.interactionManager.getGameMode();
        int exp = ForgeHooks.onBlockBreakEvent(world, type, player, pos);
        if (exp == -1) {
            return false;
        }
        // checked after the Forge hook, so we have to recheck
        if (player.isBlockBreakingRestricted(world, pos, type)) {
            return false;
        }

        // creative just removes the block
        if (player.isCreative()) {
            removeBlock(tool, context);
            return true;
        }

        // determine damage to do
        BlockState state = context.getState();
        int damage = getDamage(tool, world, pos, state);

        // remove the block
        boolean canHarvest = context.canHarvest();
        BlockEntity te = canHarvest ? world.getBlockEntity(pos) : null; // ensures tile entity is fetched so its around for afterBlockBreak
        boolean removed = removeBlock(tool, context);

        // harvest drops
        Block block = state.getBlock();
        if (removed && canHarvest) {
            block.afterBreak(world, player, pos, state, te, stack);
        }

        // drop XP
        if (removed && exp > 0) {
            block.dropExperience(world, pos, exp);
        }

        // handle modifiers if not broken
        // broken means we are using "empty hand"
        if (!tool.isBroken() && removed) {
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.BLOCK_BREAK).afterBlockBreak(tool, entry, context);
            }
            ToolDamageUtil.damageAnimated(tool, damage, player);
        }

        return true;
    }

    /**
     * Breaks a secondary block
     *
     * @param tool    Tool instance
     * @param stack   Stack instance for vanilla functions
     * @param context Tool harvest context
     */
    public static void breakExtraBlock(ToolStack tool, ItemStack stack, ToolHarvestContext context) {
        // break the actual block
        if (breakBlock(tool, stack, context)) {
            World world = context.getWorld();
            BlockPos pos = context.getPos();
            // need to send the event to tell the client a block was broken
            // normally this is sent within one of the block breaking hooks that is called on both sides, suppressing the packet being sent to the breaking player
            // we only break the center block client side, so need to send the event directly
            // TODO: in theory, we can use this to reduce the number of sounds playing on breaking a lot of blocks, would require sending a custom packet if we want the particles still
            world.syncWorldEvent(2001, pos, Block.getRawIdFromState(context.getState()));
            TinkerNetwork.getInstance().sendVanillaPacket(Objects.requireNonNull(context.getPlayer()), new BlockUpdateS2CPacket(world, pos));
        }
    }

    /**
     * Call on block break to break a block.
     * Used in {@link net.minecraftforge.common.extensions.IForgeItem#onBlockStartBreak(ItemStack, BlockPos, Player)}.
     * See also {@link net.minecraft.client.network.ClientPlayerInteractionManager#breakBlock(BlockPos)} (client)
     * and {@link net.minecraft.server.network.ServerPlayerInteractionManager#tryBreakBlock(BlockPos)} (server)
     *
     * @param stack  Stack instance
     * @param pos    Position to break
     * @param player Player instance
     * @return True if the block break is overridden.
     */
    public static boolean handleBlockBreak(ItemStack stack, BlockPos pos, PlayerEntity player) {
        // TODO: offhand harvest reconsidering
    /* this is a really dumb hack.
    // Basically when something with silktouch harvests a block from the offhand
    // the game can't detect that. so we have to switch around the items in the hands for the break call
    // it's switched back in onBlockDestroyed
    if (DualToolHarvestUtil.shouldUseOffhand(player, pos, player.getHeldItemMainhand())) {
      ItemStack off = player.getHeldItemOffhand();

      this.switchItemsInHands(player);
      // remember, off is in the mainhand now
      CompoundNBT tag = off.getOrCreateTag();
      tag.putLong(TAG_SWITCHED_HAND_HAX, player.getEntityWorld().getGameTime());
      off.setTag(tag);
    }*/

        //return this.breakBlock(stack, pos, player);

        // client can run normal block breaking
        if (player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        // create contexts
        ServerWorld world = serverPlayer.getServerWorld();
        ToolStack tool = ToolStack.from(stack);
        BlockState state = world.getBlockState(pos);
        Direction sideHit = BlockSideHitListener.getSideHit(player);

        // if broken, clear the item stack temporarily then break
        if (tool.isBroken()) {
            // no harvest context
            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            ToolHarvestContext context = new ToolHarvestContext(world, serverPlayer, state, pos, sideHit,
                    !player.isCreative() && state.canHarvestBlock(world, pos, player), false);
            breakBlock(tool, ItemStack.EMPTY, context);
            player.setStackInHand(Hand.MAIN_HAND, stack);
        } else {
            // add in harvest info
            // must not be broken, and the tool definition must be effective
            ToolHarvestContext context = new ToolHarvestContext(world, serverPlayer, state, pos, sideHit,
                    !player.isCreative() && state.canHarvestBlock(world, pos, player),
                    IsEffectiveToolHook.isEffective(tool, state));
            // tell modifiers we are about to harvest, lets them add for instance modifiers conditioned on harvesting
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.BLOCK_HARVEST).startHarvest(tool, entry, context);
            }
            // let armor change enchantments
            // TODO: should we have a hook for non-enchantment armor responses?
            NbtList originalEnchantments = HarvestEnchantmentsModifierHook.updateHarvestEnchantments(tool, stack, context);
            // need to calculate the iterator before we break the block, as we need the reference hardness from the center
            Iterable<BlockPos> extraBlocks = context.isEffective() ? tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, stack, player, state, world, pos, sideHit, AreaOfEffectIterator.AOEMatchType.BREAKING) : Collections.emptyList();

            // actually break the block, run AOE if successful
            boolean didHarvest = breakBlock(tool, stack, context);
            if (didHarvest) {
                for (BlockPos extraPos : extraBlocks) {
                    BlockState extraState = world.getBlockState(extraPos);
                    // prevent calling that stuff for air blocks, could lead to unexpected behaviour since it fires events
                    // this should never actually happen, but just in case some AOE is odd
                    if (!extraState.isAir()) {
                        // prevent mutable position leak, breakBlock has a few places wanting immutable
                        breakExtraBlock(tool, stack, context.forPosition(extraPos.toImmutable(), extraState));
                    }
                }
            }
            // restore the enchantments harvest changed
            if (originalEnchantments != null) {
                HarvestEnchantmentsModifierHook.restoreEnchantments(stack, originalEnchantments);
            }
            // alert modifiers we finished harvesting
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.BLOCK_HARVEST).finishHarvest(tool, entry, context, didHarvest);
            }
        }

        return true;
    }

    /**
     * Handles {@link net.minecraft.item.Item#postMine(net.minecraft.item.ItemStack, net.minecraft.world.World, net.minecraft.block.BlockState, net.minecraft.util.math.BlockPos, net.minecraft.entity.LivingEntity)} for modifiable items
     */
    public static boolean mineBlock(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return false;
        }

        if (!worldIn.isClient && worldIn instanceof ServerWorld) {
            // must not be broken, and the tool definition must be effective
            boolean isEffective = IsEffectiveToolHook.isEffective(tool, state);
            ToolHarvestContext context = new ToolHarvestContext((ServerWorld) worldIn, entityLiving, state, pos, Direction.UP, true, isEffective);
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.BLOCK_BREAK).afterBlockBreak(tool, entry, context);
            }
            ToolDamageUtil.damageAnimated(tool, ToolHarvestLogic.getDamage(tool, worldIn, pos, state), entityLiving);
        }

        return true;
    }
}
