package slimeknights.tconstruct.tools.recipe;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.recipe.worktable.AbstractWorktableRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Reorders modifiers ion a tool
 */
public class ModifierSortingRecipe extends AbstractWorktableRecipe {
    private static final Text TITLE = TConstruct.makeTranslation("recipe", "modifier_sorting.title");
    private static final Text DESCRIPTION = TConstruct.makeTranslation("recipe", "modifier_sorting.description");
    private static final Text NOT_ENOUGH_MODIFIERS = TConstruct.makeTranslation("recipe", "modifier_sorting.not_enough_modifiers");

    public static final RecordLoadable<ModifierSortingRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), INPUTS_FIELD, ModifierSortingRecipe::new);

    public ModifierSortingRecipe(Identifier id, List<SizedIngredient> inputs) {
        super(id, inputs);
    }

    @Override
    public boolean matches(ITinkerableContainer inv, World world) {
        if (!inv.getTinkerableStack().isIn(TinkerTags.Items.MODIFIABLE)) {
            return false;
        }
        return ModifierRecipe.checkMatch(inv, this.inputs);
    }

    @Override
    public Text getTitle() {
        return TITLE;
    }

    @Override
    public Text getDescription(@Nullable ITinkerableContainer inv) {
        if (inv != null && inv.getTinkerable().getUpgrades().getModifiers().size() < 2) {
            return NOT_ENOUGH_MODIFIERS;
        }
        return DESCRIPTION;
    }

    @Override
    public RecipeResult<LazyToolStack> getResult(ITinkerableContainer inv, ModifierEntry modifier) {
        ToolStack tool = inv.getTinkerable();

        // find the modifier to remove
        List<ModifierEntry> upgrades = tool.getUpgrades().getModifiers();
        int toMove = IntStream.range(0, upgrades.size())
                .filter(i -> upgrades.get(i).matches(modifier.getId()))
                .findFirst().orElse(-1);
        // if no change, no need to do anything
        if (toMove == -1) {
            return RecipeResult.pass();
        }

        // reorder
        tool = tool.copy();
        List<ModifierEntry> newUpgrades = new ArrayList<>(upgrades);
        Collections.swap(newUpgrades, toMove, toMove == upgrades.size() - 1 ? 0 : toMove + 1);
        tool.setUpgrades(new ModifierNBT(newUpgrades));

        // no need to validate, its the same modifiers
        return RecipeResult.success(LazyToolStack.from(tool, inv.getTinkerableSize()));
    }

    @Override
    public void updateInputs(LazyToolStack result, ITinkerableContainer.Mutable inv, ModifierEntry selected, boolean isServer) {
        // input is not consumed
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.modifierSortingSerializer.get();
    }
}
