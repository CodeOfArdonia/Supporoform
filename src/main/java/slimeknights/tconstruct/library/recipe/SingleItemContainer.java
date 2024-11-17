package slimeknights.tconstruct.library.recipe;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

/**
 * Simple class for an inventory containing just one item
 */
@Getter
public class SingleItemContainer implements ISingleStackContainer {
    @Setter
    private ItemStack stack = ItemStack.EMPTY;
}
