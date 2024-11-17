package slimeknights.tconstruct.common.multiblock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Base interface for master block entities
 */
public interface IMasterLogic {
    private BlockEntity self() {
        return (BlockEntity) this;
    }

    /**
     * Gets the block of the master tile entity
     */
    default BlockState getMasterBlock() {
        return this.self().getCachedState();
    }

    /**
     * Gets the position of the master tile entity
     */
    default BlockPos getMasterPos() {
        return this.self().getPos();
    }

    /**
     * Called when servants change their state
     *
     * @param pos   Position that changed. May not be the servant position
     * @param state State that changed. May not be the servant state
     */
    void notifyChange(BlockPos pos, BlockState state);
}
