package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

/**
 * Smeltery implementation of the alloy tank, basically just a wrapper around the smeltery tank
 */
@RequiredArgsConstructor
public class SmelteryAlloyTank implements IMutableAlloyTank {
    /**
     * Handler parent
     */
    private final SmelteryTank handler;
    /**
     * Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature
     */
    @Getter
    @Setter
    private int temperature = 0;

    @Override
    public int getTanks() {
        return this.handler.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.handler.getFluidInTank(tank);
    }

    @Override
    public boolean canFit(FluidStack fluid, long removed) {
        // the fluid fits if the net gain in fluid fits in the empty space
        return (fluid.getAmount() - removed) <= this.handler.getRemainingSpace();
    }

    @Override
    public FluidStack drain(int tank, FluidStack fluidStack) {
        return this.handler.drain(fluidStack, FluidAction.EXECUTE);
    }

    @Override
    public int fill(FluidStack fluidStack) {
        return this.handler.fill(fluidStack, FluidAction.EXECUTE);
    }
}
