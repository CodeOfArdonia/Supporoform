package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * A record loadable that loads directly into the parent instead of nesting.
 *
 * @param <P> Parent object
 * @param <T> Loadable type
 */
public record DirectField<T, P>(RecordLoadable<T> loadable,
                                Function<P, T> getter) implements AlwaysPresentLoadableField<T, P> {
    @Override
    public T get(JsonObject json) {
        return this.loadable.deserialize(json);
    }

    @Override
    public T get(JsonObject json, TypedMap context) {
        return this.loadable.deserialize(json, context);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
        this.loadable.serialize(this.getter.apply(parent), json);
    }

    @Override
    public T decode(PacketByteBuf buffer, TypedMap context) {
        return this.loadable.decode(buffer, context);
    }
}
