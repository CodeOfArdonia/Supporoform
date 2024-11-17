package slimeknights.tconstruct.smeltery.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import slimeknights.tconstruct.library.recipe.FluidValues;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

/**
 * Capability handler instance for the copper can item
 */
@AllArgsConstructor
public class CopperCanFluidHandler implements IFluidHandlerItem, ICapabilityProvider {
    private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);

    @Getter
    private final ItemStack container;

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, this.holder);
    }


    /* Tank properties */

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidValues.INGOT;
    }

    /**
     * Gets the contained fluid
     */
    private Fluid getFluid() {
        return CopperCanItem.getFluid(this.container);
    }

    /**
     * Gets the contained fluid
     */
    @Nullable
    private NbtCompound getFluidTag() {
        return CopperCanItem.getFluidTag(this.container);
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return new FluidStack(this.getFluid(), FluidValues.INGOT, this.getFluidTag());
    }


    /* Interaction */

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // must not be filled, must have enough
        if (this.getFluid() != Fluids.EMPTY || resource.getAmount() < FluidValues.INGOT) {
            return 0;
        }
        // update fluid and return
        if (action.execute()) {
            CopperCanItem.setFluid(this.container, resource);
        }
        return FluidValues.INGOT;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        // must be draining at least an ingot
        if (resource.isEmpty() || resource.getAmount() < FluidValues.INGOT) {
            return FluidStack.EMPTY;
        }
        // must have a fluid, must match what they are draining
        Fluid fluid = this.getFluid();
        if (fluid == Fluids.EMPTY || fluid != resource.getFluid()) {
            return FluidStack.EMPTY;
        }
        // output 1 ingot
        FluidStack output = new FluidStack(fluid, FluidValues.INGOT, this.getFluidTag());
        if (action.execute()) {
            CopperCanItem.setFluid(this.container, FluidStack.EMPTY);
        }
        return output;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // must be draining at least an ingot
        if (maxDrain < FluidValues.INGOT) {
            return FluidStack.EMPTY;
        }
        // must have a fluid
        Fluid fluid = this.getFluid();
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        // output 1 ingot
        FluidStack output = new FluidStack(fluid, FluidValues.INGOT, this.getFluidTag());
        if (action.execute()) {
            CopperCanItem.setFluid(this.container, FluidStack.EMPTY);
        }
        return output;
    }
}
