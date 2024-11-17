package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.InventoryBlock;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.DuctBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

/**
 * Filtering drain block, have to reimplement either inventory block logic or seared block logic unfortunately
 */
public class SearedDuctBlock extends InventoryBlock {
    public static final BooleanProperty IN_STRUCTURE = SearedBlock.IN_STRUCTURE;
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public SearedDuctBlock(Settings properties) {
        super(properties);
        this.setDefaultState(this.getDefaultState().with(IN_STRUCTURE, false));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new DuctBlockEntity(pPos, pState);
    }

    /* Seared block interaction */

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!newState.isOf(this)) {
            BlockEntityHelper.get(SmelteryComponentBlockEntity.class, worldIn, pos).ifPresent(te -> te.notifyMasterOfChange(pos, newState));
        }
        super.onStateReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        SmelteryComponentBlockEntity.updateNeighbors(world, pos, state);
        RetexturedBlock.updateTextureBlock(world, pos, stack);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return RetexturedBlock.getPickBlock(world, pos, state);
    }


    /* Orientation */

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(IN_STRUCTURE, FACING);
    }

    @Nullable
    @Override
    public PathNodeType getBlockPathType(BlockState state, BlockView level, BlockPos pos, @Nullable MobEntity mob) {
        return state.get(IN_STRUCTURE) ? PathNodeType.DAMAGE_FIRE : PathNodeType.OPEN;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(FACING, mirror.apply(state.get(FACING)));
    }
}
