package slimeknights.tconstruct.library.recipe.casting;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.function.Consumer;

/**
 * Builder for a potion bottle filling recipe. Takes a fluid and optional cast to create an item that copies the fluid NBT
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
@RequiredArgsConstructor(staticName = "castingRecipe")
public class PotionCastingRecipeBuilder extends AbstractRecipeBuilder<PotionCastingRecipeBuilder> {
    private final Item result;
    private final TypeAwareRecipeSerializer<PotionCastingRecipe> recipeSerializer;
    private Ingredient bottle = Ingredient.EMPTY;
    private FluidIngredient fluid = FluidIngredient.EMPTY;
    @Setter
    @Accessors(chain = true)
    private int coolingTime = 5;

    /**
     * Creates a new casting basin recipe
     *
     * @param result Recipe result
     * @return Builder instance
     */
    public static PotionCastingRecipeBuilder basinRecipe(ItemConvertible result) {
        return castingRecipe(result.asItem(), TinkerSmeltery.basinPotionRecipeSerializer.get());
    }

    /**
     * Creates a new casting table recipe
     *
     * @param result Recipe result
     * @return Builder instance
     */
    public static PotionCastingRecipeBuilder tableRecipe(ItemConvertible result) {
        return castingRecipe(result.asItem(), TinkerSmeltery.tablePotionRecipeSerializer.get());
    }


    /* Fluids */

    /**
     * Sets the fluid for this recipe
     *
     * @param tagIn  Tag<Fluid> instance
     * @param amount amount of fluid
     * @return Builder instance
     */
    public PotionCastingRecipeBuilder setFluid(TagKey<Fluid> tagIn, int amount) {
        return this.setFluid(FluidIngredient.of(tagIn, amount));
    }

    /**
     * Sets the fluid ingredient
     *
     * @param fluid Fluid ingredient instance
     * @return Builder instance
     */
    public PotionCastingRecipeBuilder setFluid(FluidIngredient fluid) {
        this.fluid = fluid;
        return this;
    }


    /* Cast */

    /**
     * Sets the cast from a tag, bottles are always consumed
     *
     * @param tagIn Cast tag
     * @return Builder instance
     */
    public PotionCastingRecipeBuilder setBottle(TagKey<Item> tagIn) {
        return this.setBottle(Ingredient.fromTag(tagIn));
    }

    /**
     * Sets the bottle from an item, bottles are always consumed
     *
     * @param itemIn Cast item
     * @return Builder instance
     */
    public PotionCastingRecipeBuilder setBottle(ItemConvertible itemIn) {
        return this.setBottle(Ingredient.ofItems(itemIn));
    }

    /**
     * Sets the bottle from an ingredient, bottles are always consumed
     *
     * @param ingredient Cast ingredient
     * @return Builder instance
     */
    public PotionCastingRecipeBuilder setBottle(Ingredient ingredient) {
        this.bottle = ingredient;
        return this;
    }

    /**
     * Builds a recipe using the registry name as the recipe name
     *
     * @param consumerIn Recipe consumer
     */
    @Override
    public void save(Consumer<RecipeJsonProvider> consumerIn) {
        this.save(consumerIn, Registries.ITEM.getId(this.result));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (this.fluid == FluidIngredient.EMPTY) {
            throw new IllegalStateException("Casting recipes require a fluid input");
        }
        if (this.coolingTime < 0) {
            throw new IllegalStateException("Cooling time is too low, must be at least 0");
        }
        Identifier advancementId = this.buildOptionalAdvancement(id, "casting");
        consumer.accept(new LoadableFinishedRecipe<>(new PotionCastingRecipe(this.recipeSerializer, id, this.group, this.bottle, this.fluid, this.result, this.coolingTime), PotionCastingRecipe.LOADER, advancementId));
    }
}
