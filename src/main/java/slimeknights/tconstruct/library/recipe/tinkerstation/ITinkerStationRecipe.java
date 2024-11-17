package slimeknights.tconstruct.library.recipe.tinkerstation;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;
import slimeknights.mantle.recipe.ICommonRecipe;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;

/**
 * Main interface for all recipes in the Tinker Station
 */
public interface ITinkerStationRecipe extends ICommonRecipe<ITinkerStationContainer> {
    /**
     * Max number of tools in the tinker station slot, if the stack size is larger than this, only some of the tool is consumed
     */
    int DEFAULT_TOOL_STACK_SIZE = 16;

    /* Recipe data */

    @Override
    default RecipeType<?> getType() {
        return TinkerRecipeTypes.TINKER_STATION.get();
    }

    /**
     * If true, this recipe matches the given inputs, ignoring current tool state
     */
    @Override
    boolean matches(ITinkerStationContainer inv, World world);

    /**
     * Gets the recipe result. Return {@link ItemStack#EMPTY) to represent {@link RecipeResult#PASS}, or a non-empty stack to represent success.
     * For more complex recipes, override {@link #getValidatedResult(ITinkerStationContainer, DynamicRegistryManager) } instead.
     * <p>
     * Do not call this method directly, but it is okay to override it.
     *
     * @return Recipe result, may be empty.
     */
    @Override
    default ItemStack craft(ITinkerStationContainer inv, DynamicRegistryManager manager) {
        return getOutput(manager).copy();
    }

    /**
     * Gets the recipe result, or an object containing an error message if the recipe matches but cannot be applied.
     * TODO 1.20: switch return type to {@code RecipeResult<LazyToolStack>}
     *
     * @return Validated result
     */
    default RecipeResult<ItemStack> getValidatedResult(ITinkerStationContainer inv, DynamicRegistryManager manager) {
        ItemStack result = craft(inv, manager);
        if (result.isEmpty()) {
            return RecipeResult.pass();
        }
        return RecipeResult.success(result);
    }

    /**
     * Gets the number to shrink the tool slot by, perfectly valid for this to be higher than the contained number of tools
     */
    default int shrinkToolSlotBy() {
        return DEFAULT_TOOL_STACK_SIZE;
    }

    /**
     * Updates the input stacks upon crafting this recipe
     *
     * @param result   Result from {@link #assemble(ITinkerStationContainer)}. Generally should not be modified. TODO: switch parameter to LazyToolStack.
     * @param inv      Inventory instance to modify inputs
     * @param isServer If true, this is on the serverside. Use to handle randomness, {@link IMutableTinkerStationContainer#giveItem(ItemStack)} should handle being called serverside only
     */
    default void updateInputs(ItemStack result, IMutableTinkerStationContainer inv, boolean isServer) {
        // shrink all stacks by 1
        for (int index = 0; index < inv.getInputCount(); index++) {
            inv.shrinkInput(index, 1);
        }
    }
}
