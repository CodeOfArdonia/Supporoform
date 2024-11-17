package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.SafeClient;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

/**
 * Common logic between the tank and the melter
 */
public interface ITankBlockEntity extends IFluidTankUpdater, FluidUpdatePacket.IFluidPacketReceiver {
    /**
     * Gets the tank in this tile entity
     *
     * @return Tank
     */
    FluidTankAnimated getTank();

    /*
     * Comparator
     */

    /**
     * Gets the comparator strength for the tank
     *
     * @return Tank comparator strength
     */
    default int comparatorStrength() {
        FluidTankAnimated tank = this.getTank();
        return 15 * tank.getFluidAmount() / tank.getCapacity();
    }

    /**
     * Gets the last comparator strength for this tank
     *
     * @return Last comparator strength
     */
    int getLastStrength();

    /**
     * Updates the last comparator strength for this tank
     *
     * @param strength Last comparator strength
     */
    void setLastStrength(int strength);

    @Override
    default void onTankContentsChanged() {
        int newStrength = this.comparatorStrength();
        BlockEntity te = this.getTE();
        World world = te.getWorld();
        if (newStrength != this.getLastStrength() && world != null) {
            world.updateNeighborsAlways(te.getPos(), te.getCachedState().getBlock());
            this.setLastStrength(newStrength);
        }
    }

    /*
     * Fluid tank updater
     */

    /**
     * If true, the fluid is rendered as part of the model
     */
    default boolean isFluidInModel() {
        return Config.CLIENT.tankFluidModel.get();
    }

    @Override
    default void updateFluidTo(FluidStack fluid) {
        // update tank fluid
        FluidTankAnimated tank = this.getTank();
        int oldAmount = tank.getFluidAmount();
        int newAmount = fluid.getAmount();
        tank.setFluid(fluid);

        // update the tank render offset from the change
        tank.setRenderOffset(tank.getRenderOffset() + newAmount - oldAmount);

        // update the block model
        if (this.isFluidInModel()) {
            SafeClient.updateFluidModel(this.getTE(), tank, oldAmount, newAmount);
        }
    }


    /*
     * Tile entity methods
     */

    /**
     * @return tile entity world
     */
    default BlockEntity getTE() {
        return (BlockEntity) this;
    }

    /*
     * Helpers
     */

    /**
     * Implements logic for {@link net.minecraft.block.Block#getComparatorOutput(BlockState, World, BlockPos)}
     *
     * @param world World instance
     * @param pos   Block position
     * @return Comparator power
     */
    static int getComparatorInputOverride(WorldAccess world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (!(te instanceof ITankBlockEntity)) {
            return 0;
        }
        return ((ITankBlockEntity) te).comparatorStrength();
    }
}
