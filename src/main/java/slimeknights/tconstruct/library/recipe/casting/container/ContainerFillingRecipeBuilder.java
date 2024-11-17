package slimeknights.tconstruct.library.recipe.casting.container;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builder for a container filling recipe. Takes an arbitrary fluid for a specific amount to fill a Forge {@link net.minecraftforge.fluids.capability.IFluidHandlerItem}
 */
@AllArgsConstructor(staticName = "castingRecipe")
@SuppressWarnings({"WeakerAccess", "unused"})
public class ContainerFillingRecipeBuilder extends AbstractRecipeBuilder<ContainerFillingRecipeBuilder> {
    private final Identifier result;
    private final int fluidAmount;
    private final TypeAwareRecipeSerializer<? extends ContainerFillingRecipe> recipeSerializer;

    /**
     * Creates a new builder instance using the given result, amount, and serializer
     *
     * @param result           Recipe result
     * @param fluidAmount      Container size
     * @param recipeSerializer Serializer
     * @return Builder instance
     */
    public static ContainerFillingRecipeBuilder castingRecipe(ItemConvertible result, int fluidAmount, TypeAwareRecipeSerializer<? extends ContainerFillingRecipe> recipeSerializer) {
        return new ContainerFillingRecipeBuilder(Registries.ITEM.getId(result.asItem()), fluidAmount, recipeSerializer);
    }

    /**
     * Creates a new basin recipe builder using the given result, amount, and serializer
     *
     * @param result      Recipe result
     * @param fluidAmount Container size
     * @return Builder instance
     */
    public static ContainerFillingRecipeBuilder basinRecipe(Identifier result, int fluidAmount) {
        return castingRecipe(result, fluidAmount, TinkerSmeltery.basinFillingRecipeSerializer.get());
    }

    /**
     * Creates a new basin recipe builder using the given result, amount, and serializer
     *
     * @param result      Recipe result
     * @param fluidAmount Container size
     * @return Builder instance
     */
    public static ContainerFillingRecipeBuilder basinRecipe(ItemConvertible result, int fluidAmount) {
        return castingRecipe(result, fluidAmount, TinkerSmeltery.basinFillingRecipeSerializer.get());
    }

    /**
     * Creates a new table recipe builder using the given result, amount, and serializer
     *
     * @param result      Recipe result
     * @param fluidAmount Container size
     * @return Builder instance
     */
    public static ContainerFillingRecipeBuilder tableRecipe(Identifier result, int fluidAmount) {
        return castingRecipe(result, fluidAmount, TinkerSmeltery.tableFillingRecipeSerializer.get());
    }

    /**
     * Creates a new table recipe builder using the given result, amount, and serializer
     *
     * @param result      Recipe result
     * @param fluidAmount Container size
     * @return Builder instance
     */
    public static ContainerFillingRecipeBuilder tableRecipe(ItemConvertible result, int fluidAmount) {
        return castingRecipe(result, fluidAmount, TinkerSmeltery.tableFillingRecipeSerializer.get());
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        this.save(consumer, this.result);
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumerIn, Identifier id) {
        Identifier advancementId = this.buildOptionalAdvancement(id, "casting");
        consumerIn.accept(new Result(id, advancementId));
    }

    private class Result extends AbstractFinishedRecipe {
        public Result(Identifier ID, @Nullable Identifier advancementID) {
            super(ID, advancementID);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return recipeSerializer;
        }

        @Override
        public void serialize(JsonObject json) {
            if (!group.isEmpty()) {
                json.addProperty("group", group);
            }
            json.addProperty("fluid_amount", fluidAmount);
            // TODO: consider another way to spoof this for datagen?
            json.addProperty("container", result.toString());
        }
    }
}
