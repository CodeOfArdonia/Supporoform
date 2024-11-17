package slimeknights.tconstruct.tools.modifiers.upgrades.armor;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.impl.InventoryModifier;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public class ItemFrameModifier extends InventoryModifier {
    /**
     * Pattern and inventory key
     */
    private static final Pattern ITEM_FRAME = new Pattern(TConstruct.MOD_ID, "item_frame");

    public ItemFrameModifier() {
        super(ITEM_FRAME, 1);
    }

    @Override
    public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
        return 1;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
        return hasStack ? null : ITEM_FRAME;
    }

    /**
     * Parses all stacks in NBT into the passed list
     */
    public void getAllStacks(IToolStackView tool, ModifierEntry entry, List<ItemStack> stackList) {
        IModDataView modData = tool.getPersistentData();
        if (modData.contains(ITEM_FRAME, NbtElement.LIST_TYPE)) {
            NbtList list = tool.getPersistentData().get(ITEM_FRAME, GET_COMPOUND_LIST);
            int max = this.getSlots(tool, entry);

            // make sure the stacks are in order, NBT could store them in any order
            ItemStack[] parsed = new ItemStack[max];
            for (int i = 0; i < list.size(); i++) {
                NbtCompound compound = list.getCompound(i);
                int slot = compound.getInt(TAG_SLOT);
                if (slot < max) {
                    parsed[slot] = ItemStack.fromNbt(compound);
                }
            }
            // add stacks into the list
            for (ItemStack stack : parsed) {
                if (stack != null && !stack.isEmpty()) {
                    stackList.add(stack);
                }
            }
        }
    }
}
