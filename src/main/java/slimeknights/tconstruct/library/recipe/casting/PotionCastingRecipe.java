package slimeknights.tconstruct.library.recipe.casting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.List;

/**
 * Recipe for casting a fluid onto an item, copying the fluid NBT to the item
 */
@RequiredArgsConstructor
public class PotionCastingRecipe implements ICastingRecipe, IMultiRecipe<DisplayCastingRecipe> {
    public static final RecordLoadable<PotionCastingRecipe> LOADER = RecordLoadable.create(
            LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
            IngredientLoadable.DISALLOW_EMPTY.requiredField("bottle", r -> r.bottle),
            FluidIngredient.LOADABLE.requiredField("fluid", r -> r.fluid),
            Loadables.ITEM.requiredField("result", r -> r.result),
            IntLoadable.FROM_ONE.defaultField("cooling_time", 5, r -> r.coolingTime),
            PotionCastingRecipe::new);

    @Getter
    private final TypeAwareRecipeSerializer<?> serializer;
    @Getter
    private final Identifier id;
    @Getter
    private final String group;
    /**
     * Input on the casting table, always consumed
     */
    private final Ingredient bottle;
    /**
     * Potion ingredient, typically just the potion tag
     */
    private final FluidIngredient fluid;
    /**
     * Potion item result, will be given the proper NBT
     */
    private final Item result;
    /**
     * Cooling time for this recipe, used for tipped arrows
     */
    private final int coolingTime;

    @Override
    public RecipeType<?> getType() {
        return this.serializer.getType();
    }

    private List<DisplayCastingRecipe> displayRecipes = null;

    @Override
    public boolean matches(ICastingContainer inv, World level) {
        return this.bottle.test(inv.getStack()) && this.fluid.test(inv.getFluid());
    }

    @Override
    public long getFluidAmount(ICastingContainer inv) {
        return this.fluid.getAmount(inv.getFluid());
    }

    @Override
    public boolean isConsumed() {
        return true;
    }

    @Override
    public boolean switchSlots() {
        return false;
    }

    @Override
    public int getCoolingTime(ICastingContainer inv) {
        return this.coolingTime;
    }

    @Override
    public ItemStack assemble(ICastingContainer inv) {
        ItemStack result = new ItemStack(this.result);
        result.setNbt(inv.getFluidTag());
        return result;
    }

    @Override
    public List<DisplayCastingRecipe> getRecipes() {
        if (this.displayRecipes == null) {
            // create a subrecipe for every potion variant
            List<ItemStack> bottles = List.of(this.bottle.getMatchingStacks());
            this.displayRecipes = Registries.POTION.stream()
                    .map(potion -> {
                        ItemStack result = PotionUtil.setPotion(new ItemStack(this.result), potion);
                        return new DisplayCastingRecipe(this.getType(), bottles, this.fluid.getFluids().stream()
                                .map(fluid -> new FluidStack(fluid.getFluid(), fluid.getAmount(), result.getNbt()))
                                .toList(),
                                result, this.coolingTime, true);
                    }).toList();
        }
        return this.displayRecipes;
    }


    /* Recipe interface methods */

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(Ingredient.EMPTY, this.bottle);
    }

    /**
     * @deprecated use {@link #assemble(Inventory)}
     */
    @Deprecated
    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return new ItemStack(this.result);
    }
}
