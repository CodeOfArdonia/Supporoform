package slimeknights.tconstruct.library.recipe.casting.material;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builder for a composite part recipe, should exist for each part
 */
@RequiredArgsConstructor(staticName = "composite")
public class CompositeCastingRecipeBuilder extends AbstractRecipeBuilder<CompositeCastingRecipeBuilder> {
    private final IMaterialItem result;
    private final int itemCost;
    @Accessors(fluent = true)
    @Setter
    private MaterialStatsId castingStatConflict = null;
    private final TypeAwareRecipeSerializer<? extends CompositeCastingRecipe> serializer;

    public static CompositeCastingRecipeBuilder basin(IMaterialItem result, int itemCost) {
        return composite(result, itemCost, TinkerSmeltery.basinCompositeSerializer.get());
    }

    public static CompositeCastingRecipeBuilder table(IMaterialItem result, int itemCost) {
        return composite(result, itemCost, TinkerSmeltery.tableCompositeSerializer.get());
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer) {
        save(consumer, Registry.ITEM.getKey(result.asItem()));
    }

    @Override
    public void save(Consumer<RecipeJsonProvider> consumer, Identifier id) {
        Identifier advancementId = this.buildOptionalAdvancement(id, "casting");
        consumer.accept(new LoadableFinishedRecipe<>(new CompositeCastingRecipe(serializer, id, group, result, itemCost, castingStatConflict), CompositeCastingRecipe.LOADER, advancementId));
    }

    private class Finished extends AbstractFinishedRecipe {
        public Finished(Identifier ID, @Nullable Identifier advancementID) {
            super(ID, advancementID);
        }

        @Override
        public void serialize(JsonObject json) {
            if (!group.isEmpty()) {
                json.addProperty("group", group);
            }
            json.addProperty("result", Registries.ITEM.getKey(result.asItem()).toString());
            json.addProperty("item_cost", itemCost);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return serializer;
        }
    }
}
