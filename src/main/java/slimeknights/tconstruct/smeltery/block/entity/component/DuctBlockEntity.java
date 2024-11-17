package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.SmelteryFluidIO;
import slimeknights.tconstruct.smeltery.block.entity.inventory.DuctItemHandler;
import slimeknights.tconstruct.smeltery.block.entity.inventory.DuctTankWrapper;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;
import slimeknights.tconstruct.smeltery.menu.SingleItemContainerMenu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

/**
 * Filtered drain tile entity
 */
public class DuctBlockEntity extends SmelteryFluidIO implements NamedScreenHandlerFactory {
    private static final String TAG_ITEM = "item";
    private static final Text TITLE = TConstruct.makeTranslation("gui", "duct");

    @Getter
    private final DuctItemHandler itemHandler = new DuctItemHandler(this);
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> itemHandler);

    public DuctBlockEntity(BlockPos pos, BlockState state) {
        this(TinkerSmeltery.duct.get(), pos, state);
    }

    protected DuctBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    /* Container */

    @Override
    public Text getDisplayName() {
        return TITLE;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int id, PlayerInventory inventory, PlayerEntity playerEntity) {
        return new SingleItemContainerMenu(id, inventory, this);
    }


    /* Capability */

    @Nonnull
    @Override
    public <C> LazyOptional<C> getCapability(Capability<C> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    protected LazyOptional<IFluidHandler> makeWrapper(LazyOptional<IFluidHandler> capability) {
        return LazyOptional.of(() -> new DuctTankWrapper(capability.orElse(emptyInstance), itemHandler));
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return RetexturedHelper.getModelDataBuilder(getTexture()).with(IDisplayFluidListener.PROPERTY, IDisplayFluidListener.normalizeFluid(itemHandler.getFluid())).build();
    }

    /**
     * Updates the fluid in model data
     */
    public void updateFluid() {
        requestModelDataUpdate();
        assert world != null;
        BlockState state = getCachedState();
        world.updateListeners(pos, state, state, 48);
    }


    /* NBT */

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (tags.contains(TAG_ITEM, NbtElement.COMPOUND_TYPE)) {
            itemHandler.readFromNBT(tags.getCompound(TAG_ITEM));
        }
    }

    @Override
    public void handleUpdateTag(NbtCompound tag) {
        super.handleUpdateTag(tag);
        if (world != null && world.isClient) {
            updateFluid();
        }
    }

    @Override
    public void saveSynced(NbtCompound tags) {
        super.saveSynced(tags);
        tags.put(TAG_ITEM, this.itemHandler.writeToNBT());
    }
}
