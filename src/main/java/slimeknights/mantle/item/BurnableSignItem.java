package slimeknights.mantle.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SignItem;
import net.minecraft.recipe.RecipeType;
import org.jetbrains.annotations.Nullable;

public class BurnableSignItem extends SignItem {
    private final int burnTime;

    public BurnableSignItem(Settings propertiesIn, Block floorBlockIn, Block wallBlockIn, int burnTime) {
        super(propertiesIn, floorBlockIn, wallBlockIn);
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return this.burnTime;
    }
}
