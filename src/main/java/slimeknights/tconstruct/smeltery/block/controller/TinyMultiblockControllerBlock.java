package slimeknights.tconstruct.smeltery.block.controller;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

public abstract class TinyMultiblockControllerBlock extends ControllerBlock {
    private static final Text NO_FUEL_TANK = TConstruct.makeTranslation("multiblock", "tiny.no_fuel_tank");

    protected TinyMultiblockControllerBlock(Settings builder) {
        super(builder);
    }


    /*
     * Fuel tank
     */

    /**
     * Checks if the given state is a valid melter fuel source
     *
     * @param state State instance
     * @return True if its a valid fuel source
     */
    protected boolean isValidFuelSource(BlockState state) {
        return state.isIn(TinkerTags.Blocks.FUEL_TANKS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = super.getPlacementState(context);
        if (state != null) {
            return state.with(IN_STRUCTURE, isValidFuelSource(context.getWorld().getBlockState(context.getBlockPos().down())));
        }
        return null;
    }

    @Deprecated
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.with(IN_STRUCTURE, isValidFuelSource(neighbor));
        }
        return state;
    }

    @Deprecated
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit)) {
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    protected boolean displayStatus(PlayerEntity player, World world, BlockPos pos, BlockState state) {
        if (!world.isClient && !state.get(IN_STRUCTURE)) {
            player.sendMessage(NO_FUEL_TANK, true);
        }
        return true;
    }


    /*
     * Comparator
     */

    @Deprecated
    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Deprecated
    @Override
    public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
        return ITankBlockEntity.getComparatorInputOverride(worldIn, pos);
    }

}
