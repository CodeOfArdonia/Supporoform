package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Function4;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Record loadable with 4 fields
 */
record RecordLoadable4<A, B, C, D, R>(
        RecordField<A, ? super R> fieldA,
        RecordField<B, ? super R> fieldB,
        RecordField<C, ? super R> fieldC,
        RecordField<D, ? super R> fieldD,
        Function4<A, B, C, D, R> constructor
) implements RecordLoadable<R> {
    @Override
    public R deserialize(JsonObject json, TypedMap context) {
        return this.constructor.apply(
                this.fieldA.get(json, context),
                this.fieldB.get(json, context),
                this.fieldC.get(json, context),
                this.fieldD.get(json, context)
        );
    }

    @Override
    public void serialize(R object, JsonObject json) {
        this.fieldA.serialize(object, json);
        this.fieldB.serialize(object, json);
        this.fieldC.serialize(object, json);
        this.fieldD.serialize(object, json);
    }

    @Override
    public R decode(PacketByteBuf buffer, TypedMap context) {
        return this.constructor.apply(
                this.fieldA.decode(buffer, context),
                this.fieldB.decode(buffer, context),
                this.fieldC.decode(buffer, context),
                this.fieldD.decode(buffer, context)
        );
    }

    @Override
    public void encode(PacketByteBuf buffer, R object) {
        this.fieldA.encode(buffer, object);
        this.fieldB.encode(buffer, object);
        this.fieldC.encode(buffer, object);
        this.fieldD.encode(buffer, object);
    }
}
