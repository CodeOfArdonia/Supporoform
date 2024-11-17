package slimeknights.tconstruct.library.recipe.ingredient;

import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;

import org.jetbrains.annotations.Nullable;

/**
 * Ingredient that contains another ingredient nested inside
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class NestedIngredient extends AbstractIngredient {
    protected final Ingredient nested;


    /* Defer to nested */

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return this.nested.test(stack);
    }

    @Override
    public ItemStack[] getItems() {
        return this.nested.getMatchingStacks();
    }

    @Override
    public IntList getStackingIds() {
        return this.nested.getMatchingItemIds();
    }

    @Override
    public boolean isEmpty() {
        return this.nested.isEmpty();
    }

    @Override
    protected void invalidate() {
        super.invalidate();
        this.nested.checkInvalidation();
    }

    @Override
    public boolean isSimple() {
        return this.nested.isSimple();
    }
}
