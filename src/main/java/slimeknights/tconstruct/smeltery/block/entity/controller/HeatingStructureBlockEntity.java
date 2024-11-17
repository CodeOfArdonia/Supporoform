package slimeknights.tconstruct.smeltery.block.entity.controller;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.SmelteryControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.EntityMeltingModule;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockResult;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;
import slimeknights.tconstruct.smeltery.menu.HeatingStructureContainerMenu;
import slimeknights.tconstruct.smeltery.network.StructureErrorPositionPacket;
import slimeknights.tconstruct.smeltery.network.StructureUpdatePacket;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

public abstract class HeatingStructureBlockEntity extends NameableBlockEntity implements IMasterLogic, ISmelteryTankHandler, IRetexturedBlockEntity {
    private static final String TAG_STRUCTURE = "multiblock";
    private static final String TAG_TANK = "tank";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_ERROR_POS = "lastError";

    /**
     * Ticker instance for the serverside
     */
    public static final BlockEntityTicker<HeatingStructureBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.serverTick(level, pos, state);
    /**
     * Ticker instance for the clientside
     */
    public static final BlockEntityTicker<HeatingStructureBlockEntity> CLIENT_TICKER = (level, pos, state, self) -> self.clientTick(level, pos, state);

    /**
     * Sub module to detect the multiblock for this structure
     */
    private final HeatingStructureMultiblock<?> multiblock = createMultiblock();

    /**
     * Position of the block causing the structure to not form
     */
    @Nullable
    @Getter
    private BlockPos errorPos;
    /**
     * Number of ticks the error will remain visible for
     */
    private int errorVisibleFor = 0;
    /**
     * Temporary hack until forge fixes {@link #onLoad()}, do a first tick listener here as drains don't tick
     */
    private boolean addedDrainListeners = false;

    /* Saved data, written to Tag */
    /**
     * Current structure contents
     */
    @Nullable
    @Getter
    protected StructureData structure;
    /**
     * Tank instance for this smeltery
     */
    @Getter
    protected final SmelteryTank<HeatingStructureBlockEntity> tank = new SmelteryTank<>(this);
    /**
     * Capability to pass to drains for fluid handling
     */
    @Getter
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.empty();

    /**
     * Inventory handling melting items
     */
    @Getter
    protected final MeltingModuleInventory meltingInventory = createMeltingInventory();

    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> meltingInventory);

    /**
     * Fuel module
     */
    @Getter
    protected final FuelModule fuelModule = new FuelModule(this, () -> structure != null ? structure.getTanks() : Collections.emptyList());
    /**
     * Current fuel consumption rate
     */
    protected int fuelRate = 1;


    /**
     * Module handling entity interaction
     */
    protected final EntityMeltingModule entityModule = new EntityMeltingModule(this, tank, this::canMeltEntities, this::insertIntoInventory, () -> structure == null ? null : structure.getBounds());


    /* Instance data, this data is not written to Tag */
    /**
     * Timer to allow delaying actions based on number of ticks alive
     */
    protected int tick = 0;
    /**
     * Updates every second. Once it reaches 10, checks above the smeltery for a layer to see if we can expand up
     */
    private int expandCounter = 0;
    /**
     * If true, structure will check for an update next tick
     */
    private boolean structureUpdateQueued = false;
    /**
     * If true, fluids have changed since the last update and should be synced to the client, synced at most once every 4 ticks
     */
    private boolean fluidUpdateQueued = false;
    /**
     * Cache of the bounds for the case of no structure
     */
    private Box defaultBounds;
    @NotNull
    @Getter
    private Block texture = Blocks.AIR;

    /* Client display */
    private final List<WeakReference<IDisplayFluidListener>> fluidDisplayListeners = new ArrayList<>();

    /* Misc helpers */
    /**
     * Function to drop an item
     */
    protected final Consumer<ItemStack> dropItem = this::dropItem;
    /**
     * Fluid being displayed in the block model
     */
    private FluidStack displayFluid = FluidStack.EMPTY;

    protected HeatingStructureBlockEntity(BlockEntityType<? extends HeatingStructureBlockEntity> type, BlockPos pos, BlockState state, Text name) {
        super(type, pos, state, name);
    }

    /* Abstract methods */

    /**
     * Creates the multiblock for this tile
     */
    protected abstract HeatingStructureMultiblock<?> createMultiblock();

    /**
     * Creates the melting inventory for this structure
     */
    protected abstract MeltingModuleInventory createMeltingInventory();

    /**
     * Called while active to heat the contained items
     */
    protected abstract void heat();


    /* Logic */

    /**
     * Updates the error position and syncs to the client if relevant
     */
    private void updateErrorPos() {
        BlockPos oldErrorPos = this.errorPos;
        this.errorPos = multiblock.getLastResult().getPos();
        if (!Objects.equals(oldErrorPos, errorPos)) {
            TinkerNetwork.getInstance().sendToClientsAround(new StructureErrorPositionPacket(pos, errorPos), world, pos);
        }
    }

    /**
     * Handles the client tick
     */
    protected void clientTick(World level, BlockPos pos, BlockState state) {
        if (errorVisibleFor > 0) {
            errorVisibleFor--;
        }
        if (!addedDrainListeners) {
            addedDrainListeners = true;
            if (structure != null) {
                structure.forEachContained(sPos -> {
                    if (level.getBlockEntity(sPos) instanceof IDisplayFluidListener listener) {
                        fluidDisplayListeners.add(new WeakReference<>(listener));
                    }
                });
                // if we have listeners and a fluid, send a first update
                if (!fluidDisplayListeners.isEmpty()) {
                    FluidStack fluid = IDisplayFluidListener.normalizeFluid(tank.getFluidInTank(0));
                    if (!fluid.isEmpty()) {
                        updateListeners(fluid);
                    }
                }
            }
        }
    }

    /**
     * Handles the server tick
     */
    protected void serverTick(World level, BlockPos pos, BlockState state) {
        if (level.isClient) {
            if (errorVisibleFor > 0) {
                errorVisibleFor--;
            }
            return;
        }
        // invalid state, just a safety check in case its air somehow
        if (!state.contains(ControllerBlock.IN_STRUCTURE)) {
            return;
        }

        // run structure update if requested
        if (structureUpdateQueued) {
            checkStructure();
            structureUpdateQueued = false;
        }

        // if we have a structure, run smeltery logic
        if (structure != null && state.get(SmelteryControllerBlock.IN_STRUCTURE)) {
            // every 15 seconds, check above the smeltery to try to expand
            if (tick == 0) {
                expandCounter++;
                if (expandCounter >= 10 && structure.getInnerY() < multiblock.getMaxHeight()) {
                    expandCounter = 0;
                    // instead of rechecking the whole structure, just recheck the layer above and queue an update if its usable
                    if (multiblock.canExpand(structure, level)) {
                        updateStructure();
                    } else {
                        updateErrorPos();
                    }
                }
            } else if (tick % 4 == 0) {
                // check the next inside position to see if its a valid inner block every other tick
                if (!multiblock.isInnerBlock(level, structure.getNextInsideCheck())) {
                    updateStructure();
                }
            }

            // main heating logic
            heat();

            // fluid update sync every four ticks, whether it has tanks or not
            if (tick % 4 == 3) {
                if (fluidUpdateQueued) {
                    fluidUpdateQueued = false;
                    tank.syncFluids();
                }
            }
        } else if (tick == 0) {
            updateStructure();
        }

        // update tick timer
        tick = (tick + 1) % 20;
    }

    /**
     * Drops an item into the level
     *
     * @param stack Item to drop
     */
    protected void dropItem(ItemStack stack) {
        assert world != null;
        if (!world.isClient && !stack.isEmpty()) {
            double x = (double) (world.random.nextFloat() * 0.5F) + 0.25D;
            double y = (double) (world.random.nextFloat() * 0.5F) + 0.25D;
            double z = (double) (world.random.nextFloat() * 0.5F) + 0.25D;
            BlockPos pos = this.pos.offset(getCachedState().get(ControllerBlock.FACING));
            ItemEntity itementity = new ItemEntity(world, (double) pos.getX() + x, (double) pos.getY() + y, (double) pos.getZ() + z, stack);
            itementity.setToDefaultPickupDelay();
            world.spawnEntity(itementity);
        }
    }


    /* Capability */

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemCapability.invalidate();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(capability, facing);
    }


    /* Structure */

    /**
     * Marks the smeltery for a structure check
     */
    public void updateStructure() {
        structureUpdateQueued = true;
    }

    /**
     * Sets the structure and updates results of the new size, good method to override
     *
     * @param structure New structure
     */
    protected void setStructure(@Nullable StructureData structure) {
        this.structure = structure;
    }

    /**
     * Attempts to locate a valid smeltery structure
     */
    protected void checkStructure() {
        if (world == null || world.isClient) {
            return;
        }
        boolean wasFormed = getCachedState().get(ControllerBlock.IN_STRUCTURE);
        StructureData oldStructure = structure;
        StructureData newStructure = multiblock.detectMultiblock(world, pos, getCachedState().get(Properties.HORIZONTAL_FACING));

        // update block state
        boolean formed = newStructure != null;
        if (formed != wasFormed) {
            world.setBlockState(pos, getCachedState().with(ControllerBlock.IN_STRUCTURE, formed));
        }

        // structure info updates
        if (formed) {
            // sync size to the client
            TinkerNetwork.getInstance().sendToClientsAround(
                    new StructureUpdatePacket(pos, newStructure.getMinPos(), newStructure.getMaxPos(), newStructure.getTanks()), world, pos);

            // update tank capability, do first for update listeners on the drain blocks
            if (!fluidCapability.isPresent()) {
                fluidCapability = LazyOptional.of(() -> tank);
            }

            // set master positions
            newStructure.assignMaster(this, oldStructure);
            setStructure(newStructure);
        } else {
            // remove tank capability
            if (fluidCapability.isPresent()) {
                fluidCapability.invalidate();
                fluidCapability = LazyOptional.empty();
            }

            // clear positions
            if (oldStructure != null) {
                oldStructure.clearMaster(this);
            }
            setStructure(null);
        }

        // update the error position, we do on both success and failure for the sake of expanding positions
        updateErrorPos();

        // clear expand counter either way
        expandCounter = 0;
    }

    /**
     * Called when the controller is broken to invalidate the master in all servants
     */
    public void invalidateStructure() {
        if (structure != null) {
            structure.clearMaster(this);
            structure = null;
            errorPos = null;
        }
    }

    @Override
    public void notifyChange(BlockPos pos, BlockState state) {
        // structure invalid? can ignore this, will automatically check later
        if (structure == null) {
            return;
        }

        assert world != null;
        if (multiblock.shouldUpdate(world, structure, pos, state)) {
            updateStructure();
        }
    }

    /**
     * Gets the last result from this multiblock
     */
    public MultiblockResult getStructureResult() {
        return multiblock.getLastResult();
    }

    /* Tank */

    @Override
    public void updateFluidsFromPacket(List<FluidStack> fluids) {
        tank.setFluids(fluids);
    }

    /**
     * Updates all fluid display listeners
     */
    private void updateListeners(FluidStack fluid) {
        Iterator<WeakReference<IDisplayFluidListener>> iterator = fluidDisplayListeners.iterator();
        while (iterator.hasNext()) {
            IDisplayFluidListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.notifyDisplayFluidUpdated(fluid);
            }
        }
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return RetexturedHelper.getModelDataBuilder(getTexture()).with(IDisplayFluidListener.PROPERTY, displayFluid).build();
    }

    /**
     * Updates the fluid displayed in the block, only used client side
     *
     * @param fluid Fluid
     */
    private void updateDisplayFluid(FluidStack fluid) {
        if (world != null && world.isClient) {
            // update ourself
            this.displayFluid = IDisplayFluidListener.normalizeFluid(fluid);
            this.requestModelDataUpdate();
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, 48);
            updateListeners(displayFluid);
        }
    }

    @Override
    public void addDisplayListener(IDisplayFluidListener listener) {
        boolean have = false;
        for (WeakReference<IDisplayFluidListener> existing : fluidDisplayListeners) {
            if (existing.get() == listener) {
                have = true;
                break;
            }
        }
        if (!have) {
            fluidDisplayListeners.add(new WeakReference<>(listener));
        }
        listener.notifyDisplayFluidUpdated(IDisplayFluidListener.normalizeFluid(tank.getFluidInTank(0)));
    }

    @Override
    public void notifyFluidsChanged(FluidChange type, FluidStack fluid) {
        if (type == FluidChange.ORDER_CHANGED) {
            updateDisplayFluid(fluid);
        } else {
            // mark that fluids need an update on the client
            fluidUpdateQueued = true;
            this.setChangedFast();
        }
    }

    @Override
    public Box getRenderBoundingBox() {
        if (structure != null) {
            return structure.getBounds();
        } else if (defaultBounds == null) {
            defaultBounds = new Box(pos, pos.add(1, 1, 1));
        }
        return defaultBounds;
    }

    /* Heating helpers */

    /**
     * Checks if we can melt entities
     *
     * @return True if we can melt entities
     */
    private boolean canMeltEntities() {
        if (fuelModule.hasFuel()) {
            return true;
        }
        return fuelModule.findFuel(false) > 0;
    }

    /**
     * Inserts an item into the inventory
     *
     * @param stack Stack to insert
     */
    private ItemStack insertIntoInventory(ItemStack stack) {
        return ItemHandlerHelper.insertItem(meltingInventory, stack, false);
    }


    /* UI and sync */

    @Nullable
    @Override
    public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        return new HeatingStructureContainerMenu(id, inv, this);
    }

    /**
     * Sets the structure info on the client side
     *
     * @param minPos Min structure position
     * @param maxPos Max structure position
     */
    public void setStructureSize(BlockPos minPos, BlockPos maxPos, List<BlockPos> tanks) {
        setStructure(multiblock.createClient(minPos, maxPos, tanks));
        fuelModule.clearCachedDisplayListeners();
        if (structure == null) {
            fluidDisplayListeners.clear();
        } else {
            fluidDisplayListeners.removeIf(reference -> {
                IDisplayFluidListener listener = reference.get();
                return listener == null || !structure.contains(listener.getListenerPos());
            });
        }
    }

    /**
     * Updates the error position from the server
     */
    public void setErrorPos(@Nullable BlockPos errorPos) {
        this.errorPos = errorPos;
        if (errorPos != null && this.world != null) {
            // 10 seconds after its set
            this.errorVisibleFor = 200;
        }
    }

    /**
     * If true, the error position should be visible
     */
    public boolean isHighlightError() {
        return errorVisibleFor > 0;
    }

    /**
     * If true, the given item triggers debug blocks
     */
    protected abstract boolean isDebugItem(ItemStack stack);

    /**
     * If true, debug blocks should show in the TESR to the given player
     */
    public boolean showDebugBlockBorder(PlayerEntity player) {
        return isDebugItem(player.getMainHandStack())
                || isDebugItem(player.getOffHandStack())
                || isDebugItem(player.getEquippedStack(EquipmentSlot.HEAD));
    }


    /* Retexturing */

    @Override
    public String getTextureName() {
        return RetexturedHelper.getTextureName(texture);
    }

    @Override
    public void updateTexture(String name) {
        Block oldTexture = texture;
        texture = RetexturedHelper.getBlock(name);
        if (oldTexture != texture) {
            setChangedFast();
            RetexturedHelper.onTextureUpdated(this);
        }
    }



    /* Tag */

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(TAG_TANK, NbtElement.COMPOUND_TYPE)) {
            tank.read(nbt.getCompound(TAG_TANK));
            FluidStack first = tank.getFluidInTank(0);
            if (!first.isEmpty()) {
                updateDisplayFluid(first);
            }
        }
        if (nbt.contains(TAG_INVENTORY, NbtElement.COMPOUND_TYPE)) {
            meltingInventory.readFromTag(nbt.getCompound(TAG_INVENTORY));
        }
        if (nbt.contains(TAG_STRUCTURE, NbtElement.COMPOUND_TYPE)) {
            setStructure(multiblock.readFromTag(nbt.getCompound(TAG_STRUCTURE), this.pos));
            if (structure != null) {
                fluidCapability = LazyOptional.of(() -> tank);
            }
        }
        // only exists to be sent server to client in update packets
        if (nbt.contains(TAG_ERROR_POS, NbtElement.COMPOUND_TYPE)) {
            this.errorPos = NbtHelper.toBlockPos(nbt.getCompound(TAG_ERROR_POS)).add(this.pos);
        }
        fuelModule.readFromTag(nbt);
        if (nbt.contains(TAG_TEXTURE, NbtElement.STRING_TYPE)) {
            texture = RetexturedHelper.getBlock(nbt.getString(TAG_TEXTURE));
            RetexturedHelper.onTextureUpdated(this);
        }
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        // Tag that just writes to disk
        super.writeNbt(compound);
        if (structure != null) {
            compound.put(TAG_STRUCTURE, structure.writeToTag(this.pos));
        }
        fuelModule.writeToTag(compound);
    }

    @Override
    public void saveSynced(NbtCompound compound) {
        // Tag that writes to disk and syncs to client
        super.saveSynced(compound);
        compound.put(TAG_TANK, tank.write(new NbtCompound()));
        compound.put(TAG_INVENTORY, meltingInventory.writeToTag());
        if (texture != Blocks.AIR) {
            compound.putString(TAG_TEXTURE, getTextureName());
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        // Tag that just syncs to client
        NbtCompound nbt = super.toInitialChunkDataNbt();
        if (this.structure != null) {
            nbt.put(TAG_STRUCTURE, this.structure.writeClientTag(this.pos));
        }
        // sync error position, not actually saved in Tag
        if (this.errorPos != null) {
            nbt.put(TAG_ERROR_POS, NbtHelper.fromBlockPos(this.errorPos.subtract(this.pos)));
        }
        return nbt;
    }


    /* Helpers */


    /**
     * Handles the unchecked cast for a block entity ticker
     */
    @Nullable
    public static <HAVE extends HeatingStructureBlockEntity, RET extends BlockEntity> BlockEntityTicker<RET> getTicker(World level, BlockEntityType<RET> expected, BlockEntityType<HAVE> have) {
        return BlockEntityHelper.castTicker(expected, have, level.isClient ? CLIENT_TICKER : SERVER_TICKER);
    }
}
