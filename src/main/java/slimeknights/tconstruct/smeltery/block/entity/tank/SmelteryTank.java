package slimeknights.tconstruct.smeltery.block.entity.tank;

import com.google.common.collect.Lists;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler.FluidChange;
import slimeknights.tconstruct.smeltery.network.SmelteryTankUpdatePacket;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.ListIterator;

/**
 * Fluid handler implementation for the smeltery
 */
public class SmelteryTank<T extends MantleBlockEntity & ISmelteryTankHandler> implements IFluidHandler {
    private final T parent;
    /**
     * Fluids actually contained in the tank
     */
    @Getter
    private final List<FluidStack> fluids;
    /**
     * Maximum capacity of the smeltery
     * -- SETTER --
     *  Updates the maximum tank capacity
     *
     *
     * -- GETTER --
     *  Gets the maximum amount of space in the smeltery tank
     *
     @param maxCapacity New max capacity
      * @return Tank capacity

     */
    @Getter
    @Setter
    private long capacity;
    /**
     * Current amount of fluid in the tank
     */
    @Getter
    private long contained;

    public SmelteryTank(T parent) {
        fluids = Lists.newArrayList();
        capacity = 0;
        contained = 0;
        this.parent = parent;
    }

    /**
     * Called when the fluids change to sync to client
     */
    public void syncFluids() {
        World world = parent.getWorld();
        if (world != null && !world.isClient) {
            BlockPos pos = parent.getPos();
            TinkerNetwork.getInstance().sendToClientsAround(new SmelteryTankUpdatePacket(pos, fluids), world, pos);
        }
    }


    /* Capacity and space */

    /**
     * Gets the amount of empty space in the tank
     *
     * @return Remaining space in the tank
     */
    public int getRemainingSpace() {
        if (contained >= capacity) {
            return 0;
        }
        return capacity - contained;
    }


    /* Fluids */

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int getTanks() {
        if (contained < capacity) {
            return fluids.size() + 1;
        }
        return fluids.size();
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= fluids.size()) {
            return FluidStack.EMPTY;
        }
        return fluids.get(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0) {
            return 0;
        }
        // index of the tank size means the "empty" segment
        int remaining = capacity - contained;
        if (tank == fluids.size()) {
            return remaining;
        }
        // any valid index, return the amount contained and the extra space
        return fluids.get(tank).getAmount() + remaining;
    }

    /**
     * Moves the fluid with the passed index to the beginning/bottom of the fluid tank stack
     *
     * @param index Index to move
     */
    public void moveFluidToBottom(int index) {
        if (index < fluids.size()) {
            FluidStack fluid = fluids.get(index);
            fluids.remove(index);
            fluids.add(0, fluid);
            parent.notifyFluidsChanged(FluidChange.CHANGED, FluidStack.EMPTY);
        }
    }


    /* Filling and draining */

    @Override
    public long fill(FluidStack resource, FluidAction action) {
        // if full or nothing being filled, do nothing
        if (contained >= capacity || resource.isEmpty()) {
            return 0;
        }

        // determine how much we can fill
        long usable = Math.min(capacity - contained, resource.getAmount());
        // could be negative if the smeltery size changes then you try filling it
        if (usable <= 0) {
            return 0;
        }

        // done here if just simulating
        if (action.simulate()) {
            return usable;
        }

        // add contained fluid amount
        contained += usable;

        // check if we already have the given liquid
        for (FluidStack fluid : fluids) {
            if (fluid.isFluidEqual(resource)) {
                // yup. add it
                fluid.grow(usable);
                parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
                return usable;
            }
        }

        // not present yet, add it
        resource = resource.copy();
        resource.setAmount(usable);
        fluids.add(resource);
        parent.notifyFluidsChanged(FluidChange.ADDED, resource);
        return usable;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (fluids.isEmpty()) {
            return FluidStack.EMPTY;
        }

        // simply drain the first one
        FluidStack fluid = fluids.get(0);
        long drainable = Math.min(maxDrain, fluid.getAmount());

        // copy contained fluid to return for accuracy
        FluidStack ret = fluid.copy();
        ret.setAmount(drainable);

        // remove the fluid from the tank
        if (action.execute()) {
            fluid.shrink(drainable);
            contained -= drainable;
            // if now empty, remove from the list
            if (fluid.getAmount() <= 0) {
                fluids.remove(fluid);
                parent.notifyFluidsChanged(FluidChange.REMOVED, fluid);
            } else {
                parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
            }
        }

        // return drained fluid
        return ret;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack toDrain, FluidAction action) {
        // search for the resource
        ListIterator<FluidStack> iter = fluids.listIterator();
        while (iter.hasNext()) {
            FluidStack fluid = iter.next();
            if (fluid.isFluidEqual(toDrain)) {
                // if found, determine how much we can drain
                int drainable = Math.min(toDrain.getAmount(), fluid.getAmount());

                // copy contained fluid to return for accuracy
                FluidStack ret = fluid.copy();
                ret.setAmount(drainable);

                // update tank if executing
                if (action.execute()) {
                    fluid.shrink(drainable);
                    contained -= drainable;
                    // if now empty, remove from the list
                    if (fluid.getAmount() <= 0) {
                        iter.remove();
                        parent.notifyFluidsChanged(FluidChange.REMOVED, fluid);
                    } else {
                        parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
                    }
                }

                return ret;
            }
        }

        // nothing drained
        return FluidStack.EMPTY;
    }

    /* Saving and loading */

    private static final String TAG_FLUIDS = "fluids";
    private static final String TAG_CAPACITY = "capacity";

    /**
     * Updates fluids in the tank, typically from a packet
     *
     * @param fluids List of fluids
     */
    public void setFluids(List<FluidStack> fluids) {
        FluidStack oldFirst = getFluidInTank(0);
        this.fluids.clear();
        this.fluids.addAll(fluids);
        contained = fluids.stream().mapToLong(FluidStack::getAmount).reduce(0, Long::sum);
        FluidStack newFirst = getFluidInTank(0);
        if (!oldFirst.isFluidEqual(newFirst)) {
            this.parent.notifyFluidsChanged(FluidChange.ORDER_CHANGED, newFirst);
        }
    }

    /**
     * Writes the tank to NBT
     */
    public NbtCompound write(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (FluidStack liquid : this.fluids) {
            NbtCompound fluidTag = new NbtCompound();
            liquid.writeToNBT(fluidTag);
            list.add(fluidTag);
        }
        nbt.put(TAG_FLUIDS, list);
        nbt.putInt(TAG_CAPACITY, this.capacity);
        return nbt;
    }

    /**
     * Reads the tank from NBT
     */
    public void read(NbtCompound tag) {
        NbtList list = tag.getList(TAG_FLUIDS, NbtElement.COMPOUND_TYPE);
        this.fluids.clear();
        this.contained = 0;
        for (int i = 0; i < list.size(); i++) {
            NbtCompound fluidTag = list.getCompound(i);
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(fluidTag);
            if (!fluid.isEmpty()) {
                this.fluids.add(fluid);
                this.contained += fluid.getAmount();
            }
        }
        this.capacity = tag.getInt(TAG_CAPACITY);
    }
}
