package slimeknights.mantle.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import io.github.fabricators_of_create.porting_lib.loot.LootModifierManager;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.recipe.Ingredient;
import slimeknights.mantle.data.JsonCodec.GsonCodec;

/**
 * This class contains codecs for various vanilla things that we need to use in codecs. Typically the reason is forge pre-emptively moved a thing to codecs before vanilla did.
 */
public class MantleCodecs {
    /**
     * Codec for loot pool entries
     */
    public static final Codec<LootPoolEntry> LOOT_ENTRY = new GsonCodec<>("loot entry", LootModifierManager.GSON_INSTANCE, LootPoolEntry.class);
    /**
     * Codec for loot pool entries
     */
    public static final Codec<LootFunction[]> LOOT_FUNCTIONS = new GsonCodec<>("loot functions", LootModifierManager.GSON_INSTANCE, LootFunction[].class);
    /**
     * Codec for ingredients, handling forge ingredient types
     */
    public static final Codec<Ingredient> INGREDIENT = new JsonCodec<>() {
        @Override
        public Ingredient deserialize(JsonElement element) {
            return Ingredient.fromJson(element);
        }

        @Override
        public JsonElement serialize(Ingredient ingredient) {
            return ingredient.toJson();
        }

        @Override
        public String toString() {
            return "Ingredient";
        }
    };
}
