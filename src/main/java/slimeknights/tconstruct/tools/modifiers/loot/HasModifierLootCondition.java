package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.JsonSerializer;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

/**
 * Condition to check if a held tool has the given modifier
 */
@RequiredArgsConstructor
public class HasModifierLootCondition implements LootCondition {
    private final ModifierId modifier;

    @Override
    public LootConditionType getType() {
        return TinkerModifiers.hasModifierLootCondition.get();
    }

    @Override
    public boolean test(LootContext context) {
        ItemStack tool = context.get(LootContextParameters.TOOL);
        return tool != null && tool.isIn(TinkerTags.Items.MODIFIABLE) && ModifierUtil.getModifierLevel(tool, this.modifier) > 0;
    }

    public static class ConditionSerializer implements JsonSerializer<HasModifierLootCondition> {
        @Override
        public void toJson(JsonObject json, HasModifierLootCondition condition, JsonSerializationContext context) {
            json.addProperty("modifier", condition.modifier.toString());
        }

        @Override
        public HasModifierLootCondition fromJson(JsonObject json, JsonDeserializationContext context) {
            return new HasModifierLootCondition(new ModifierId(JsonHelper.getResourceLocation(json, "modifier")));
        }
    }
}
