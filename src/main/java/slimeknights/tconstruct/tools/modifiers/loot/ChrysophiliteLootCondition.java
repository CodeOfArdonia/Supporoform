package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.JsonSerializer;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.traits.skull.ChrysophiliteModifier;

import java.util.Set;

/**
 * Condition to check if the enemy has the chrysophilite modifier
 */
public class ChrysophiliteLootCondition implements LootCondition {
    public static final ChrysophiliteSerializer SERIALIZER = new ChrysophiliteSerializer();
    public static final ChrysophiliteLootCondition INSTANCE = new ChrysophiliteLootCondition();

    private ChrysophiliteLootCondition() {
    }

    @Override
    public boolean test(LootContext context) {
        return ChrysophiliteModifier.getTotalGold(context.get(LootContextParameters.THIS_ENTITY)) > 0;
    }

    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return ImmutableSet.of(LootContextParameters.THIS_ENTITY);
    }

    @Override
    public LootConditionType getType() {
        return TinkerModifiers.chrysophiliteLootCondition.get();
    }

    /**
     * Loot serializer instance
     */
    private static class ChrysophiliteSerializer implements JsonSerializer<ChrysophiliteLootCondition> {
        @Override
        public void toJson(JsonObject json, ChrysophiliteLootCondition loot, JsonSerializationContext context) {
        }

        @Override
        public ChrysophiliteLootCondition fromJson(JsonObject jsonObject, JsonDeserializationContext context) {
            return INSTANCE;
        }
    }
}
