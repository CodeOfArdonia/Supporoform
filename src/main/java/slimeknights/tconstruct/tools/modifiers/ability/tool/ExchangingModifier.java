package slimeknights.tconstruct.tools.modifiers.ability.tool;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.common.network.UpdateNeighborsPacket;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.mining.RemoveBlockModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

public class ExchangingModifier extends NoLevelsModifier implements RemoveBlockModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.REMOVE_BLOCK);
    }

    @Override
    public int getPriority() {
        // super low because we need to run after the shears ability modifier, and any other similar hooks
        return Short.MIN_VALUE - 20;
    }

    @Override
    public Boolean removeBlock(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        // must have blocks in the offhand
        ItemStack offhand = context.getLiving().getOffHandStack();
        BlockState state = context.getState();
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        // from this point on, we are in charge of breaking the block, start by harvesting it so piglins get mad and stuff
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            state.getBlock().onBreak(world, pos, state, player);
        }

        // block is unchanged, stuck setting it to a temporary block before replacing, as otherwise we risk duplication with the TE and tryPlace will likely fail
        BlockState fluidState = world.getFluidState(pos).getBlockState();
        boolean placedBlock = false;
        if (state.getBlock() == blockItem.getBlock()) {
            // the 0 in the last parameter prevents neighbor updates, meaning torches won't drop
            // this is fine as the block will be replaced in the next step by the proper block,
            // however doing it in one step is probably more ideal for block updates, hence only doing it when needed
            placedBlock = world.setBlockState(pos, fluidState, 0, 0);
            if (!placedBlock) {
                return false;
            }
        }

        // generate placing context
        Direction sideHit = context.getSideHit();
        // subtract the offsets instead of adding as the position is empty, want to "hit" a realistic location
        ItemPlacementContext blockUseContext = new ItemPlacementContext(world, player, Hand.OFF_HAND, offhand, Util.createTraceResult(pos, sideHit, true));
        blockUseContext.canReplaceExisting = true; // force replacement, even if the position is not replacable (as it most always will be)

        // swap the block, it never goes to air so things like torches will remain
        ActionResult success = blockItem.place(blockUseContext);
        if (success.isAccepted()) {
            if (!context.isAOE() && player != null) {
                TinkerNetwork.getInstance().sendTo(new UpdateNeighborsPacket(state, pos), player);
            }
            context.getLiving().swingHand(Hand.OFF_HAND, false);
            return true;
        } else if (placedBlock) {
            // notify that the fluid was placed properly, as it was suppressed earlier, and placing again will fail to hit it
            state.prepare(world, pos, Block.NOTIFY_LISTENERS, 511);
            fluidState.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS, 511);
            fluidState.prepare(world, pos, Block.NOTIFY_LISTENERS, 511);
            return true;
        } else {
            // so we failed to place the new block for some reason, remove the old block to prevent dupes
            return world.setBlockState(pos, fluidState, 3);
        }
    }
}
