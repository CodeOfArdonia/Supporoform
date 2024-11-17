package slimeknights.tconstruct.library.modifiers.impl;

import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.InventoryModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.INamespacedNBTView;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.function.BiFunction;

/**
 * Modifier that has an inventory
 * TODO: migrate to a modifier module
 */
@RequiredArgsConstructor
public class InventoryModifier extends Modifier implements InventoryModifierHook, VolatileDataModifierHook, ValidateModifierHook, ModifierRemovalHook {
    /**
     * Mod Data NBT mapper to get a compound list
     */
    protected static final BiFunction<NbtCompound, String, NbtList> GET_COMPOUND_LIST = (nbt, name) -> nbt.getList(name, NbtElement.COMPOUND_TYPE);
    /**
     * Error for if the container has items preventing modifier removal
     */
    private static final Text HAS_ITEMS = TConstruct.makeTranslation("modifier", "inventory_cannot_remove");
    /**
     * NBT key to store the slot for a stack
     */
    protected static final String TAG_SLOT = "Slot";

    /**
     * Persistent data key for the inventory storage, if null uses the modifier ID
     */
    @Nullable
    private final Identifier inventoryKey;
    /**
     * Number of slots to add per modifier level
     */
    protected final int slotsPerLevel;

    public InventoryModifier(int slotsPerLevel) {
        this(null, slotsPerLevel);
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ToolInventoryCapability.HOOK, ModifierHooks.VOLATILE_DATA, ModifierHooks.REMOVE);
    }

    /**
     * Gets the inventory key used for NBT serializing
     */
    protected Identifier getInventoryKey() {
        return inventoryKey == null ? getId() : inventoryKey;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        ToolInventoryCapability.addSlots(volatileData, getSlots(volatileData, modifier));
    }

    /**
     * Same as {@link ValidateModifierHook#validate(IToolStackView, ModifierEntry)} but allows passing in a max slots count.
     * Allows the subclass to validate on a different max slots if needed
     *
     * @param tool     Tool to check
     * @param maxSlots Max slots to use in the check
     * @return True if the number of slots is valid
     */
    @Nullable
    protected Text validateForMaxSlots(IToolStackView tool, int maxSlots) {
        IModDataView persistentData = tool.getPersistentData();
        Identifier key = getInventoryKey();
        if (persistentData.contains(key, NbtElement.LIST_TYPE)) {
            NbtList listNBT = persistentData.get(key, GET_COMPOUND_LIST);
            if (!listNBT.isEmpty()) {
                if (maxSlots == 0) {
                    return HAS_ITEMS;
                }
                // first, see whether we have any available slots
                BitSet freeSlots = new BitSet(maxSlots);
                freeSlots.set(0, maxSlots - 1, true);
                for (int i = 0; i < listNBT.size(); i++) {
                    freeSlots.set(listNBT.getCompound(i).getInt(TAG_SLOT), false);
                }
                for (int i = 0; i < listNBT.size(); i++) {
                    NbtCompound compoundNBT = listNBT.getCompound(i);
                    if (compoundNBT.getInt(TAG_SLOT) >= maxSlots) {
                        int free = freeSlots.stream().findFirst().orElse(-1);
                        if (free == -1) {
                            return HAS_ITEMS;
                        } else {
                            freeSlots.set(free, false);
                            compoundNBT.putInt(TAG_SLOT, free);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Text validate(IToolStackView tool, ModifierEntry modifier) {
        return validateForMaxSlots(tool, getSlots(tool, modifier));
    }

    @Nullable
    @Override
    public Text onRemoved(IToolStackView tool, Modifier modifier) {
        Text component = validateForMaxSlots(tool, 0);
        if (component != null) {
            return component;
        }
        tool.getPersistentData().remove(getInventoryKey());
        return null;
    }

    @Override
    public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
        IModDataView modData = tool.getPersistentData();
        Identifier key = getInventoryKey();
        if (slot < getSlots(tool, modifier) && modData.contains(key, NbtElement.LIST_TYPE)) {
            NbtList list = tool.getPersistentData().get(key, GET_COMPOUND_LIST);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound compound = list.getCompound(i);
                if (compound.getInt(TAG_SLOT) == slot) {
                    return ItemStack.fromNbt(compound);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
        if (slot < getSlots(tool, modifier)) {
            NbtList list;
            ModDataNBT modData = tool.getPersistentData();
            // if the tag exists, fetch it
            Identifier key = getInventoryKey();
            if (modData.contains(key, NbtElement.LIST_TYPE)) {
                list = modData.get(key, GET_COMPOUND_LIST);
                // first, try to find an existing stack in the slot
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound compound = list.getCompound(i);
                    if (compound.getInt(TAG_SLOT) == slot) {
                        if (stack.isEmpty()) {
                            list.remove(i);
                        } else {
                            compound.getKeys().clear();
                            stack.writeNbt(compound);
                            compound.putInt(TAG_SLOT, slot);
                        }
                        return;
                    }
                }
            } else if (stack.isEmpty()) {
                // nothing to do if empty
                return;
            } else {
                list = new NbtList();
                modData.put(key, list);
            }

            // list did not contain the slot, so add it
            if (!stack.isEmpty()) {
                list.add(write(stack, slot));
            }
        }
    }

    /**
     * Gets the number of slots for this modifier
     */
    public int getSlots(INamespacedNBTView volatileData, ModifierEntry modifier) {
        return modifier.intEffectiveLevel() * slotsPerLevel;
    }

    @Override
    public final int getSlots(IToolStackView tool, ModifierEntry modifier) {
        return getSlots(tool.getVolatileData(), modifier);
    }

    /**
     * Writes a stack to NBT, including the slot
     */
    protected static NbtCompound write(ItemStack stack, int slot) {
        NbtCompound compound = new NbtCompound();
        stack.writeNbt(compound);
        compound.putInt(TAG_SLOT, slot);
        return compound;
    }
}
