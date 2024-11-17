package slimeknights.mantle.recipe.data;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds a recipe consumer wrapper, which adds some extra properties to wrap the result of another recipe
 */
@SuppressWarnings("UnusedReturnValue")
public class ConsumerWrapperBuilder {
    private final List<ICondition> conditions = new ArrayList<>();
    @Nullable
    private final RecipeSerializer<?> override;
    @Nullable
    private final Identifier overrideName;

    private ConsumerWrapperBuilder(@Nullable RecipeSerializer<?> override, @Nullable Identifier overrideName) {
        this.override = override;
        this.overrideName = overrideName;
    }

    /**
     * Creates a wrapper builder with the default serializer
     *
     * @return Default serializer builder
     */
    public static ConsumerWrapperBuilder wrap() {
        return new ConsumerWrapperBuilder(null, null);
    }

    /**
     * Creates a wrapper builder with a serializer override
     *
     * @param override Serializer override
     * @return Default serializer builder
     */
    public static ConsumerWrapperBuilder wrap(RecipeSerializer<?> override) {
        return new ConsumerWrapperBuilder(override, null);
    }

    /**
     * Creates a wrapper builder with a serializer name override
     *
     * @param override Serializer override
     * @return Default serializer builder
     */
    public static ConsumerWrapperBuilder wrap(Identifier override) {
        return new ConsumerWrapperBuilder(null, override);
    }

    /**
     * Adds a conditional to the consumer
     *
     * @param condition Condition to add
     * @return Added condition
     */
    public ConsumerWrapperBuilder addCondition(ICondition condition) {
        this.conditions.add(condition);
        return this;
    }

    /**
     * Builds the consumer for the wrapper builder
     *
     * @param consumer Base consumer
     * @return Built wrapper consumer
     */
    public Consumer<RecipeJsonProvider> build(Consumer<RecipeJsonProvider> consumer) {
        return (recipe) -> consumer.accept(new Wrapped(recipe, this.conditions, this.override, this.overrideName));
    }

    private static class Wrapped implements RecipeJsonProvider {
        private final RecipeJsonProvider original;
        private final List<ICondition> conditions;
        @Nullable
        private final RecipeSerializer<?> override;
        @Nullable
        private final Identifier overrideName;

        private Wrapped(RecipeJsonProvider original, List<ICondition> conditions, @Nullable RecipeSerializer<?> override, @Nullable Identifier overrideName) {
            // if wrapping another wrapper result, merge the two together
            if (original instanceof Wrapped toMerge) {
                this.original = toMerge.original;
                this.conditions = ImmutableList.<ICondition>builder().addAll(toMerge.conditions).addAll(conditions).build();
                // consumer wrappers are processed inside out, so the innermost wrapped recipe is the one with the most recent serializer override
                if (toMerge.override != null || toMerge.overrideName != null) {
                    this.override = toMerge.override;
                    this.overrideName = toMerge.overrideName;
                } else {
                    this.override = override;
                    this.overrideName = overrideName;
                }
            } else {
                this.original = original;
                this.conditions = conditions;
                this.override = override;
                this.overrideName = overrideName;
            }
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (this.overrideName != null) {
                json.addProperty("type", this.overrideName.toString());
            } else {
                json.addProperty("type", Objects.requireNonNull(Registries.RECIPE_SERIALIZER.getId(this.getSerializer())).toString());
            }
            this.serialize(json);
            return json;
        }

        @Override
        public void serialize(JsonObject json) {
            // add conditions on top
            if (!this.conditions.isEmpty()) {
                JsonArray conditionsArray = new JsonArray();
                for (ICondition condition : this.conditions) {
                    conditionsArray.add(CraftingHelper.processConditions(condition));
                }
                json.add("conditions", conditionsArray);
            }
            // serialize the normal recipe
            this.original.serialize(json);
        }

        @Override
        public Identifier getRecipeId() {
            return this.original.getRecipeId();
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            if (this.override != null) {
                return this.override;
            }
            return this.original.getSerializer();
        }

        @Nullable
        @Override
        public JsonObject toAdvancementJson() {
            return this.original.toAdvancementJson();
        }

        @Nullable
        @Override
        public Identifier getAdvancementId() {
            return this.original.getAdvancementId();
        }
    }
}
