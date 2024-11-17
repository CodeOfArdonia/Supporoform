package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Loadable for reading NBT, converting from a JSON object to a tag.
 */
public enum NBTLoadable implements RecordLoadable<NbtCompound> {
    /**
     * Disallows reading NBT from a string in the Forge style
     */
    DISALLOW_STRING,
    /**
     * Allows reading NBT from a string in the forge style
     */
    ALLOW_STRING;

    @Override
    public NbtCompound deserialize(JsonObject json, TypedMap context) {
        return (NbtCompound) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
    }

    @Override
    public NbtCompound convert(JsonElement element, String key) {
        if (this == ALLOW_STRING && !element.isJsonObject()) {
            try {
                return StringNbtReader.parse(JsonHelper.DEFAULT_GSON.toJson(element));
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("Invalid NBT Entry: ", e);
            }
        }
        return RecordLoadable.super.convert(element, key);
    }

    @Override
    public JsonObject serialize(NbtCompound object) {
        return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, object).getAsJsonObject();
    }

    @Override
    public void serialize(NbtCompound object, JsonObject json) {
        json.entrySet().addAll(this.serialize(object).entrySet());
    }

    @Override
    public NbtCompound decode(PacketByteBuf buffer, TypedMap context) {
        NbtCompound tag = buffer.readNbt();
        if (tag == null) {
            return new NbtCompound();
        }
        return tag;
    }

    @Override
    public void encode(PacketByteBuf buffer, NbtCompound object) {
        buffer.writeNbt(object);
    }

    @Override
    public <P> LoadableField<NbtCompound, P> nullableField(String key, Function<P, NbtCompound> getter) {
        return new NullableNBTField<>(this, key, getter);
    }


    /**
     * Special implementation of nullable field to compact the buffer since it natively handles nullable NBT
     */
    private record NullableNBTField<P>(Loadable<NbtCompound> loadable, String key,
                                       Function<P, NbtCompound> getter) implements LoadableField<NbtCompound, P> {
        @Nullable
        @Override
        public NbtCompound get(JsonObject json) {
            return this.loadable.getOrDefault(json, this.key, null);
        }

        @Override
        public void serialize(P parent, JsonObject json) {
            NbtCompound nbt = this.getter.apply(parent);
            if (nbt != null) {
                json.add(this.key, this.loadable.serialize(nbt));
            }
        }

        @Nullable
        @Override
        public NbtCompound decode(PacketByteBuf buffer) {
            return buffer.readNbt();
        }

        @Override
        public void encode(PacketByteBuf buffer, P parent) {
            buffer.writeNbt(this.getter.apply(parent));
        }
    }
}
