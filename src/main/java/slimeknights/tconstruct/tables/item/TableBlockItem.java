package slimeknights.tconstruct.tables.item;

import slimeknights.mantle.item.RetexturedBlockItem;

import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.collection.DefaultedList;

/**
 * Retextured block that conditionally enables show all variants
 */
public class TableBlockItem extends RetexturedBlockItem {
    private final BooleanSupplier showAllCondition;

    public TableBlockItem(Block block, TagKey<Item> textureTag, Properties builder, BooleanSupplier showAllCondition) {
        super(block, textureTag, builder);
        this.showAllCondition = showAllCondition;
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.allowedIn(group)) {
            if (this.showAllCondition.getAsBoolean()) {
                addTagVariants(this.getBlock(), this.textureTag, items, true);
            } else {
                items.add(new ItemStack(this));
            }
        }
    }
}
