package slimeknights.tconstruct.shared.block;

import java.util.EnumMap;

import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;

/**
 * Pane block with sensible culling
 */
public class BetterPaneBlock extends PaneBlock {
    public static final EnumMap<Direction, BooleanProperty> DIRECTIONS;

    static {
        DIRECTIONS = new EnumMap<>(Direction.class);
        DIRECTIONS.put(Direction.NORTH, NORTH);
        DIRECTIONS.put(Direction.EAST, EAST);
        DIRECTIONS.put(Direction.SOUTH, SOUTH);
        DIRECTIONS.put(Direction.WEST, WEST);
    }

    public BetterPaneBlock(Settings builder) {
        super(builder);
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
        // cull top and bottom if all props that we have are contained in the above or below
        if (adjacentBlockState.getBlock() == this && side.getAxis().isVertical()) {
            for (Direction dir : Type.HORIZONTAL) {
                BooleanProperty prop = DIRECTIONS.get(dir);
                if (state.get(prop) && !adjacentBlockState.get(prop)) {
                    return false;
                }
            }
            return true;
        }
        return super.isSideInvisible(state, adjacentBlockState, side);
    }
}
