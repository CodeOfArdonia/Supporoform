package slimeknights.tconstruct.library.recipe.material;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.recipe.container.ISingleStackContainer;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recipe to get the material from an ingredient
 */
public class MaterialRecipe implements ICustomOutputRecipe<ISingleStackContainer>, IMaterialValue {
    public static final RecordLoadable<MaterialRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            LoadableRecipeSerializer.RECIPE_GROUP,
            IngredientLoadable.DISALLOW_EMPTY.requiredField("ingredient", MaterialRecipe::getIngredient),
            IMaterialValue.VALUE_FIELD,
            IMaterialValue.NEEDED_FIELD,
            MaterialVariantId.LOADABLE.requiredField("material", r -> r.getMaterial().getVariant()),
            ItemOutput.Loadable.OPTIONAL_STACK.emptyField("leftover", r -> r.leftover),
            MaterialRecipe::new);

    /**
     * Vanilla requires 4 ingots for full repair, we drop it down to 3 to mesh better with nuggets and blocks and to fit small head costs better
     */
    public static final float INGOTS_PER_REPAIR = 3f;

    @Getter
    protected final Identifier id;
    @Getter
    protected final String group;
    @Getter
    protected final Ingredient ingredient;
    /**
     * Amount of material this recipe returns
     */
    @Getter
    protected final int value;
    /**
     * Amount of input items needed to craft this material
     */
    @Getter
    protected final int needed;
    /**
     * Material ID for the recipe return
     */
    @Getter
    protected final MaterialVariant material;
    /**
     * Leftover stack of value 1, used if the value is more than 1
     */
    protected final ItemOutput leftover;

    /**
     * Creates a new material recipe
     */
    @SuppressWarnings("WeakerAccess")
    public MaterialRecipe(Identifier id, String group, Ingredient ingredient, int value, int needed, MaterialVariantId materialId, ItemOutput leftover) {
        this.id = id;
        this.group = group;
        this.ingredient = ingredient;
        this.value = value;
        this.needed = needed;
        this.material = MaterialVariant.of(materialId);
        // ignore leftover if the value is 1, its useless to us
        this.leftover = value > 1 ? leftover : ItemOutput.EMPTY;
    }

    /* Basic */

    @Override
    public RecipeType<?> getType() {
        return TinkerRecipeTypes.MATERIAL.get();
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(TinkerTables.partBuilder);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerTables.materialRecipeSerializer.get();
    }

    @Override
    public ItemStack getLeftover() {
        return this.leftover.get().copy();
    }

    /* Material methods */

    @Override
    public boolean matches(ISingleStackContainer inv, World worldIn) {
        return !this.material.isUnknown() && this.ingredient.test(inv.getStack());
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(Ingredient.EMPTY, this.ingredient);
    }

    /**
     * Cache of the display items list
     */
    private List<ItemStack> displayItems = null;

    /**
     * Gets a list of stacks for display in the recipe
     */
    public List<ItemStack> getDisplayItems() {
        if (this.displayItems == null) {
            if (this.needed > 1) {
                this.displayItems = Arrays.stream(this.ingredient.getMatchingStacks())
                        .map(stack -> ItemHandlerHelper.copyStackWithSize(stack, this.needed))
                        .collect(Collectors.toList());
            } else {
                this.displayItems = Arrays.asList(this.ingredient.getMatchingStacks());
            }
        }
        return this.displayItems;
    }

    /**
     * Gets the amount to repair per item for tool repair
     *
     * @param amount Base material amount, typically the head durability stat
     * @return Float amount per item to repair
     */
    public float scaleRepair(float amount) {
        // not cached as it may vary per stat type
        return this.getValue() * amount / INGOTS_PER_REPAIR / this.getNeeded();
    }
}
