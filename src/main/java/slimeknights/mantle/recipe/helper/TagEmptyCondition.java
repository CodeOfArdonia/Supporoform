package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.JsonHelper;

/**
 * Condition that checks when a fluid tag is empty. Same as {@link net.minecraftforge.common.crafting.conditions.TagEmptyCondition} but for fluids instead of items
 */
@RequiredArgsConstructor
public class TagEmptyCondition<T> implements ICondition {
    private static final Identifier NAME = Mantle.getResource("tag_empty");
    public static final Serializer SERIALIZER = new Serializer();
    private final TagKey<T> tag;

    public TagEmptyCondition(RegistryKey<? extends Registry<T>> registry, Identifier name) {
        this(TagKey.of(registry, name));
    }

    @Override
    public Identifier getID() {
        return NAME;
    }

    @Override
    public boolean test(IContext context) {
        return context.getTag(this.tag).isEmpty();
    }

    @Override
    public String toString() {
        return "tag_empty(\"" + this.tag + "\")";
    }

    private static class Serializer implements IConditionSerializer<TagEmptyCondition<?>> {
        @Override
        public void write(JsonObject json, TagEmptyCondition<?> value) {
            json.addProperty("registry", value.tag.registry().getValue().toString());
            json.addProperty("tag", value.tag.id().toString());
        }

        private <T> TagEmptyCondition<T> readGeneric(JsonObject json) {
            RegistryKey<Registry<T>> registry = RegistryKey.ofRegistry(JsonHelper.getResourceLocation(json, "registry"));
            return new TagEmptyCondition<>(registry, JsonHelper.getResourceLocation(json, "tag"));
        }

        @Override
        public TagEmptyCondition<?> read(JsonObject json) {
            return this.readGeneric(json);
        }

        @Override
        public Identifier getID() {
            return NAME;
        }
    }
}
