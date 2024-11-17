package slimeknights.tconstruct.library.json.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Condition requiring that items exist in the intersection of all required item tags.
 */
public class TagDifferencePresentCondition<T> implements ICondition {
    private static final Identifier NAME = TConstruct.getResource("tag_difference_present");
    public static final Serializer SERIALIZER = new Serializer();

    private final TagKey<T> base;
    private final List<TagKey<T>> subtracted;

    public TagDifferencePresentCondition(TagKey<T> base, List<TagKey<T>> subtracted) {
        if (subtracted.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a condition with no subtracted");
        }
        this.base = base;
        this.subtracted = subtracted;
    }

    /**
     * Creates a condition from a set of keys
     */
    @SafeVarargs
    public static <T> TagDifferencePresentCondition<T> ofKeys(TagKey<T> base, TagKey<T>... subtracted) {
        return new TagDifferencePresentCondition<>(base, Arrays.asList(subtracted));
    }

    /**
     * Creates a condition from a registry and a set of names
     */
    public static <T> TagDifferencePresentCondition<T> ofNames(RegistryKey<? extends Registry<T>> registry, Identifier base, Identifier... subtracted) {
        TagKey<T> baseKey = TagKey.of(registry, base);
        return new TagDifferencePresentCondition<>(baseKey, Arrays.stream(subtracted).map(name -> TagKey.of(registry, name)).toList());
    }

    @Override
    public Identifier getID() {
        return NAME;
    }

    @Override
    public boolean test(IContext context) {
        // get the base tag
        Collection<RegistryEntry<T>> base = context.getTag(this.base);
        if (base == null || base.isEmpty()) {
            return false;
        }

        // no subtracted tags? we good
        if (this.subtracted.isEmpty()) {
            return true;
        }
        // all tags have something, so find the first item that is in all tags
        itemLoop:
        for (RegistryEntry<T> entry : base) {
            // find the first item contained in no subtracted tags
            for (TagKey<T> tag : this.subtracted) {
                if (context.getTag(tag).contains(entry)) {
                    continue itemLoop;
                }
            }
            // no subtracted contains the item? success
            return true;
        }
        // no item not in any subtracted
        return false;
    }

    private static class Serializer implements IConditionSerializer<TagDifferencePresentCondition<?>> {
        @Override
        public void write(JsonObject json, TagDifferencePresentCondition<?> value) {
            json.addProperty("registry", value.base.registry().getValue().toString());
            json.addProperty("base", value.base.id().toString());
            JsonArray names = new JsonArray();
            for (TagKey<?> name : value.subtracted) {
                names.add(name.id().toString());
            }
            json.add("subtracted", names);
        }

        private static <T> TagDifferencePresentCondition<T> readGeneric(JsonObject json) {
            RegistryKey<Registry<T>> registry = RegistryKey.ofRegistry(JsonHelper.getResourceLocation(json, "registry"));
            return new TagDifferencePresentCondition<>(
                    TagKey.of(registry, JsonHelper.getResourceLocation(json, "base")),
                    JsonHelper.parseList(json, "subtracted", (e, s) -> TagKey.of(registry, JsonHelper.convertToResourceLocation(e, s))));
        }

        @Override
        public TagDifferencePresentCondition<?> read(JsonObject json) {
            return readGeneric(json);
        }

        @Override
        public Identifier getID() {
            return NAME;
        }
    }
}
