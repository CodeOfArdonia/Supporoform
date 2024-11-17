package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

public class TankBlockEntity extends SmelteryComponentBlockEntity implements ITankBlockEntity {
    /**
     * Max capacity for the tank
     */
    public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 4;

    /**
     * Gets the capacity for the given block
     *
     * @param block block
     * @return Capacity
     */
    public static int getCapacity(Block block) {
        if (block instanceof ITankBlock) {
            return ((ITankBlock) block).getCapacity();
        }
        return DEFAULT_CAPACITY;
    }

    /**
     * Gets the capacity for the given item
     *
     * @param item item
     * @return Capacity
     */
    public static int getCapacity(Item item) {
        if (item instanceof BlockItem) {
            return getCapacity(((BlockItem) item).getBlock());
        }
        return DEFAULT_CAPACITY;
    }

    /**
     * Internal fluid tank instance
     */
    @Getter
    protected final FluidTankAnimated tank;
    /**
     * Capability holder for the tank
     */
    private final LazyOptional<IFluidHandler> holder;
    /**
     * Last comparator strength to reduce block updates
     */
    @Getter
    @Setter
    private int lastStrength = -1;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof ITankBlock tank
                ? tank
                : TinkerSmeltery.searedTank.get(TankType.FUEL_TANK));
    }

    /**
     * Main constructor
     */
    public TankBlockEntity(BlockPos pos, BlockState state, ITankBlock block) {
        this(TinkerSmeltery.tank.get(), pos, state, block);
    }

    /**
     * Extendable constructor
     */
    @SuppressWarnings("WeakerAccess")
    protected TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ITankBlock block) {
        super(type, pos, state);
        tank = new FluidTankAnimated(block.getCapacity(), this);
        holder = LazyOptional.of(() -> tank);
    }


    /*
     * Tank methods
     */

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return holder.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        holder.invalidate();
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        // TODO: switch to tank property and fluid property
        return ModelData.builder().with(ModelProperties.FLUID_TANK, tank).build();
    }

    @Override
    public void onTankContentsChanged() {
        ITankBlockEntity.super.onTankContentsChanged();
        if (this.world != null) {
            world.getLightingProvider().checkBlock(this.pos);
            this.requestModelDataUpdate();
        }
    }

    @Override
    public void updateFluidTo(FluidStack fluid) {
        ITankBlockEntity.super.updateFluidTo(fluid);
        // update light if the fluid changes
        if (this.world != null) {
            world.getLightingProvider().checkBlock(this.pos);
        }
    }


    /*
     * NBT
     */

    /**
     * Sets the tag on the stack based on the contained tank
     *
     * @param stack Stack
     */
    public void setTankTag(ItemStack stack) {
        TankItem.setTank(stack, tank);
    }

    /**
     * Updates the tank from an NBT tag, used in the block
     *
     * @param nbt tank NBT
     */
    public void updateTank(NbtCompound nbt) {
        if (nbt.isEmpty()) {
            tank.setFluid(FluidStack.EMPTY);
        } else {
            tank.readFromNBT(nbt);
            if (world != null) {
                world.getLightingProvider().checkBlock(pos);
            }
        }
    }

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        tank.setCapacity(getCapacity(getCachedState().getBlock()));
        updateTank(tag.getCompound(NBTTags.TANK));
        super.readNbt(tag);
    }

    @Override
    public void saveSynced(NbtCompound tag) {
        super.saveSynced(tag);
        // want tank on the client on world load
        if (!this.tank.isEmpty()) {
            tag.put(NBTTags.TANK, this.tank.writeToNBT(new NbtCompound()));
        }
    }

    /**
     * Interface for blocks to return their capacity
     */
    public interface ITankBlock {
        /**
         * Gets the capacity for this tank
         */
        int getCapacity();
    }
}
