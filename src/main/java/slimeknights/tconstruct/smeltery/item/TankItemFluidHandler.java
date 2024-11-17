package slimeknights.tconstruct.smeltery.item;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import javax.annotation.Nonnull;

/**
 * Handler that works with a tank item to adjust its tank in NBT
 */
@RequiredArgsConstructor
public class TankItemFluidHandler implements IFluidHandlerItem, ICapabilityProvider {
    private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);
    @Getter
    private final ItemStack container;

    /**
     * Gets the tank on the stack
     */
    private FluidTank getTank() {
        return TankItem.getFluidTank(this.container);
    }

    /**
     * Updates the container from the given tank
     */
    private void updateContainer(FluidTank tank) {
        TankItem.setTank(this.container, tank);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, this.holder);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.getTank().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return TankBlockEntity.getCapacity(this.container.getItem());
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        FluidTank tank = this.getTank();
        int didFill = tank.fill(resource, action);
        if (didFill > 0 && action.execute()) {
            this.updateContainer(tank);
        }
        return didFill;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidTank tank = this.getTank();
        FluidStack didDrain = tank.drain(resource, action);
        if (!didDrain.isEmpty() && action.execute()) {
            this.updateContainer(tank);
        }
        return didDrain;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidTank tank = this.getTank();
        FluidStack didDrain = tank.drain(maxDrain, action);
        if (!didDrain.isEmpty() && action.execute()) {
            this.updateContainer(tank);
        }
        return didDrain;
    }
}
