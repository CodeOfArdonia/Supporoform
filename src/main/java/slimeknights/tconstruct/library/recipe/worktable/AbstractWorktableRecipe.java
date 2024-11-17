package slimeknights.tconstruct.library.recipe.worktable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of modifier worktable recipes, taking a list of inputs
 */
@RequiredArgsConstructor
public abstract class AbstractWorktableRecipe implements IModifierWorktableRecipe {
    public static final Ingredient DEFAULT_TOOLS = Ingredient.fromTag(TinkerTags.Items.MODIFIABLE);
    protected static final LoadableField<Ingredient, AbstractWorktableRecipe> TOOL_FIELD = IngredientLoadable.DISALLOW_EMPTY.defaultField("tools", DEFAULT_TOOLS, true, r -> r.toolRequirement);
    protected static final LoadableField<List<SizedIngredient>, AbstractWorktableRecipe> INPUTS_FIELD = SizedIngredient.LOADABLE.list(1).requiredField("inputs", r -> r.inputs);

    @Getter
    private final Identifier id;
    protected final Ingredient toolRequirement;
    protected final List<SizedIngredient> inputs;

    /* JEI */
    @Nullable
    protected List<ItemStack> tools;

    public AbstractWorktableRecipe(Identifier id, List<SizedIngredient> inputs) {
        this(id, Ingredient.fromTag(TinkerTags.Items.MODIFIABLE), inputs);
    }

    @Override
    public boolean matches(ITinkerableContainer inv, World world) {
        if (!this.toolRequirement.test(inv.getTinkerableStack())) {
            return false;
        }
        return ModifierRecipe.checkMatch(inv, this.inputs);
    }

    @Override
    public List<ModifierEntry> getModifierOptions(@Nullable ITinkerableContainer inv) {
        if (inv == null) {
            return ModifierRecipeLookup.getRecipeModifierList();
        }
        return inv.getTinkerable().getUpgrades().getModifiers();
    }

    @Override
    public void updateInputs(LazyToolStack result, ITinkerableContainer.Mutable inv, ModifierEntry selected, boolean isServer) {
        ModifierRecipe.updateInputs(inv, this.inputs);
    }


    /* JEI */

    /**
     * Gets a list of tools to display
     */
    @Override
    public List<ItemStack> getInputTools() {
        if (this.tools == null) {
            this.tools = Arrays.stream(this.toolRequirement.getMatchingStacks()).map(stack -> IModifiableDisplay.getDisplayStack(stack.getItem())).toList();
        }
        return this.tools;
    }

    @Override
    public List<ItemStack> getDisplayItems(int slot) {
        if (slot < 0 || slot >= this.inputs.size()) {
            return Collections.emptyList();
        }
        return this.inputs.get(slot).getMatchingStacks();
    }

    @Override
    public int getInputCount() {
        return this.inputs.size();
    }
}
