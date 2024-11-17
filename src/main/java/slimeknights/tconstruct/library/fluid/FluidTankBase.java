package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

public class FluidTankBase<T extends MantleBlockEntity> extends FluidTank {

    protected T parent;

    public FluidTankBase(int capacity, T parent) {
        super(capacity);
        this.parent = parent;
    }

    @Override
    protected void onContentsChanged() {
        if (this.parent instanceof IFluidTankUpdater) {
            ((IFluidTankUpdater) this.parent).onTankContentsChanged();
        }

        this.parent.markDirty();
        World level = this.parent.getWorld();
        if (level != null && !level.isClient) {
            TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(this.parent.getPos(), this.getFluid()), level, this.parent.getPos());
        }
    }
}
