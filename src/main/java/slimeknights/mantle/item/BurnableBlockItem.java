package slimeknights.mantle.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import org.jetbrains.annotations.Nullable;

public class BurnableBlockItem extends BlockItem {
    private final int burnTime;

    public BurnableBlockItem(Block blockIn, Settings builder, int burnTime) {
        super(blockIn, builder);
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return this.burnTime;
    }
}
