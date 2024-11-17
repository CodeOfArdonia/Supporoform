package slimeknights.mantle.block.entity;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.ItemStackList;

import javax.annotation.Nonnull;

// Updated version of InventoryLogic in Mantle. Also contains a few bugfixes DOES NOT OVERRIDE createMenu
public abstract class InventoryBlockEntity extends NameableBlockEntity implements Inventory, NamedScreenHandlerFactory, Nameable {
    private static final String TAG_INVENTORY_SIZE = "InventorySize";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";

    private DefaultedList<ItemStack> inventory;
    /**
     * If true, the inventory size is saved to NBT, false means you are responsible for serializing it if it changes
     */
    private final boolean saveSizeToNBT;
    protected int stackSizeLimit;
    @Getter
    protected IItemHandlerModifiable itemHandler;
    protected LazyOptional<IItemHandlerModifiable> itemHandlerCap;

    /**
     * @param name Localization String for the inventory title. Can be overridden through setCustomName
     */
    public InventoryBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Text name, boolean saveSizeToNBT, int inventorySize) {
        this(tileEntityTypeIn, pos, state, name, saveSizeToNBT, inventorySize, 64);
    }

    /**
     * @param name Localization String for the inventory title. Can be overridden through setCustomName
     */
    public InventoryBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Text name, boolean saveSizeToNBT, int inventorySize, int maxStackSize) {
        super(tileEntityTypeIn, pos, state, name);
        this.saveSizeToNBT = saveSizeToNBT;
        this.inventory = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
        this.stackSizeLimit = maxStackSize;
        this.itemHandler = new InvWrapper(this);
        this.itemHandlerCap = LazyOptional.of(() -> this.itemHandler);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return this.itemHandlerCap.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemHandlerCap.invalidate();
    }

    /* Inventory management */

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= this.inventory.size()) {
            return ItemStack.EMPTY;
        }

        return this.inventory.get(slot);
    }

    public boolean isStackInSlot(int slot) {
        return !this.getStack(slot).isEmpty();
    }

    /**
     * Same as resize, but does not call markDirty. Used on loading from NBT
     */
    private void resizeInternal(int size) {
        // save effort if the size did not change
        if (size == this.inventory.size()) {
            return;
        }
        ItemStackList newInventory = ItemStackList.withSize(size);

        for (int i = 0; i < size && i < this.inventory.size(); i++) {
            newInventory.set(i, this.inventory.get(i));
        }
        this.inventory = newInventory;
    }

    public void resize(int size) {
        this.resizeInternal(size);
        this.setChangedFast();
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public int getMaxCountPerStack() {
        return this.stackSizeLimit;
    }

    @Override
    public void setStack(int slot, ItemStack itemstack) {
        if (slot < 0 || slot >= this.inventory.size()) {
            return;
        }

        ItemStack current = this.inventory.get(slot);
        this.inventory.set(slot, itemstack);

        if (!itemstack.isEmpty() && itemstack.getCount() > this.getMaxCountPerStack()) {
            itemstack.setCount(this.getMaxCountPerStack());
        }
        if (!ItemStack.areEqual(current, itemstack)) {
            this.setChangedFast();
        }
    }

    @Override
    public ItemStack removeStack(int slot, int quantity) {
        if (quantity <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = this.getStack(slot);

        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // whole itemstack taken out
        if (itemStack.getCount() <= quantity) {
            this.setStack(slot, ItemStack.EMPTY);
            this.setChangedFast();
            return itemStack;
        }

        // split itemstack
        itemStack = itemStack.split(quantity);
        // slot is empty, set to ItemStack.EMPTY
        // isn't this redundant to the above check?
        if (this.getStack(slot).getCount() == 0) {
            this.setStack(slot, ItemStack.EMPTY);
        }

        this.setChangedFast();
        // return remainder
        return itemStack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack itemStack = this.getStack(slot);
        this.setStack(slot, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public boolean isValid(int slot, ItemStack itemstack) {
        if (slot < this.size()) {
            return this.inventory.get(slot).isEmpty() || itemstack.getCount() + this.inventory.get(slot).getCount() <= this.getMaxCountPerStack();
        }
        return false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.inventory.size(); i++) {
            this.inventory.set(i, ItemStack.EMPTY);
        }
    }

    /* Supporting methods */
    @Override
    public boolean canPlayerUse(PlayerEntity entityplayer) {
        // block changed/got broken?
        if (this.world == null || this.world.getBlockEntity(this.pos) != this || this.getCachedState().getBlock() == Blocks.AIR) {
            return false;
        }

        return entityplayer.squaredDistanceTo(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64D;
    }

    @Override
    public void onOpen(PlayerEntity player) {
    }

    @Override
    public void onClose(PlayerEntity player) {
    }

    /* NBT */

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (this.saveSizeToNBT) {
            this.resizeInternal(tags.getInt(TAG_INVENTORY_SIZE));
        }
        this.readInventoryFromNBT(tags);
    }

    @Override
    public void saveSynced(NbtCompound tags) {
        super.saveSynced(tags);
        // only sync the size to the client by default
        if (this.saveSizeToNBT) {
            tags.putInt(TAG_INVENTORY_SIZE, this.inventory.size());
        }
    }

    @Override
    public void writeNbt(NbtCompound tags) {
        super.writeNbt(tags);
        this.writeInventoryToNBT(tags);
    }

    /**
     * Writes the contents of the inventory to the tag
     */
    public void writeInventoryToNBT(NbtCompound tag) {
        Inventory inventory = this;
        NbtList nbttaglist = new NbtList();

        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStack(i).isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte(TAG_SLOT, (byte) i);
                inventory.getStack(i).writeNbt(itemTag);
                nbttaglist.add(itemTag);
            }
        }

        tag.put(TAG_ITEMS, nbttaglist);
    }

    /**
     * Reads an inventory from the tag. Overwrites current content
     */
    public void readInventoryFromNBT(NbtCompound tag) {
        NbtList list = tag.getList(TAG_ITEMS, NbtElement.COMPOUND_TYPE);

        int limit = this.getMaxCountPerStack();
        ItemStack stack;
        for (int i = 0; i < list.size(); ++i) {
            NbtCompound itemTag = list.getCompound(i);
            int slot = itemTag.getByte(TAG_SLOT) & 255;
            if (slot < this.inventory.size()) {
                stack = ItemStack.fromNbt(itemTag);
                if (!stack.isEmpty() && stack.getCount() > limit) {
                    stack.setCount(limit);
                }
                this.inventory.set(slot, stack);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.inventory) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
