package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.Identifier;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.Set;

/**
 * Boosts drop rates based on modifier level
 */
public class ModifierBonusLootFunction extends ConditionalLootFunction {
    /**
     * Modifier ID to use for multiplier bonus
     */
    private final ModifierId modifier;
    /**
     * Formula to apply
     */
    private final ApplyBonusLootFunction.Formula formula;
    /**
     * If true, considers level 1 as bonus, if false considers level 1 as no bonus
     */
    private final boolean includeBase;

    protected ModifierBonusLootFunction(LootCondition[] conditions, ModifierId modifier, ApplyBonusLootFunction.Formula formula, boolean includeBase) {
        super(conditions);
        this.modifier = modifier;
        this.formula = formula;
        this.includeBase = includeBase;
    }

    /**
     * Creates a generic builder
     */
    public static net.minecraft.loot.function.ConditionalLootFunction.Builder<?> builder(ModifierId modifier, ApplyBonusLootFunction.Formula formula, boolean includeBase) {
        return builder(conditions -> new ModifierBonusLootFunction(conditions, modifier, formula, includeBase));
    }

    /**
     * Creates a builder for the binomial with bonus formula
     */
    public static net.minecraft.loot.function.ConditionalLootFunction.Builder<?> binomialWithBonusCount(ModifierId modifier, float probability, int extra, boolean includeBase) {
        return builder(modifier, new ApplyBonusLootFunction.BinomialWithBonusCount(extra, probability), includeBase);
    }

    /**
     * Creates a builder for the ore drops formula
     */
    public static net.minecraft.loot.function.ConditionalLootFunction.Builder<?> oreDrops(ModifierId modifier, boolean includeBase) {
        return builder(modifier, new ApplyBonusLootFunction.OreDrops(), includeBase);
    }

    /**
     * Creates a builder for the uniform bonus count
     */
    public static net.minecraft.loot.function.ConditionalLootFunction.Builder<?> uniformBonusCount(ModifierId modifier, int bonusMultiplier, boolean includeBase) {
        return builder(modifier, new ApplyBonusLootFunction.UniformBonusCount(bonusMultiplier), includeBase);
    }

    @Override
    public LootFunctionType getType() {
        return TinkerModifiers.modifierBonusFunction.get();
    }

    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return ImmutableSet.of(LootContextParameters.TOOL);
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        int level = ModifierUtil.getModifierLevel(context.requireParameter(LootContextParameters.TOOL), this.modifier);
        if (!this.includeBase) {
            level--;
        }
        if (level > 0) {
            stack.setCount(this.formula.getValue(context.getRandom(), stack.getCount(), level));
        }
        return stack;
    }

    /**
     * Serializer class
     */
    public static class Serializer extends ConditionalLootFunction.Serializer<ModifierBonusLootFunction> {
        @Override
        public void toJson(JsonObject json, ModifierBonusLootFunction loot, JsonSerializationContext context) {
            super.toJson(json, loot, context);
            json.addProperty("modifier", loot.modifier.toString());
            json.addProperty("formula", loot.formula.getId().toString());
            JsonObject parameters = new JsonObject();
            loot.formula.toJson(parameters, context);
            if (parameters.size() > 0) {
                json.add("parameters", parameters);
            }
            json.addProperty("include_base", loot.includeBase);
        }

        @Override
        public ModifierBonusLootFunction fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            ModifierId modifier = new ModifierId(JsonHelper.getResourceLocation(json, "modifier"));
            Identifier id = JsonHelper.getResourceLocation(json, "formula");
            ApplyBonusLootFunction.FormulaFactory deserializer = ApplyBonusLootFunction.FACTORIES.get(id);
            if (deserializer == null) {
                throw new JsonParseException("Invalid formula id: " + id);
            }
            JsonObject parameters;
            if (json.has("parameters")) {
                parameters = net.minecraft.util.JsonHelper.getObject(json, "parameters");
            } else {
                parameters = new JsonObject();
            }
            ApplyBonusLootFunction.Formula formula = deserializer.deserialize(parameters, context);
            boolean includeBase = net.minecraft.util.JsonHelper.getBoolean(json, "include_base", true);
            return new ModifierBonusLootFunction(conditions, modifier, formula, includeBase);
        }
    }
}
