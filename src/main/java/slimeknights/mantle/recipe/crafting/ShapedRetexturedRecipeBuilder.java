package slimeknights.mantle.recipe.crafting;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.MantleRecipeSerializers;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fromShaped")
public class ShapedRetexturedRecipeBuilder {
    private final ShapedRecipeJsonBuilder parent;
    private Ingredient texture;
    private boolean matchAll;

    /**
     * Sets the texture source to the given ingredient
     *
     * @param texture Ingredient to use for texture
     * @return Builder instance
     */
    public ShapedRetexturedRecipeBuilder setSource(Ingredient texture) {
        this.texture = texture;
        return this;
    }

    /**
     * Sets the texture source to the given tag
     *
     * @param tag Tag to use for texture
     * @return Builder instance
     */
    public ShapedRetexturedRecipeBuilder setSource(TagKey<Item> tag) {
        this.texture = Ingredient.fromTag(tag);
        return this;
    }

    /**
     * Sets the match first property on the recipe.
     * If set, the recipe uses the first ingredient match for the texture. If unset, all items that match the ingredient must be the same or no texture is applied
     *
     * @return Builder instance
     */
    public ShapedRetexturedRecipeBuilder setMatchAll() {
        this.matchAll = true;
        return this;
    }

    /**
     * Builds the recipe with the default name using the given consumer
     *
     * @param consumer Recipe consumer
     */
    public void build(Consumer<RecipeJsonProvider> consumer) {
        this.validate();
        this.parent.offerTo(base -> consumer.accept(new Result(base, this.texture, this.matchAll)));
    }

    /**
     * Builds the recipe using the given consumer
     *
     * @param consumer Recipe consumer
     * @param location Recipe location
     */
    public void build(Consumer<RecipeJsonProvider> consumer, Identifier location) {
        this.validate();
        this.parent.offerTo(base -> consumer.accept(new Result(base, this.texture, this.matchAll)), location);
    }

    /**
     * Ensures this recipe can be built
     *
     * @throws IllegalStateException If the recipe cannot be built
     */
    private void validate() {
        if (this.texture == null) {
            throw new IllegalStateException("No texture defined for texture recipe");
        }
    }

    private static class Result implements RecipeJsonProvider {
        private final RecipeJsonProvider base;
        private final Ingredient texture;
        private final boolean matchAll;

        private Result(RecipeJsonProvider base, Ingredient texture, boolean matchAll) {
            this.base = base;
            this.texture = texture;
            this.matchAll = matchAll;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return MantleRecipeSerializers.CRAFTING_SHAPED_RETEXTURED;
        }

        @Override
        public Identifier getRecipeId() {
            return this.base.getRecipeId();
        }

        @Override
        public void serialize(JsonObject json) {
            this.base.serialize(json);
            json.add("texture", this.texture.toJson());
            json.addProperty("match_all", this.matchAll);
        }

        @Nullable
        @Override
        public JsonObject toAdvancementJson() {
            return this.base.toAdvancementJson();
        }

        @Nullable
        @Override
        public Identifier getAdvancementId() {
            return this.base.getAdvancementId();
        }
    }
}
