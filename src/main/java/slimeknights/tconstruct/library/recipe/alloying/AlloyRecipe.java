package slimeknights.tconstruct.library.recipe.alloying;

import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for alloying recipes
 */
public class AlloyRecipe implements ICustomOutputRecipe<IAlloyTank> {
    public static final RecordLoadable<AlloyRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            FluidIngredient.LOADABLE.list(2).requiredField("inputs", r -> r.inputs),
            FluidStackLoadable.REQUIRED_STACK.requiredField("result", r -> r.output),
            IntLoadable.FROM_ONE.requiredField("temperature", r -> r.temperature),
            AlloyRecipe::new).comapFlatMap((recipe, error) -> {
        for (FluidIngredient input : recipe.inputs) {
            if (input.test(recipe.output)) {
                throw new JsonSyntaxException("Result fluid contained in input in alloy recipe " + recipe.id);
            }
        }
        return recipe;
    }, Function.identity());

    @Getter
    private final Identifier id;
    /**
     * List of input ingredients.
     * Order matters, as if a fluid matches multiple ingredients it may produce unexpected behavior.
     * Making the most strict first will produce the best behavior
     */
    private final List<FluidIngredient> inputs;
    /**
     * Recipe output
     */
    @Getter
    private final FluidStack output;
    /**
     * Required temperature to craft this
     */
    @Getter
    private final int temperature;
    /**
     * Cache of recipe input list
     */
    private List<List<FluidStack>> displayInputs;

    public AlloyRecipe(Identifier id, List<FluidIngredient> inputs, FluidStack output, int temperature) {
        this.id = id;
        this.inputs = inputs;
        this.output = output;
        this.temperature = temperature;
    }

    /**
     * Gets the list of inputs for display in JEI
     *
     * @return List of input list for each "slot"
     */
    public List<List<FluidStack>> getDisplayInputs() {
        if (this.displayInputs == null) {
            this.displayInputs = this.inputs.stream().map(FluidIngredient::getFluids).collect(Collectors.toList());
        }
        return this.displayInputs;
    }

    /**
     * Creates the bitset used for marking fluids we do not care about
     *
     * @param inv Alloy tank
     * @return Bitset
     */
    private static BitSet makeBitset(IAlloyTank inv) {
        int tanks = inv.getTanks();
        BitSet used = new BitSet(tanks);
        // mark empty as used to save a bit of effort
        for (int i = 0; i < tanks; i++) {
            if (inv.getFluidInTank(i).isEmpty()) {
                used.set(i);
            }
        }
        return used;
    }

    /**
     * Finds a match for the given ingredient
     *
     * @param ingredient Ingredient to check
     * @param inv        Alloy tank to search
     * @param used       Bitset for already used matches, will be modified
     * @return Index of found match, or -1 if match not found
     */
    private static int findMatch(FluidIngredient ingredient, IAlloyTank inv, BitSet used, boolean checkSize) {
        FluidStack fluid;
        for (int i = 0; i < inv.getTanks(); i++) {
            // must not have used that fluid yet
            if (!used.get(i)) {
                fluid = inv.getFluidInTank(i);
                if (checkSize ? ingredient.test(fluid) : ingredient.test(fluid.getFluid())) {
                    used.set(i);
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean matches(IAlloyTank inv, World worldIn) {
        BitSet used = makeBitset(inv);
        for (FluidIngredient ingredient : this.inputs) {
            // do not care about size for matches, just want a recipe with the right fluids
            int index = findMatch(ingredient, inv, used, false);
            if (index == -1) {
                return false;
            }
        }

        // goal of matches is to see if this works for any of those fluids, so ignore current space
        return true;
    }

    /**
     * Checks if this recipe can be performed.
     * Note that {@link #performRecipe(IMutableAlloyTank)} runs similar logic, so calling both is uneccessary.
     *
     * @param inv Alloy tank inventory
     * @return True if this recipe can be performed
     */
    public boolean canPerform(IAlloyTank inv) {
        // skip if temperature is too low
        if (inv.getTemperature() < this.temperature) return false;

        // bit corresponding to fluids that are already used
        BitSet used = makeBitset(inv);
        int drainAmount = 0;
        FluidStack fluid;
        for (FluidIngredient ingredient : this.inputs) {
            // care about size, if too small just skip the recipe
            int index = findMatch(ingredient, inv, used, true);
            if (index != -1) {
                fluid = inv.getFluidInTank(index);
                drainAmount += ingredient.getAmount(fluid.getFluid());
            } else {
                // no fluid matched this ingredient, match failed
                return false;
            }
        }

        // ensure there is space for the recipe
        return inv.canFit(this.output, drainAmount);
    }

    /**
     * Attempts to perform the recipe. Will do nothing if either there is not enough input, or if there is not enough space for the output
     *
     * @param inv Fluid inventory that can be read and modified
     */
    public void performRecipe(IMutableAlloyTank inv) {
        // skip if temperature is too low
        if (inv.getTemperature() < this.temperature) return;

        // figure out how much fluid we need to remove
        FluidStack[] drainFluids = new FluidStack[inv.getTanks()];
        long drainAmount = 0;

        // bit corresponding to fluids that are already used
        BitSet used = makeBitset(inv);

        FluidStack fluid;
        for (FluidIngredient ingredient : this.inputs) {
            // care about size, if too small just skip the recipe
            int index = findMatch(ingredient, inv, used, true);
            if (index != -1 && drainFluids[index] == null) {
                fluid = inv.getFluidInTank(index);
                long amount = ingredient.getAmount(fluid.getFluid());
                drainAmount += amount;
                drainFluids[index] = new FluidStack(fluid, amount);
            } else {
                // no fluid matched this ingredient, match failed
                return;
            }
        }

        // ensure there is space for the recipe
        FluidStack drained;
        if (inv.canFit(this.output, drainAmount)) {
            // drain each marked fluid
            for (int i = 0; i < drainFluids.length; i++) {
                FluidStack toDrain = drainFluids[i];
                if (toDrain != null) {
                    drained = inv.drain(i, toDrain);
                    // ensure the right amount of fluid was drained and skip to next ingredient
                    if (drained.getAmount() != toDrain.getAmount()) {
                        TConstruct.LOG.error("Wrong amount of fluid {} drained for recipe {}", drained.getFluid(), this.id);
                    }
                }
            }

            // add the output
            int filled = inv.fill(this.output.copy());
            if (filled != this.output.getAmount()) {
                TConstruct.LOG.error("Filled only {} for recipe {}", filled, this.id);
            }
        }
    }

    @Override
    public RecipeType<?> getType() {
        return TinkerRecipeTypes.ALLOYING.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerSmeltery.alloyingSerializer.get();
    }
}