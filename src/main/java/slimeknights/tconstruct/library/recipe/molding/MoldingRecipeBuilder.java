package slimeknights.tconstruct.library.recipe.molding;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "molding")
public class MoldingRecipeBuilder extends AbstractRecipeBuilder<MoldingRecipeBuilder> {
    private final ItemOutput output;
    private final TypeAwareRecipeSerializer<MoldingRecipe> serializer;
    private Ingredient material = Ingredient.EMPTY;
    private Ingredient pattern = Ingredient.EMPTY;
    private boolean patternConsumed = false;

    /**
     * Creates a new builder of the given item
     *
     * @param item Item output
     * @return Recipe
     */
    public static MoldingRecipeBuilder moldingTable(ItemConvertible item) {
        return molding(ItemOutput.fromItem(item), TinkerSmeltery.moldingTableSerializer.get());
    }

    /**
     * Creates a new builder of the given item
     *
     * @param item Item output
     * @return Recipe
     */
    public static MoldingRecipeBuilder moldingBasin(ItemConvertible item) {
        return molding(ItemOutput.fromItem(item), TinkerSmeltery.moldingBasinSerializer.get());
    }

    /* Inputs */

    /**
     * Sets the material item, on the table
     */
    public MoldingRecipeBuilder setMaterial(Ingredient ingredient) {
        this.material = ingredient;
        return this;
    }

    /**
     * Sets the material item, on the table
     */
    public MoldingRecipeBuilder setMaterial(ItemConvertible item) {
        return setMaterial(Ingredient.ofItems(item));
    }

    /**
     * Sets the material item, on the table
     */
    public MoldingRecipeBuilder setMaterial(TagKey<Item> tag) {
        return setMaterial(Ingredient.fromTag(tag));
    }

    /**
     * Sets the mold item, in the players hand
     */
    public MoldingRecipeBuilder setPattern(Ingredient ingredient, boolean consumed) {
        this.pattern = ingredient;
        this.patternConsumed = consumed;
        return this;
    }

    /**
     * Sets the mold item, in the players hand
     */
    public MoldingRecipeBuilder setPattern(ItemConvertible item, boolean consumed) {
        return setPattern(Ingredient.ofItems(item), consumed);
    }

    /**
     * Sets the mold item, in the players hand
     */
    public MoldingRecipeBuilder setPattern(TagKey<Item> tag, boolean consumed) {
        return setPattern(Ingredient.fromTag(tag), consumed);
    }


    /* Building */

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        save(consumer, Registries.ITEM.getId(output.get().getItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        if (material == Ingredient.EMPTY) {
            throw new IllegalStateException("Missing material for molding recipe");
        }
        Identifier advancementId = buildOptionalAdvancement(id, "molding");
        consumer.accept(new LoadableFinishedRecipe<>(new MoldingRecipe(serializer, id, material, pattern, patternConsumed, output), MoldingRecipe.LOADER, advancementId));
    }

    private class Finished extends AbstractFinishedRecipe {
        public Finished(Identifier ID, @Nullable Identifier advancementID) {
            super(ID, advancementID);
        }

        @Override
        public void serialize(JsonObject json) {
            json.add("material", material.toJson());
            if (pattern != Ingredient.EMPTY) {
                json.add("pattern", pattern.toJson());
                if (patternConsumed) {
                    json.addProperty("pattern_consumed", true);
                }
            }
            json.add("result", output.serialize(false));
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return serializer;
        }
    }
}
