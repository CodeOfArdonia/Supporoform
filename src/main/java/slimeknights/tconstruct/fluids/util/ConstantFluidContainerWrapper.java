package slimeknights.tconstruct.fluids.util;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a capability handler for a container with a constant fluid
 */
public class ConstantFluidContainerWrapper implements IFluidHandlerItem, ICapabilityProvider {
    private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);

    /**
     * Contained fluid
     */
    private final FluidStack fluid;
    /**
     * If true, the container is now empty
     */
    private boolean empty = false;
    /**
     * Item stack representing the current state
     */
    @Getter
    @Nonnull
    protected ItemStack container;
    /**
     * Empty version of the container
     */
    private final ItemStack emptyStack;

    public ConstantFluidContainerWrapper(FluidStack fluid, ItemStack container, ItemStack emptyStack) {
        this.fluid = fluid;
        this.container = container;
        this.emptyStack = emptyStack;
    }

    public ConstantFluidContainerWrapper(FluidStack fluid, ItemStack container) {
        this(fluid, container, container.getCraftingRemainingItem());
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.fluid.getAmount();
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return stack.isEmpty() || stack.getFluid() == this.fluid.getFluid();
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.empty ? FluidStack.EMPTY : this.fluid;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        // cannot drain if: already drained, requested the wrong type, or requested too little
        if (this.empty || resource.getFluid() != this.fluid.getFluid() || resource.getAmount() < this.fluid.getAmount()) {
            return FluidStack.EMPTY;
        }
        if (action == FluidAction.EXECUTE) {
            this.container = this.emptyStack;
            this.empty = true;
        }
        return this.fluid.copy();
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // cannot drain if: already drained, requested the wrong type, or requested too little
        if (this.empty || maxDrain < this.fluid.getAmount()) {
            return FluidStack.EMPTY;
        }
        if (action == FluidAction.EXECUTE) {
            this.container = this.emptyStack;
            this.empty = true;
        }
        return this.fluid.copy();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(capability, this.holder);
    }
}
