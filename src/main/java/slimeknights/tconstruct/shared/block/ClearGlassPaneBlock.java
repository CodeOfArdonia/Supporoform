package slimeknights.tconstruct.shared.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import slimeknights.mantle.block.IMultipartConnectedBlock;
import slimeknights.mantle.client.model.connected.ConnectedModelRegistry;

public class ClearGlassPaneBlock extends BetterPaneBlock implements IMultipartConnectedBlock {
    public ClearGlassPaneBlock(Settings builder) {
        super(builder);
        this.setDefaultState(IMultipartConnectedBlock.defaultConnections(this.getDefaultState()));
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        IMultipartConnectedBlock.fillStateContainer(builder);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess world, BlockPos currentPos, BlockPos facingPos) {
        BlockState state = super.getStateForNeighborUpdate(stateIn, facing, facingState, world, currentPos, facingPos);
        return getConnectionUpdate(state, facing, facingState);
    }

    @Override
    public boolean connects(BlockState state, BlockState neighbor) {
        return ConnectedModelRegistry.getPredicate("pane").test(state, neighbor);
    }
}
