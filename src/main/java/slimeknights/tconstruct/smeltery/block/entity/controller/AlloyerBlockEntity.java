package slimeknights.tconstruct.smeltery.block.entity.controller;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.MelterBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.MixerAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.SingleAlloyingModule;
import slimeknights.tconstruct.smeltery.menu.AlloyerContainerMenu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * Dedicated alloying block
 */
public class AlloyerBlockEntity extends NameableBlockEntity implements ITankBlockEntity {
    /**
     * Max capacity for the tank
     */
    private static final int TANK_CAPACITY = TankType.INGOT_TANK.getCapacity();
    /**
     * Name of the container
     */
    private static final Text NAME = TConstruct.makeTranslation("gui", "alloyer");

    public static final BlockEntityTicker<AlloyerBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.tick(level, pos, state);

    /**
     * Tank for this mixer
     */
    @Getter
    protected final FluidTankAnimated tank = new FluidTankAnimated(TANK_CAPACITY, this);
    /* Capability for return */
    private final LazyOptional<IFluidHandler> tankHolder = LazyOptional.of(() -> this.tank);

    // modules
    /**
     * Logic for a mixer alloying
     */
    @Getter
    private final MixerAlloyTank alloyTank = new MixerAlloyTank(this, this.tank);
    /**
     * Base alloy logic
     */
    private final SingleAlloyingModule alloyingModule = new SingleAlloyingModule(this, this.alloyTank);
    /**
     * Fuel handling logic
     */
    @Getter
    private final FuelModule fuelModule = new FuelModule(this, () -> Collections.singletonList(this.worldPosition.below()));

    /**
     * Last comparator strength to reduce block updates
     */
    @Getter
    @Setter
    private int lastStrength = -1;

    /**
     * Internal tick counter
     */
    private int tick;

    public AlloyerBlockEntity(BlockPos pos, BlockState state) {
        this(TinkerSmeltery.alloyer.get(), pos, state);
    }

    protected AlloyerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NAME);
    }

    /*
     * Capability
     */

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return this.tankHolder.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.tankHolder.invalidate();
    }


    /*
     * Alloying
     */

    /**
     * Checks if the tile entity is active
     */
    private boolean isFormed() {
        BlockState state = this.getCachedState();
        return state.contains(MelterBlock.IN_STRUCTURE) && state.get(MelterBlock.IN_STRUCTURE);
    }

    /**
     * Handles server tick
     */
    private void tick(World level, BlockPos pos, BlockState state) {
        if (!this.isFormed()) {
            return;
        }

        switch (this.tick) {
            // tick 0: find fuel
            case 0 -> {
                this.alloyTank.setTemperature(this.fuelModule.findFuel(false));
                if (!this.fuelModule.hasFuel() && this.alloyingModule.canAlloy()) {
                    this.fuelModule.findFuel(true);
                }
            }
            // tick 2: alloy alloys and consume fuel
            case 2 -> {
                boolean hasFuel = this.fuelModule.hasFuel();

                // update state for new fuel state
                if (state.get(ControllerBlock.ACTIVE) != hasFuel) {
                    level.setBlockState(pos, state.with(ControllerBlock.ACTIVE, hasFuel));
                    // update the heater below
                    BlockPos down = pos.down();
                    BlockState downState = level.getBlockState(down);
                    if (downState.isIn(TinkerTags.Blocks.FUEL_TANKS) && downState.contains(ControllerBlock.ACTIVE) && downState.get(ControllerBlock.ACTIVE) != hasFuel) {
                        level.setBlockState(down, downState.with(ControllerBlock.ACTIVE, hasFuel));
                    }
                }

                // actual alloying
                if (hasFuel) {
                    this.alloyTank.setTemperature(this.fuelModule.getTemperature());
                    this.alloyingModule.doAlloy();
                    this.fuelModule.decreaseFuel(1);
                }
            }
        }
        this.tick = (this.tick + 1) % 4;
    }

    /**
     * Called when a neighbor of this block is changed to update the tank cache
     *
     * @param side Side changed
     */
    public void neighborChanged(Direction side) {
        this.alloyTank.refresh(side, true);
    }

    /*
     * Display
     */

    @Nullable
    @Override
    public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity playerEntity) {
        return new AlloyerContainerMenu(id, inv, this);
    }


    /*
     * NBT
     */

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public void saveSynced(NbtCompound tag) {
        super.saveSynced(tag);
        tag.put(NBTTags.TANK, this.tank.writeToNBT(new NbtCompound()));
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        this.fuelModule.writeToTag(tag);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.tank.readFromNBT(nbt.getCompound(NBTTags.TANK));
        this.fuelModule.readFromTag(nbt);
    }
}
