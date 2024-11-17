package slimeknights.mantle.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TallBlockItem;
import net.minecraft.recipe.RecipeType;
import org.jetbrains.annotations.Nullable;

public class BurnableTallBlockItem extends TallBlockItem {
    private final int burnTime;

    public BurnableTallBlockItem(Block blockIn, Settings builder, int burnTime) {
        super(blockIn, builder);
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return this.burnTime;
    }
}
