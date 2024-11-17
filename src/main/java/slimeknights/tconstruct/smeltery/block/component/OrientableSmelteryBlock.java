package slimeknights.tconstruct.smeltery.block.component;

import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Shared logic for smeltery blocks with four directions to face
 */
public class OrientableSmelteryBlock extends SearedBlock implements BlockEntityProvider {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    private final BlockEntityFactory<? extends SmelteryComponentBlockEntity> blockEntity;

    public OrientableSmelteryBlock(Settings properties, boolean requiredBlockEntity, BlockEntityFactory<? extends SmelteryComponentBlockEntity> blockEntity) {
        super(properties, requiredBlockEntity);
        this.blockEntity = blockEntity;
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (requiredBlockEntity || state.get(IN_STRUCTURE)) {
            return this.blockEntity.create(pos, state);
        }
        return null;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Deprecated
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Deprecated
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(FACING, mirror.apply(state.get(FACING)));
    }
}
