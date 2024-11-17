package slimeknights.tconstruct.library.json.field;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;

/**
 * Field which creates a JSON object to contain its value.
 *
 * @param field Determines how the contents are added to the object. Getter goes from the target parent.
 * @param key   Key of the object inside the parent JSON
 * @param mode  Determines how the field deals with the object not being present. Note it always will error if the field contains a non-object.
 * @param <T>   Field type
 * @param <P>   Parent type
 */
public record MergingField<T, P>(LoadableField<T, P> field, String key,
                                 MissingMode mode) implements LoadableField<T, P> {
    public enum MissingMode {
        /**
         * Object must exist, and will be created during datagen
         */
        DISALLOWED,
        /**
         * Object is optional at runtime, but will be created at datagen
         */
        CREATE,
        /**
         * Object is optional at runtime and will not be created at datagen
         */
        IGNORE
    }

    @Override
    public T get(JsonObject json) {
        // disallowed really just improves the error message over create in the case of disallowed being a required field
        if (this.mode == MissingMode.DISALLOWED || json.has(this.key)) {
            return this.field.get(JsonHelper.getObject(json, this.key));
        } else {
            return this.field.get(new JsonObject());
        }
    }

    @Override
    public void serialize(P parent, JsonObject json) {
        // if we have the object, write to it
        if (json.has(this.key)) {
            this.field.serialize(parent, JsonHelper.getObject(json, this.key));
        } else if (this.mode != MissingMode.IGNORE) {
            // if we don't have the object, create it unless to ignore
            JsonObject writeTo = new JsonObject();
            this.field.serialize(parent, writeTo);
            json.add(this.key, writeTo);
        }
    }

    @Override
    public T decode(PacketByteBuf buffer) {
        return this.field.decode(buffer);
    }

    @Override
    public void encode(PacketByteBuf buffer, P parent) {
        this.field.encode(buffer, parent);
    }
}
