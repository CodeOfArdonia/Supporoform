package slimeknights.tconstruct.smeltery.block.entity.component;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.common.multiblock.ServantTileEntity;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/**
 * Mostly extended to make type validaton easier, and the servant base class is not registered
 */
public class SmelteryComponentBlockEntity extends ServantTileEntity {

    public SmelteryComponentBlockEntity(BlockPos pos, BlockState state) {
        this(TinkerSmeltery.smelteryComponent.get(), pos, state);
    }

    protected SmelteryComponentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Block method to update neighbors of a smeltery component when a new one is placed
     *
     * @param world World instance
     * @param pos   Location of new smeltery component
     */
    public static void updateNeighbors(World world, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            // if the neighbor is a master, notify it we exist
            BlockEntity tileEntity = world.getBlockEntity(pos.offset(direction));
            if (tileEntity instanceof IMasterLogic master) {
                master.notifyChange(pos, state);
                break;
                // if the neighbor is a servant, notify its master we exist
            } else if (tileEntity instanceof SmelteryComponentBlockEntity component && component.hasMaster()) {
                component.notifyMasterOfChange(pos, state);
                break;
            }
        }
    }
}
