package slimeknights.tconstruct.library.recipe.casting;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.Arrays;
import java.util.List;

/**
 * Casting recipe that takes a fluid and optional cast and outputs an item.
 */
public class ItemCastingRecipe extends AbstractCastingRecipe implements IDisplayableCastingRecipe {
    /* Shared fields */
    protected static final LoadableField<FluidIngredient, ItemCastingRecipe> FLUID_FIELD = FluidIngredient.LOADABLE.requiredField("fluid", ItemCastingRecipe::getFluid);
    protected static final LoadableField<ItemOutput, ItemCastingRecipe> RESULT_FIELD = ItemOutput.Loadable.REQUIRED_ITEM.requiredField("result", r -> r.result);
    protected static final LoadableField<Integer, ItemCastingRecipe> COOLING_TIME_FIELD = IntLoadable.FROM_ONE.requiredField("cooling_time", ItemCastingRecipe::getCoolingTime);
    /**
     * Loader instance
     */
    public static final RecordLoadable<ItemCastingRecipe> LOADER = RecordLoadable.create(
            LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(),
            LoadableRecipeSerializer.RECIPE_GROUP, CAST_FIELD, FLUID_FIELD, RESULT_FIELD, COOLING_TIME_FIELD, CAST_CONSUMED_FIELD, SWITCH_SLOTS_FIELD,
            ItemCastingRecipe::new);

    @Getter
    private final RecipeSerializer<?> serializer;
    @Getter
    protected final FluidIngredient fluid;
    protected final ItemOutput result;
    @Getter
    protected final int coolingTime;

    public ItemCastingRecipe(TypeAwareRecipeSerializer<?> serializer, Identifier id, String group, Ingredient cast, FluidIngredient fluid, ItemOutput result, int coolingTime, boolean consumed, boolean switchSlots) {
        super(serializer.getType(), id, group, cast, consumed, switchSlots);
        this.serializer = serializer;
        this.fluid = fluid;
        this.result = result;
        this.coolingTime = coolingTime;
    }

    @Override
    public long getFluidAmount(ICastingContainer inv) {
        return this.fluid.getAmount(inv.getFluid());
    }

    @Override
    public boolean matches(ICastingContainer inv, World worldIn) {
        return getCast().test(inv.getStack()) && fluid.test(inv.getFluid());
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return this.result.get();
    }

    @Override
    public int getCoolingTime(ICastingContainer inv) {
        return this.coolingTime;
    }


    /* JEI */

    @Override
    public boolean hasCast() {
        return getCast() != Ingredient.EMPTY;
    }

    @Override
    public List<ItemStack> getCastItems() {
        return Arrays.asList(getCast().getMatchingStacks());
    }

    @Override
    public ItemStack getOutput() {
        return this.result.get();
    }

    /**
     * Gets a list of valid fluid inputs for this recipe, for display in JEI
     *
     * @return List of fluids
     */
    @Override
    public List<FluidStack> getFluids() {
        return this.fluid.getFluids();
    }
}
