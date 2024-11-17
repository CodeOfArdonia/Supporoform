package slimeknights.tconstruct.library.json.condition;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.RequiredArgsConstructor;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonSerializer;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

/**
 * Loot table condition to test if a tag has entries.
 * TODO: the non-loot condition form is redundant to {@link slimeknights.mantle.recipe.helper.TagEmptyCondition}
 */
@RequiredArgsConstructor
public class TagNotEmptyCondition<T> implements LootCondition, ICondition {
    private static final Identifier NAME = TConstruct.getResource("tag_not_empty");
    private final TagKey<T> tag;

    @Override
    public LootConditionType getType() {
        return TinkerCommons.lootTagNotEmptyCondition.get();
    }

    @Override
    public Identifier getID() {
        return NAME;
    }

    @Override
    public boolean test(IContext context) {
        return !context.getTag(this.tag).isEmpty();
    }

    @Override
    public boolean test(LootContext context) {
        Registry<T> registry = RegistryHelper.getRegistry(this.tag.registry());
        return registry != null && registry.iterateEntries(this.tag).iterator().hasNext();
    }

    public static class ConditionSerializer implements JsonSerializer<TagNotEmptyCondition<?>>, IConditionSerializer<TagNotEmptyCondition<?>> {
        /**
         * Helper to deal with generics
         */
        private static <T> TagKey<T> createKey(JsonObject json) {
            RegistryKey<? extends Registry<T>> registry = RegistryKey.ofRegistry(JsonHelper.getResourceLocation(json, "registry"));
            return TagKey.of(registry, JsonHelper.getResourceLocation(json, "tag"));
        }

        @Override
        public void write(JsonObject json, TagNotEmptyCondition<?> value) {
            json.addProperty("registry", value.tag.registry().getValue().toString());
            json.addProperty("tag", value.tag.id().toString());
        }

        @Override
        public void serialize(JsonObject json, TagNotEmptyCondition<?> value, JsonSerializationContext context) {
            this.write(json, value);
        }

        @Override
        public TagNotEmptyCondition<?> read(JsonObject json) {
            return new TagNotEmptyCondition<>(createKey(json));
        }

        @Override
        public TagNotEmptyCondition<?> fromJson(JsonObject json, JsonDeserializationContext context) {
            return this.read(json);
        }

        @Override
        public Identifier getID() {
            return NAME;
        }
    }
}
