package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;

/**
 * Record field that always returns the same value, used mainly to pass a different object in JSON vs buffer parsing
 */
public record ConstantField<T>(T fromJson, T fromBuffer) implements LoadableField<T, Object> {
    public ConstantField(T value) {
        this(value, value);
    }

    @Override
    public T get(JsonObject json) {
        return this.fromJson;
    }

    @Override
    public T decode(PacketByteBuf buffer) {
        return this.fromBuffer;
    }

    @Override
    public void serialize(Object parent, JsonObject json) {
    }

    @Override
    public void encode(PacketByteBuf buffer, Object parent) {
    }
}
