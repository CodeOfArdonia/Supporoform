package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Function9;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Record loadable with 9 fields
 */
@SuppressWarnings("DuplicatedCode")
record RecordLoadable9<A, B, C, D, E, F, G, H, I, R>(
        RecordField<A, ? super R> fieldA,
        RecordField<B, ? super R> fieldB,
        RecordField<C, ? super R> fieldC,
        RecordField<D, ? super R> fieldD,
        RecordField<E, ? super R> fieldE,
        RecordField<F, ? super R> fieldF,
        RecordField<G, ? super R> fieldG,
        RecordField<H, ? super R> fieldH,
        RecordField<I, ? super R> fieldI,
        Function9<A, B, C, D, E, F, G, H, I, R> constructor
) implements RecordLoadable<R> {
    @Override
    public R deserialize(JsonObject json, TypedMap context) {
        return this.constructor.apply(
                this.fieldA.get(json, context),
                this.fieldB.get(json, context),
                this.fieldC.get(json, context),
                this.fieldD.get(json, context),
                this.fieldE.get(json, context),
                this.fieldF.get(json, context),
                this.fieldG.get(json, context),
                this.fieldH.get(json, context),
                this.fieldI.get(json, context)
        );
    }

    @Override
    public void serialize(R object, JsonObject json) {
        this.fieldA.serialize(object, json);
        this.fieldB.serialize(object, json);
        this.fieldC.serialize(object, json);
        this.fieldD.serialize(object, json);
        this.fieldE.serialize(object, json);
        this.fieldF.serialize(object, json);
        this.fieldG.serialize(object, json);
        this.fieldH.serialize(object, json);
        this.fieldI.serialize(object, json);
    }

    @Override
    public R decode(PacketByteBuf buffer, TypedMap context) {
        return this.constructor.apply(
                this.fieldA.decode(buffer, context),
                this.fieldB.decode(buffer, context),
                this.fieldC.decode(buffer, context),
                this.fieldD.decode(buffer, context),
                this.fieldE.decode(buffer, context),
                this.fieldF.decode(buffer, context),
                this.fieldG.decode(buffer, context),
                this.fieldH.decode(buffer, context),
                this.fieldI.decode(buffer, context)
        );
    }

    @Override
    public void encode(PacketByteBuf buffer, R object) {
        this.fieldA.encode(buffer, object);
        this.fieldB.encode(buffer, object);
        this.fieldC.encode(buffer, object);
        this.fieldD.encode(buffer, object);
        this.fieldE.encode(buffer, object);
        this.fieldF.encode(buffer, object);
        this.fieldG.encode(buffer, object);
        this.fieldH.encode(buffer, object);
        this.fieldI.encode(buffer, object);
    }
}
