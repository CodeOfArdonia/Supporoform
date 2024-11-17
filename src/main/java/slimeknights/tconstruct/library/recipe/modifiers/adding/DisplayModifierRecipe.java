package slimeknights.tconstruct.library.recipe.modifiers.adding;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Recipe instance to return in JEI from recipes that contain multiple display recipes
 */
@RequiredArgsConstructor
public class DisplayModifierRecipe implements IDisplayModifierRecipe {
    private final List<SizedIngredient> inputs;
    @Getter
    private final List<ItemStack> toolWithoutModifier;
    @Getter
    private final List<ItemStack> toolWithModifier;
    /**
     * Error message to display if the requirements do not match
     */
    @Getter
    private final ModifierEntry displayResult;
    @Getter
    private final IntRange level;
    @Nullable
    @Getter
    private final SlotCount slots;

    @Override
    public int getInputCount() {
        return this.inputs.size();
    }

    @Override
    public List<ItemStack> getDisplayItems(int slot) {
        if (slot >= 0 && slot < this.inputs.size()) {
            return this.inputs.get(slot).getMatchingStacks();
        }
        return Collections.emptyList();
    }
}
