package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;

/**
 * Implementation of a loadable using a codec. Note this will be inefficient when reading from and writing to the network
 */
public record CodecLoadable<T>(DynamicOps<NbtElement> ops, Codec<T> codec) implements Loadable<T> {
    public CodecLoadable(Codec<T> codec) {
        this(NbtOps.INSTANCE, codec);
    }

    @Override
    public T convert(JsonElement element, String key) {
        return this.codec.parse(JsonOps.INSTANCE, element).getOrThrow(false, ErrorFactory.JSON_SYNTAX_ERROR);
    }

    @Override
    public JsonElement serialize(T object) {
        return this.codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow(false, ErrorFactory.RUNTIME);
    }

    @Override
    public T decode(PacketByteBuf buffer) {
        return buffer.decode(this.ops, this.codec);
    }

    @Override
    public void encode(PacketByteBuf buffer, T object) {
        buffer.encode(this.ops, this.codec, object);
    }
}
