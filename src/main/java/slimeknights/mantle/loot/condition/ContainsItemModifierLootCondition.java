package slimeknights.mantle.loot.condition;

import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.Mantle;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Loot condition requiring one of the existing items is the given stack
 */
@RequiredArgsConstructor
public class ContainsItemModifierLootCondition implements ILootModifierCondition {
    public static final Identifier ID = Mantle.getResource("contains_item");
    private final Ingredient ingredient;
    private final int amountNeeded;

    public ContainsItemModifierLootCondition(Ingredient ingredient) {
        this(ingredient, 1);
    }

    @Override
    public boolean test(List<ItemStack> generatedLoot, LootContext context) {
        int matched = 0;
        for (ItemStack stack : generatedLoot) {
            if (this.ingredient.test(stack)) {
                matched += stack.getCount();
                if (matched >= this.amountNeeded) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JsonObject serialize(JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("ingredient", this.ingredient.toJson());
        if (this.amountNeeded != 1) {
            json.addProperty("needed", this.amountNeeded);
        }
        return json;
    }

    /**
     * Parses this from JSON
     */
    public static ContainsItemModifierLootCondition deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject json = JsonHelper.asObject(element, "condition");
        Ingredient ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"));
        int needed = JsonHelper.getInt(json, "needed", 1);
        return new ContainsItemModifierLootCondition(ingredient, needed);
    }
}
