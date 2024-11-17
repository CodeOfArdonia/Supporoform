package slimeknights.tconstruct.tables.block.entity.inventory;

import com.google.common.base.Preconditions;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;

/**
 * Extension of {@link RecipeInputInventory} to use instead wrap an existing {@link Inventory}
 */
public class CraftingContainerWrapper extends CraftingInventory {
    private final Inventory crafter;

    public CraftingContainerWrapper(Inventory crafter, int width, int height) {
        //noinspection ConstantConditions
        super(null, width, height);
        Preconditions.checkArgument(crafter.size() == width * height, "Invalid width and height for inventroy size");
        this.crafter = crafter;
    }

    /**
     * Inventory redirection
     */

    @Override
    public ItemStack getStack(int index) {
        return this.crafter.getStack(index);
    }

    @Override
    public int size() {
        return this.crafter.size();
    }

    @Override
    public boolean isEmpty() {
        return this.crafter.isEmpty();
    }

    @Override
    public ItemStack removeStack(int index) {
        return this.crafter.removeStack(index);
    }

    @Override
    public ItemStack removeStack(int index, int count) {
        return this.crafter.removeStack(index, count);
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        this.crafter.setStack(index, stack);
    }

    @Override
    public void markDirty() {
        this.crafter.markDirty();
    }

    @Override
    public void clear() {
        this.crafter.clear();
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher helper) {
        for (int i = 0; i < this.crafter.size(); i++) {
            helper.addUnenchantedInput(this.crafter.getStack(i));
        }
    }
}
