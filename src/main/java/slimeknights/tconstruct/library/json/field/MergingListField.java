package slimeknights.tconstruct.library.json.field;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.field.NullableField;

import java.util.List;
import java.util.function.Function;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;

/**
 * Field that writes into a list of objects inside the parent object. Requires the other list to be equal or smaller size from this list.
 *
 * @param field  Field with an identity "getter" to fetch values from the nested array. The key inside this field determines the location in the list object.
 *               This field may not evaluate to null, use an appropriate default instead
 * @param key    Key in the parent that contains the list
 * @param getter Getter to fetch the list from the parent object
 * @param <T>    Field type
 * @param <P>    Parent type
 */
public record MergingListField<T, P>(LoadableField<T, T> field, String key,
                                     Function<P, List<T>> getter) implements LoadableField<List<T>, P> {
    public MergingListField {
        if (field instanceof NullableField) {
            throw new IllegalArgumentException("Merging list field does not support nulls, use a defaulting field with an appropriate default instead");
        }
    }

    @Override
    public List<T> get(JsonObject json) {
        if (json.has(this.key)) {
            JsonArray array = JsonHelper.getArray(json, this.key);
            // using an array list as immutable lists do not support null, but for our uses we need to allow null
            ImmutableList.Builder<T> builder = ImmutableList.builder();
            for (int i = 0; i < array.size(); i++) {
                JsonObject element = JsonHelper.asObject(array.get(i), this.key + '[' + i + ']');
                builder.add(this.field.get(element));
            }
            return builder.build();
        }
        return List.of();
    }

    @Override
    public void serialize(P parent, JsonObject json) {
        List<T> objects = this.getter.apply(parent);
        if (json.has(this.key)) {
            JsonArray array = JsonHelper.getArray(json, this.key);
            int size = objects.size();
            if (array.size() < size) {
                throw new RuntimeException("Too many elements in list to merge into parent: have " + size + " but limited to " + array.size());
            }
            for (int i = 0; i < size; i++) {
                JsonObject element = JsonHelper.asObject(array.get(i), this.key + '[' + i + ']');
                this.field.serialize(objects.get(i), element);
            }
        }
    }

    @Override
    public List<T> decode(PacketByteBuf buffer) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            builder.add(this.field.decode(buffer));
        }
        return builder.build();
    }

    @Override
    public void encode(PacketByteBuf buffer, P parent) {
        List<T> list = this.getter.apply(parent);
        buffer.writeVarInt(list.size());
        for (T value : list) {
            this.field.encode(buffer, value);
        }
    }
}
