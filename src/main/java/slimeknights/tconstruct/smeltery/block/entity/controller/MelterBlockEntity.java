package slimeknights.tconstruct.smeltery.block.entity.controller;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.MelterBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.menu.MelterContainerMenu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class MelterBlockEntity extends NameableBlockEntity implements ITankBlockEntity {

    /**
     * Max capacity for the tank
     */
    private static final int TANK_CAPACITY = FluidValues.INGOT * 12;
    /* tags */
    private static final String TAG_INVENTORY = "inventory";
    /**
     * Name of the GUI
     */
    private static final MutableText NAME = TConstruct.makeTranslation("gui", "melter");

    public static final BlockEntityTicker<MelterBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.tick(level, pos, state);

    /* Tank */
    /**
     * Internal fluid tank output
     */
    @Getter
    protected final FluidTankAnimated tank = new FluidTankAnimated(TANK_CAPACITY, this);
    /**
     * Capability holder for the tank
     */
    private final LazyOptional<IFluidHandler> tankHolder = LazyOptional.of(() -> tank);
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

    /* Heating */
    /**
     * Handles all the melting needs
     */
    @Getter
    private final MeltingModuleInventory meltingInventory = new MeltingModuleInventory(this, tank, Config.COMMON.melterOreRate, 3);
    /**
     * Capability holder for the tank
     */
    private final LazyOptional<IItemHandler> inventoryHolder = LazyOptional.of(() -> meltingInventory);

    /**
     * Fuel handling logic
     */
    @Getter
    private final FuelModule fuelModule = new FuelModule(this, () -> Collections.singletonList(this.worldPosition.below()));

    /**
     * Main constructor
     */
    public MelterBlockEntity(BlockPos pos, BlockState state) {
        this(TinkerSmeltery.melter.get(), pos, state);
    }

    /**
     * Extendable constructor
     */
    @SuppressWarnings("WeakerAccess")
    protected MelterBlockEntity(BlockEntityType<? extends MelterBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NAME);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity playerEntity) {
        return new MelterContainerMenu(id, inv, this);
    }

    /*
     * Tank methods
     */

    @Override
    public @NotNull ModelData getModelData() {
        // TODO: switch to tank property and fluid property
        return ModelData.builder().with(ModelProperties.FLUID_TANK, tank).build();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return tankHolder.cast();
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryHolder.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.tankHolder.invalidate();
        this.inventoryHolder.invalidate();
    }

    /*
     * Melting
     */

    /**
     * Checks if the tile entity is active
     */
    private boolean isFormed() {
        BlockState state = this.getCachedState();
        return state.contains(MelterBlock.IN_STRUCTURE) && state.get(MelterBlock.IN_STRUCTURE);
    }

    /**
     * Ticks the TE on the server
     */
    private void tick(World level, BlockPos pos, BlockState state) {
        // are we fully formed?
        if (isFormed()) {
            switch (this.tick) {
                // tick 0: find fuel
                case 0:
                    if (!this.fuelModule.hasFuel() && this.meltingInventory.canHeat(this.fuelModule.findFuel(false))) {
                        this.fuelModule.findFuel(true);
                    }
                    // tick 2: heat items and consume fuel
                case 2: {
                    boolean hasFuel = this.fuelModule.hasFuel();
                    // update the active state
                    if (state.get(ControllerBlock.ACTIVE) != hasFuel) {
                        level.setBlockState(pos, state.with(ControllerBlock.ACTIVE, hasFuel));
                        // update the heater below
                        BlockPos down = pos.down();
                        BlockState downState = level.getBlockState(down);
                        if (downState.isIn(TinkerTags.Blocks.FUEL_TANKS) && downState.contains(ControllerBlock.ACTIVE) && downState.get(ControllerBlock.ACTIVE) != hasFuel) {
                            level.setBlockState(down, downState.with(ControllerBlock.ACTIVE, hasFuel));
                        }
                    }
                    // heat items
                    if (hasFuel) {
                        this.meltingInventory.heatItems(this.fuelModule.getTemperature());
                        this.fuelModule.decreaseFuel(1);
                    } else {
                        this.meltingInventory.coolItems();
                    }
                }
            }
            this.tick = (this.tick + 1) % 4;
        }
    }


    /*
     * NBT
     */

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.tank.readFromNBT(tag.getCompound(NBTTags.TANK));
        this.fuelModule.readFromTag(tag);
        if (tag.contains(TAG_INVENTORY, NbtElement.COMPOUND_TYPE)) {
            this.meltingInventory.readFromTag(tag.getCompound(TAG_INVENTORY));
        }
    }

    @Override
    public void saveSynced(NbtCompound tag) {
        super.saveSynced(tag);
        tag.put(NBTTags.TANK, this.tank.writeToNBT(new NbtCompound()));
        tag.put(TAG_INVENTORY, this.meltingInventory.writeToTag());
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        this.fuelModule.writeToTag(tag);
    }
}
