package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.inventory.HeaterItemHandler;
import slimeknights.tconstruct.smeltery.menu.SingleItemContainerMenu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

/**
 * Tile entity for the heater block below the melter
 */
public class HeaterBlockEntity extends NameableBlockEntity {
    private static final String TAG_ITEM = "item";
    private static final Text TITLE = TConstruct.makeTranslation("gui", "heater");

    private final HeaterItemHandler itemHandler = new HeaterItemHandler(this);
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> this.itemHandler);

    protected HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TITLE);
    }

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        this(TinkerSmeltery.heater.get(), pos, state);
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
            return this.itemCapability.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemCapability.invalidate();
    }


    /* NBT */

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (tags.contains(TAG_ITEM, NbtElement.COMPOUND_TYPE)) {
            this.itemHandler.readFromNBT(tags.getCompound(TAG_ITEM));
        }
    }

    @Override
    public void writeNbt(NbtCompound tags) {
        super.writeNbt(tags);
        tags.put(TAG_ITEM, this.itemHandler.writeToNBT());
    }
}
