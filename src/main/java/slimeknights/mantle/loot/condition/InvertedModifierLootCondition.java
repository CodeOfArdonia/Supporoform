package slimeknights.mantle.loot.condition;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.Mantle;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Loot modifier condition that inverts the base condition
 */
@RequiredArgsConstructor
public class InvertedModifierLootCondition implements ILootModifierCondition {
    public static final Identifier ID = Mantle.getResource("inverted");

    /**
     * Condition to invert
     */
    private final ILootModifierCondition base;

    @Override
    public boolean test(List<ItemStack> generatedLoot, LootContext context) {
        return !this.base.test(generatedLoot, context);
    }

    @Override
    public ILootModifierCondition inverted() {
        return this.base;
    }

    @Override
    public JsonObject serialize(JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("condition", this.base.serialize(context));
        return json;
    }

    /**
     * Deserializes this condition from JSON
     */
    public static InvertedModifierLootCondition deserialize(JsonElement object, Type typeOfT, JsonDeserializationContext context) {
        JsonObject json = JsonHelper.asObject(object, "condition");
        ILootModifierCondition condition = ILootModifierCondition.MODIFIER_CONDITIONS.deserialize(JsonHelper.getObject(json, "condition"), ILootModifierCondition.class, context);
        return new InvertedModifierLootCondition(condition);
    }
}
