package slimeknights.tconstruct.smeltery.block.component;

import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SearedBlock extends Block implements BlockEntityProvider {
    public static final BooleanProperty IN_STRUCTURE = BooleanProperty.of("in_structure");

    protected final boolean requiredBlockEntity;

    public SearedBlock(Settings properties, boolean requiredBlockEntity) {
        super(properties);
        this.requiredBlockEntity = requiredBlockEntity;
        this.setDefaultState(this.getDefaultState().with(IN_STRUCTURE, false));
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(IN_STRUCTURE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (this.requiredBlockEntity || state.get(IN_STRUCTURE)) {
            return new SmelteryComponentBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    @Deprecated
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (this.requiredBlockEntity || oldState.get(IN_STRUCTURE)) {
            // if the block is unchanged, remove the block entity if we no longer have one
            if (newState.isOf(this)) {
                if (!this.requiredBlockEntity && !newState.get(IN_STRUCTURE)) {
                    world.removeBlockEntity(pos);
                }
            } else {
                // block changed, tell the master then ditch the block entity
                BlockEntityHelper.get(SmelteryComponentBlockEntity.class, world, pos).ifPresent(te -> te.notifyMasterOfChange(pos, newState));
                world.removeBlockEntity(pos);
            }
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        SmelteryComponentBlockEntity.updateNeighbors(world, pos, state);
    }

    @Override
    @Deprecated
    public boolean onSyncedBlockEvent(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.onSyncedBlockEvent(state, worldIn, pos, id, param);
        BlockEntity be = worldIn.getBlockEntity(pos);
        return be != null && be.onSyncedBlockEvent(id, param);
    }

    @Nullable
    @Override
    public PathNodeType getBlockPathType(BlockState state, BlockView level, BlockPos pos, @Nullable MobEntity mob) {
        return state.get(IN_STRUCTURE) ? PathNodeType.DAMAGE_FIRE : PathNodeType.OPEN;
    }
}
