package slimeknights.mantle.recipe.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.MantleRecipeSerializers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for a shaped recipe with fallbacks
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fallback")
public class ShapedFallbackRecipeBuilder {
    private final ShapedRecipeJsonBuilder base;
    private final List<Identifier> alternatives = new ArrayList<>();

    /**
     * Adds a single alternative to this recipe. Any matching alternative causes this recipe to fail
     *
     * @param location Alternative
     * @return Builder instance
     */
    public ShapedFallbackRecipeBuilder addAlternative(Identifier location) {
        this.alternatives.add(location);
        return this;
    }

    /**
     * Adds a list of alternatives to this recipe. Any matching alternative causes this recipe to fail
     *
     * @param locations Alternative list
     * @return Builder instance
     */
    public ShapedFallbackRecipeBuilder addAlternatives(Collection<Identifier> locations) {
        this.alternatives.addAll(locations);
        return this;
    }

    /**
     * Builds the recipe using the output as the name
     *
     * @param consumer Recipe consumer
     */
    public void build(Consumer<RecipeJsonProvider> consumer) {
        this.base.offerTo(base -> consumer.accept(new Result(base, this.alternatives)));
    }

    /**
     * Builds the recipe using the given ID
     *
     * @param consumer Recipe consumer
     * @param id       Recipe ID
     */
    public void build(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        this.base.offerTo(base -> consumer.accept(new Result(base, this.alternatives)), id);
    }

    private record Result(RecipeJsonProvider base, List<Identifier> alternatives) implements RecipeJsonProvider {
        @Override
        public void serialize(JsonObject json) {
            this.base.serialize(json);
            json.add("alternatives", this.alternatives.stream()
                    .map(Identifier::toString)
                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return MantleRecipeSerializers.CRAFTING_SHAPED_FALLBACK;
        }

        @Override
        public Identifier getRecipeId() {
            return this.base.getRecipeId();
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
