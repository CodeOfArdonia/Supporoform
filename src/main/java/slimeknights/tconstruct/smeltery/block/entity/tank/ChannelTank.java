package slimeknights.tconstruct.smeltery.block.entity.tank;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.nbt.NbtCompound;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

import javax.annotation.Nonnull;

/**
 * Tank for channel contents
 */
public class ChannelTank extends FluidTank {
    private static final String TAG_LOCKED = "locked";

    /**
     * Amount of fluid that may not be extracted this tick
     * Essentially, since we cannot guarantee tick order, this prevents us from having a net 0 fluid for the renderer
     * if draining and filling at the same time
     */
    private int locked;

    /**
     * Tank owner
     */
    private final ChannelBlockEntity parent;

    public ChannelTank(int capacity, ChannelBlockEntity parent) {
        super(capacity, fluid -> !fluid.getFluid().getFluidType().isLighterThanAir());
        this.parent = parent;
    }

    /**
     * Called on channel update to clear the lock, allowing this fluid to be drained
     */
    public void freeFluid() {
        this.locked = 0;
    }

    /**
     * Returns the maximum fluid that can be extracted from this tank
     *
     * @return Max fluid that can be pulled
     */
    public int getMaxUsable() {
        return Math.max(fluid.getAmount() - this.locked, 0);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        boolean wasEmpty = isEmpty();
        int amount = super.fill(resource, action);
        if (action.execute()) {
            this.locked += amount;
            // if we added something, sync to client
            if (wasEmpty && !isEmpty()) {
                this.parent.sendFluidUpdate();
            }
        }
        return amount;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        boolean wasEmpty = isEmpty();
        FluidStack stack = super.drain(maxDrain, action);
        // if we removed something, sync to client
        if (action.execute() && !wasEmpty && isEmpty()) {
            this.parent.sendFluidUpdate();
        }
        return stack;
    }

    @Override
    public FluidTank readFromNBT(NbtCompound nbt) {
        this.locked = nbt.getInt(TAG_LOCKED);
        super.readFromNBT(nbt);
        return this;
    }

    @Override
    public NbtCompound writeToNBT(NbtCompound nbt) {
        nbt = super.writeToNBT(nbt);
        nbt.putInt(TAG_LOCKED, this.locked);
        return nbt;
    }
}
