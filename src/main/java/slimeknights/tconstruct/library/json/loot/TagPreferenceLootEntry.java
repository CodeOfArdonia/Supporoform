package slimeknights.tconstruct.library.json.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import slimeknights.mantle.recipe.helper.TagPreference;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.function.Consumer;

/**
 * Loot entry that returns an item from a tag
 */
public class TagPreferenceLootEntry extends LeafEntry {
    private final TagKey<Item> tag;

    protected TagPreferenceLootEntry(TagKey<Item> tag, int weight, int quality, LootCondition[] conditions, LootFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.tag = tag;
    }

    @Override
    public LootPoolEntryType getType() {
        return TinkerCommons.lootTagPreference.get();
    }

    @Override
    protected void generateLoot(Consumer<ItemStack> consumer, LootContext context) {
        TagPreference.getPreference(this.tag).ifPresent(item -> consumer.accept(new ItemStack(item)));
    }

    /**
     * Creates a new builder
     */
    public static LeafEntry.Builder<?> tagPreference(TagKey<Item> tag) {
        return builder((weight, quality, conditions, functions) -> new TagPreferenceLootEntry(tag, weight, quality, conditions, functions));
    }

    public static class Serializer extends LeafEntry.Serializer<TagPreferenceLootEntry> {
        @Override
        public void addEntryFields(JsonObject json, TagPreferenceLootEntry object, JsonSerializationContext conditions) {
            super.addEntryFields(json, object, conditions);
            json.addProperty("tag", object.tag.id().toString());
        }

        @Override
        protected TagPreferenceLootEntry fromJson(JsonObject json, JsonDeserializationContext context, int weight, int quality, LootCondition[] conditions, LootFunction[] functions) {
            TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, JsonHelper.getResourceLocation(json, "tag"));
            return new TagPreferenceLootEntry(tag, weight, quality, conditions, functions);
        }
    }
}
