package slimeknights.tconstruct.smeltery.block;

import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CastingTableBlock extends AbstractCastingBlock {

    private static final VoxelShape SHAPE = VoxelShapes.combineAndSimplify(
            VoxelShapes.fullCube(),
            VoxelShapes.union(
                    Block.createCuboidShape(4.0D, 0.0D, 0.0D, 12.0D, 10.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 4.0D, 16.0D, 10.0D, 12.0D),
                    Block.createCuboidShape(1.0D, 15.0D, 1.0D, 15.0D, 16.0D, 15.0D)
            ), BooleanBiFunction.ONLY_FIRST);

    public CastingTableBlock(Settings builder, boolean requireCast) {
        super(builder, requireCast);
    }

    @Deprecated
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new CastingBlockEntity.Table(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World pLevel, BlockState pState, BlockEntityType<T> check) {
        return CastingBlockEntity.getTicker(pLevel, check, TinkerSmeltery.table.get());
    }
}
