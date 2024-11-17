package slimeknights.mantle.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;

/**
 * Recipe that has an output other than an {@link ItemStack}
 *
 * @param <C> Inventory type
 */
public interface ICustomOutputRecipe<C extends Inventory> extends ICommonRecipe<C> {
    /**
     * @deprecated Item stack output not supported
     */
    @Override
    @Deprecated
    default ItemStack getOutput(DynamicRegistryManager access) {
        return ItemStack.EMPTY;
    }

    /**
     * @deprecated Item stack output not supported
     */
    @Override
    @Deprecated
    default ItemStack craft(C inv, DynamicRegistryManager access) {
        return ItemStack.EMPTY;
    }
}