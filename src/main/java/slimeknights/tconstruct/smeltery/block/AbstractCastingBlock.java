package slimeknights.tconstruct.smeltery.block;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.shared.block.TableBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

public abstract class AbstractCastingBlock extends TableBlock {
    @Getter
    private final boolean requireCast;

    protected AbstractCastingBlock(Settings builder, boolean requireCast) {
        super(builder);
        this.requireCast = requireCast;
    }

    @Override
    @Deprecated
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState pState, World pLevel, BlockPos pPos) {
        return null;
    }

    @Deprecated
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult rayTraceResult) {
        if (player.isSneaking()) {
            return ActionResult.PASS;
        }
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof CastingBlockEntity) {
            ((CastingBlockEntity) te).interact(player, hand);
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, rayTraceResult);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (worldIn.isClient()) {
            return;
        }
        BlockEntityHelper.get(CastingBlockEntity.class, worldIn, pos).ifPresent(casting -> casting.handleRedstone(worldIn.isReceivingRedstonePower(pos)));
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
        BlockEntityHelper.get(CastingBlockEntity.class, worldIn, pos).ifPresent(CastingBlockEntity::swap);
    }

    @Override
    protected boolean openGui(PlayerEntity playerEntity, World world, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
        return BlockEntityHelper.get(CastingBlockEntity.class, worldIn, pos).map(CastingBlockEntity::getAnalogSignal).orElse(0);
    }
}
