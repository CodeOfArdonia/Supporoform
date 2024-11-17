package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.AllArgsConstructor;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

@AllArgsConstructor
public class DuctTankWrapper implements IFluidHandler {
    private final IFluidHandler parent;
    private final DuctItemHandler itemHandler;


    /* Properties */

    @Override
    public int getTanks() {
        return this.parent.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.parent.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.parent.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return this.itemHandler.getFluid().isFluidEqual(stack);
    }


    /* Interactions */

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !this.itemHandler.getFluid().isFluidEqual(resource)) {
            return 0;
        }
        return this.parent.fill(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack fluid = this.itemHandler.getFluid();
        if (fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return this.parent.drain(new FluidStack(fluid, maxDrain), action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !this.itemHandler.getFluid().isFluidEqual(resource)) {
            return FluidStack.EMPTY;
        }
        return this.parent.drain(resource, action);
    }
}
