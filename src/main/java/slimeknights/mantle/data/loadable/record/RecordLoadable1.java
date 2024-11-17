package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Record loadable with a single field
 */
record RecordLoadable1<A, R>(
        RecordField<A, ? super R> fieldA,
        Function<A, R> constructor
) implements RecordLoadable<R> {
    @Override
    public R deserialize(JsonObject json, TypedMap context) {
        return this.constructor.apply(this.fieldA.get(json, context));
    }

    @Override
    public void serialize(R object, JsonObject json) {
        this.fieldA.serialize(object, json);
    }

    @Override
    public R decode(PacketByteBuf buffer, TypedMap context) {
        return this.constructor.apply(this.fieldA.decode(buffer, context));
    }

    @Override
    public void encode(PacketByteBuf buffer, R object) {
        this.fieldA.encode(buffer, object);
    }
}
