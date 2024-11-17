package slimeknights.tconstruct.tools.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.utils.Util;

import java.util.Collection;
import java.util.List;

public class CreativeSlotItem extends Item {
    private static final String NBT_KEY = "slot";
    private static final String TOOLTIP = TConstruct.makeTranslationKey("item", "creative_slot.tooltip");
    private static final Text TOOLTIP_MISSING = TConstruct.makeTranslation("item", "creative_slot.missing").formatted(Formatting.RED);

    public CreativeSlotItem(Settings properties) {
        super(properties);
    }

    /**
     * Gets the value of the slot tag from the given stack
     */
    @Nullable
    public static SlotType getSlot(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_KEY, NbtElement.STRING_TYPE)) {
            return SlotType.getIfPresent(nbt.getString(NBT_KEY));
        }
        return null;
    }

    /**
     * Makes an item stack with the given slot type
     */
    public static ItemStack withSlot(ItemStack stack, SlotType type) {
        stack.getOrCreateNbt().putString(NBT_KEY, type.getName());
        return stack;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        SlotType slot = getSlot(stack);
        String originalKey = this.getTranslationKey();
        if (slot != null) {
            String betterKey = originalKey + "." + slot.getName();
            if (Util.canTranslate(betterKey)) {
                return betterKey;
            }
        }
        return originalKey;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        SlotType slot = getSlot(stack);
        if (slot != null) {
            tooltip.add(Text.translatable(TOOLTIP, slot.getDisplayName()).formatted(Formatting.GRAY));
        } else {
            tooltip.add(TOOLTIP_MISSING);
        }
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (allowedIn(group)) {
            Collection<SlotType> allTypes = SlotType.getAllSlotTypes();
            if (allTypes.isEmpty()) {
                items.add(new ItemStack(this));
            } else {
                for (SlotType type : allTypes) {
                    items.add(withSlot(new ItemStack(this), type));
                }
            }
        }
    }
}
