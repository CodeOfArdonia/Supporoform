package slimeknights.mantle.data.loadable.mapping;

import com.google.common.collect.ImmutableCollection;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.Loadable;

import java.util.Collection;

/**
 * Shared base class for a loadable of a collection of elements
 */
@RequiredArgsConstructor
public abstract class CollectionLoadable<T, C extends Collection<T>, B extends ImmutableCollection.Builder<T>> implements Loadable<C> {
    /**
     * Loadable for an object
     */
    private final Loadable<T> base;
    /**
     * If true, empty is an allowed value
     */
    private final int minSize;

    /**
     * Creates a builder for the collection
     */
    protected abstract B makeBuilder();

    /**
     * Builds the final collection
     */
    protected abstract C build(B builder);

    @Override
    public C convert(JsonElement element, String key) {
        JsonArray array = JsonHelper.asArray(element, key);
        if (array.size() < this.minSize) {
            throw new JsonSyntaxException(key + " must have at least " + this.minSize + " elements");
        }
        B builder = this.makeBuilder();
        for (int i = 0; i < array.size(); i++) {
            builder.add(this.base.convert(array.get(i), key + '[' + i + ']'));
        }
        return this.build(builder);
    }

    @Override
    public JsonArray serialize(C collection) {
        if (collection.size() < this.minSize) {
            throw new RuntimeException("Collection must have at least " + this.minSize + " elements");
        }
        JsonArray array = new JsonArray();
        for (T element : collection) {
            array.add(this.base.serialize(element));
        }
        return array;
    }

    @Override
    public C decode(PacketByteBuf buffer) {
        B builder = this.makeBuilder();
        int max = buffer.readVarInt();
        for (int i = 0; i < max; i++) {
            builder.add(this.base.decode(buffer));
        }
        return this.build(builder);
    }

    @Override
    public void encode(PacketByteBuf buffer, C collection) {
        buffer.writeVarInt(collection.size());
        for (T element : collection) {
            this.base.encode(buffer, element);
        }
    }
}
