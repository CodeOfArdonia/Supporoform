package slimeknights.tconstruct.library.recipe.partbuilder;

import slimeknights.mantle.recipe.container.ISingleStackContainer;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemStack;

/**
 * Inventory interface for the part builder
 */
public interface IPartBuilderContainer extends ISingleStackContainer {
    /**
     * Gets the material recipe based on the current slot contents
     *
     * @return Material recipe, or null if the slot contents are not a valid material
     */
    @Nullable
    IMaterialValue getMaterial();

    /**
     * Gets the stack in the pattern slot
     *
     * @return Pattern slot stack
     */
    ItemStack getPatternStack();

    /*
     * Gets the currently selected pattern item
     * @return
     */
    //ResourceLocation getPattern();

    /**
     * @deprecated use {{@link #getStack()}}
     */
    @Deprecated
    @Override
    default ItemStack getStack(int index) {
        return switch (index) {
            case 0 -> getStack();
            case 1 -> getPatternStack();
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    default boolean isEmpty() {
        return getStack().isEmpty() && getPatternStack().isEmpty();
    }

    /**
     * @deprecated always 2, not useful
     */
    @Deprecated
    @Override
    default int size() {
        return 2;
    }
}
