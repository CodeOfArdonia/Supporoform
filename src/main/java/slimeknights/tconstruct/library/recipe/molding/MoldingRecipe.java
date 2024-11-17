package slimeknights.tconstruct.library.recipe.molding;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICommonRecipe;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;

/**
 * Recipe to combine two items on the top of a casting table, changing the first
 */
public class MoldingRecipe implements ICommonRecipe<IMoldingContainer> {
    public static final RecordLoadable<MoldingRecipe> LOADER = RecordLoadable.create(
            LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
            ContextKey.ID.requiredField(),
            IngredientLoadable.DISALLOW_EMPTY.requiredField("material", MoldingRecipe::getMaterial),
            IngredientLoadable.ALLOW_EMPTY.defaultField("pattern", Ingredient.EMPTY, MoldingRecipe::getPattern),
            BooleanLoadable.INSTANCE.defaultField("pattern_consumed", false, false, MoldingRecipe::isPatternConsumed),
            ItemOutput.Loadable.REQUIRED_STACK.requiredField("result", r -> r.recipeOutput),
            MoldingRecipe::new);

    @Getter
    private final RecipeType<?> type;
    @Getter
    private final RecipeSerializer<?> serializer;
    @Getter
    private final Identifier id;
    @Getter
    private final Ingredient material;
    @Getter
    private final Ingredient pattern;
    @Getter
    private final boolean patternConsumed;
    private final ItemOutput recipeOutput;

    public MoldingRecipe(TypeAwareRecipeSerializer<?> serializer, Identifier id, Ingredient material, Ingredient pattern, boolean patternConsumed, ItemOutput recipeOutput) {
        this.type = serializer.getType();
        this.serializer = serializer;
        this.id = id;
        this.material = material;
        this.pattern = pattern;
        this.patternConsumed = pattern != Ingredient.EMPTY && patternConsumed;
        this.recipeOutput = recipeOutput;
    }

    @Override
    public boolean matches(IMoldingContainer inv, World worldIn) {
        return this.material.test(inv.getMaterial()) && this.pattern.test(inv.getPattern());
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(Ingredient.EMPTY, this.material, this.pattern);
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return this.recipeOutput.get();
    }
}
