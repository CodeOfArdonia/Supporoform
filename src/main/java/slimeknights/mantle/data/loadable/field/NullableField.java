package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.Loadable;

import java.util.function.Function;

/**
 * Optional field that may be null
 *
 * @param <P> Parent object
 * @param <T> Loadable type
 */
public record NullableField<T, P>(Loadable<T> loadable, String key,
                                  Function<P, T> getter) implements LoadableField<T, P> {
    @Override
    public T get(JsonObject json) {
        return this.loadable.getOrDefault(json, this.key, null);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
        T object = this.getter.apply(parent);
        if (object != null) {
            json.add(this.key, this.loadable.serialize(object));
        }
    }

    @Override
    public T decode(PacketByteBuf buffer) {
        if (buffer.readBoolean()) {
            return this.loadable.decode(buffer);
        }
        return null;
    }

    @Override
    public void encode(PacketByteBuf buffer, P parent) {
        T object = this.getter.apply(parent);
        if (object != null) {
            buffer.writeBoolean(true);
            this.loadable.encode(buffer, object);
        } else {
            buffer.writeBoolean(false);
        }
    }
}
