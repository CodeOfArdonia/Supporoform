package slimeknights.tconstruct.tables.block.entity.chest;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
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
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

/**
 * Shared base logic for all Tinkers' chest tile entities
 */
public abstract class AbstractChestBlockEntity extends NameableBlockEntity {
    private static final String KEY_ITEMS = "Items";

    @Getter
    private final IChestItemHandler itemHandler;
    private final LazyOptional<IItemHandler> capability;

    protected AbstractChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Text name, IChestItemHandler itemHandler) {
        super(type, pos, state, name);
        itemHandler.setParent(this);
        this.itemHandler = itemHandler;
        this.capability = LazyOptional.of(() -> itemHandler);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.capability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.capability.invalidate();
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int menuId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new TinkerChestContainerMenu(menuId, playerInventory, this);
    }

    /**
     * Checks if the given item should be inserted into the chest on interact
     *
     * @param player   Player inserting
     * @param heldItem Stack to insert
     * @return Return true
     */
    public boolean canInsert(PlayerEntity player, ItemStack heldItem) {
        return true;
    }

    @Override
    public void writeNbt(NbtCompound tags) {
        super.writeNbt(tags);
        // move the items from the serialized result
        // we don't care about the size and need it here for compat with old worlds
        NbtCompound handlerNBT = this.itemHandler.serializeNBT();
        tags.put(KEY_ITEMS, handlerNBT.getList(KEY_ITEMS, NbtElement.COMPOUND_TYPE));
    }

    /**
     * Reads the inventory from NBT
     */
    public void readInventory(NbtCompound tags) {
        // copy in just the items key for deserializing, don't want to change the size
        NbtCompound handlerNBT = new NbtCompound();
        handlerNBT.put(KEY_ITEMS, tags.getList(KEY_ITEMS, NbtElement.COMPOUND_TYPE));
        this.itemHandler.deserializeNBT(handlerNBT);
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        this.readInventory(tags);
    }
}
