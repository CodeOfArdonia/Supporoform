package slimeknights.mantle.loot.condition;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.JsonSerializer;
import slimeknights.mantle.loot.MantleLoot;
import slimeknights.mantle.util.JsonHelper;

import java.util.Set;

/**
 * Variant of {@link net.minecraft.loot.condition.BlockStatePropertyLootCondition} that allows using a tag for block type instead of a block
 */
@RequiredArgsConstructor
public class BlockTagLootCondition implements LootCondition {
    public static final SerializerImpl SERIALIZER = new SerializerImpl();

    private final TagKey<Block> tag;
    private final StatePredicate properties;

    public BlockTagLootCondition(TagKey<Block> tag) {
        this(tag, StatePredicate.ANY);
    }

    public BlockTagLootCondition(TagKey<Block> tag, StatePredicate.Builder builder) {
        this(tag, builder.build());
    }

    @Override
    public boolean test(LootContext context) {
        BlockState state = context.get(LootContextParameters.BLOCK_STATE);
        return state != null && state.isIn(this.tag) && this.properties.test(state);
    }

    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return ImmutableSet.of(LootContextParameters.BLOCK_STATE);
    }

    @Override
    public LootConditionType getType() {
        return MantleLoot.BLOCK_TAG_CONDITION;
    }

    private static class SerializerImpl implements JsonSerializer<BlockTagLootCondition> {
        @Override
        public void toJson(JsonObject json, BlockTagLootCondition loot, JsonSerializationContext context) {
            json.addProperty("tag", loot.tag.id().toString());
            if (loot.properties != StatePredicate.ANY) {
                json.add("properties", loot.properties.toJson());
            }
        }

        @Override
        public BlockTagLootCondition fromJson(JsonObject json, JsonDeserializationContext context) {
            TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, JsonHelper.getResourceLocation(json, "tag"));
            StatePredicate predicate = StatePredicate.ANY;
            if (json.has("properties")) {
                predicate = StatePredicate.fromJson(json.get("properties"));
            }
            return new BlockTagLootCondition(tag, predicate);
        }
    }
}
