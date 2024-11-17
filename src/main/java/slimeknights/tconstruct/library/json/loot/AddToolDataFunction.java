package slimeknights.tconstruct.library.json.loot;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import lombok.experimental.Accessors;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;

/**
 * Loot function to add data to a tool.
 */
public class AddToolDataFunction extends ConditionalLootFunction {
    public static final Identifier ID = TConstruct.getResource("add_tool_data");
    public static final Serializer SERIALIZER = new Serializer();

    /**
     * Percentage of damage on the tool, if 0 the tool is undamaged
     */
    private final float damage;
    /**
     * Fixed materials on the tool, any nulls in the list will randomize
     */
    private final List<RandomMaterial> materials;

    protected AddToolDataFunction(LootCondition[] conditionsIn, float damage, List<RandomMaterial> materials) {
        super(conditionsIn);
        this.damage = damage;
        this.materials = materials;
    }

    /**
     * Creates a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LootFunctionType getType() {
        return TinkerTools.lootAddToolData.get();
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        if (stack.isIn(TinkerTags.Items.MODIFIABLE)) {
            ToolStack tool = ToolStack.from(stack);
            ToolDefinition definition = tool.getDefinition();
            if (definition.hasMaterials() && !materials.isEmpty()) {
                tool.setMaterials(RandomMaterial.build(ToolMaterialHook.stats(definition), materials, context.getRandom()));
            } else {
                // not multipart? no sense doing materials, just initialize stats
                tool.rebuildStats();
            }
            // set damage last to a percentage of max damage if requested
            if (damage > 0) {
                tool.setDamage((int) (tool.getStats().get(ToolStats.DURABILITY) * damage));
            }
        }
        return stack;
    }

    /**
     * Serializer logic for the function
     */
    private static class Serializer extends ConditionalLootFunction.Serializer<AddToolDataFunction> {
        private static final LoadableField<List<RandomMaterial>, AddToolDataFunction> MATERIAL_LIST = RandomMaterial.LOADER.list(0).defaultField("materials", List.of(), d -> d.materials);

        @Override
        public void toJson(JsonObject json, AddToolDataFunction loot, JsonSerializationContext context) {
            super.toJson(json, loot, context);
            // initial damage
            if (loot.damage > 0) {
                json.addProperty("damage_percent", loot.damage);
            }
            MATERIAL_LIST.serialize(loot, json);
        }

        @Override
        public AddToolDataFunction fromJson(JsonObject object, JsonDeserializationContext context, LootCondition[] conditions) {
            float damage = JsonHelper.getFloat(object, "damage_percent", 0f);
            if (damage < 0 || damage > 1) {
                throw new JsonSyntaxException("damage_percent must be between 0 and 1, given " + damage);
            }
            List<RandomMaterial> materials = MATERIAL_LIST.get(object);
            return new AddToolDataFunction(conditions, damage, materials);
        }
    }

    /**
     * Builder to create a new add tool data function
     */
    @Accessors(chain = true)
    public static class Builder extends ConditionalLootFunction.Builder<Builder> {
        private final ImmutableList.Builder<RandomMaterial> materials = ImmutableList.builder();
        private float damage = 0;

        protected Builder() {
        }

        @Override
        protected Builder getThisBuilder() {
            return this;
        }

        /**
         * Sets the damage for the tool
         */
        public void setDamage(float damage) {
            if (damage < 0 || damage > 1) {
                throw new IllegalArgumentException("Damage must be between 0 and 1, given " + damage);
            }
            this.damage = damage;
        }

        /**
         * Adds a material to the builder
         */
        public Builder addMaterial(RandomMaterial mat) {
            materials.add(mat);
            return this;
        }

        /**
         * Adds a material to the builder
         */
        public Builder addMaterial(MaterialId mat) {
            return addMaterial(RandomMaterial.fixed(mat));
        }

        @Override
        public LootFunction build() {
            return new AddToolDataFunction(getConditions(), damage, materials.build());
        }
    }
}
