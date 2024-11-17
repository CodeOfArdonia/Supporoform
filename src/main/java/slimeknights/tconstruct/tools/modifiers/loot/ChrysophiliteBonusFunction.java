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
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.traits.skull.ChrysophiliteModifier;

import java.util.Set;

/**
 * Loot modifier to boost drops based on teh chrysophilite amount
 */
public class ChrysophiliteBonusFunction extends ConditionalLootFunction {
    public static final Serializer SERIALIZER = new Serializer();

    /**
     * Formula to apply
     */
    private final ApplyBonusLootFunction.Formula formula;
    /**
     * If true, the includes the helmet in the level, if false level is just gold pieces
     */
    private final boolean includeBase;

    protected ChrysophiliteBonusFunction(LootCondition[] conditions, ApplyBonusLootFunction.Formula formula, boolean includeBase) {
        super(conditions);
        this.formula = formula;
        this.includeBase = includeBase;
    }

    /**
     * Creates a generic builder
     */
    public static ConditionalLootFunction.Builder<?> builder(ApplyBonusLootFunction.Formula formula, boolean includeBase) {
        return builder(conditions -> new ChrysophiliteBonusFunction(conditions, formula, includeBase));
    }

    /**
     * Creates a builder for the binomial with bonus formula
     */
    public static ConditionalLootFunction.Builder<?> binomialWithBonusCount(float probability, int extra, boolean includeBase) {
        return builder(new ApplyBonusLootFunction.BinomialWithBonusCount(extra, probability), includeBase);
    }

    /**
     * Creates a builder for the ore drops formula
     */
    public static ConditionalLootFunction.Builder<?> oreDrops(boolean includeBase) {
        return builder(new ApplyBonusLootFunction.OreDrops(), includeBase);
    }

    /**
     * Creates a builder for the uniform bonus count
     */
    public static ConditionalLootFunction.Builder<?> uniformBonusCount(int bonusMultiplier, boolean includeBase) {
        return builder(new ApplyBonusLootFunction.UniformBonusCount(bonusMultiplier), includeBase);
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        int level = ChrysophiliteModifier.getTotalGold(context.get(LootContextParameters.THIS_ENTITY));
        if (!this.includeBase) {
            level--;
        }
        if (level > 0) {
            stack.setCount(this.formula.getValue(context.getRandom(), stack.getCount(), level));
        }
        return stack;
    }

    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return ImmutableSet.of(LootContextParameters.THIS_ENTITY);
    }

    @Override
    public LootFunctionType getType() {
        return TinkerModifiers.chrysophiliteBonusFunction.get();
    }

    /**
     * Serializer class
     */
    private static class Serializer extends ConditionalLootFunction.Serializer<ChrysophiliteBonusFunction> {
        @Override
        public void toJson(JsonObject json, ChrysophiliteBonusFunction loot, JsonSerializationContext context) {
            super.toJson(json, loot, context);
            json.addProperty("formula", loot.formula.getId().toString());
            JsonObject parameters = new JsonObject();
            loot.formula.toJson(parameters, context);
            if (parameters.size() > 0) {
                json.add("parameters", parameters);
            }
            json.addProperty("include_base", loot.includeBase);
        }

        @Override
        public ChrysophiliteBonusFunction fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
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
            return new ChrysophiliteBonusFunction(conditions, formula, includeBase);
        }
    }
}
