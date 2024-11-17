package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.DrainBlockEntity;

/**
 * Extenson to include interaction behavior
 */
public class SearedDrainBlock extends RetexturedOrientableSmelteryBlock {
    public SearedDrainBlock(Settings properties) {
        super(properties, DrainBlockEntity::new);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (FluidTransferHelper.interactWithFluidItem(world, pos, player, hand, hit)) {
            return ActionResult.SUCCESS;
        } else if (FluidTransferHelper.interactWithBucket(world, pos, player, hand, hit.getSide(), state.get(FACING).getOpposite())) {
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
