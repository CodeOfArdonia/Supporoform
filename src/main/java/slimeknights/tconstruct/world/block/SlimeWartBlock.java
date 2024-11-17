package slimeknights.tconstruct.world.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Simple block to hide ichor
 */
public class SlimeWartBlock extends Block {
    private final FoliageType foliageType;

    public SlimeWartBlock(Settings properties, FoliageType foliageType) {
        super(properties);
        this.foliageType = foliageType;
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.foliageType != FoliageType.ICHOR) {
            super.fillItemCategory(group, items);
        }
    }
}
