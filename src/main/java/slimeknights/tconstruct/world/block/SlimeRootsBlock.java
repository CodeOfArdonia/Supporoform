package slimeknights.tconstruct.world.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.MangroveRootsBlock;
import net.minecraft.util.math.Direction;

public class SlimeRootsBlock extends MangroveRootsBlock {
    public SlimeRootsBlock(Settings props) {
        super(props);
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState neighbor, Direction side) {
        return neighbor.isOf(this) && side.getAxis() == Direction.Axis.Y;
    }
}
