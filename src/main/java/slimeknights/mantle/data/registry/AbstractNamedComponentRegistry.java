package slimeknights.mantle.data.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;

import java.util.Collection;
import java.util.function.Function;

/**
 * Shared logic for registries that map a resource location to an object.
 */
public abstract class AbstractNamedComponentRegistry<T> implements ResourceLocationLoadable<T> {
    /**
     * Name to make exceptions clearer
     */
    protected final String errorText;

    public AbstractNamedComponentRegistry(String errorText) {
        this.errorText = errorText + " ";
    }

    /**
     * Gets a value or null if missing
     */
    @Nullable
    public abstract T getValue(Identifier name);

    /**
     * Gets all keys registered
     */
    public abstract Collection<Identifier> getKeys();

    /**
     * Gets all keys registered
     */
    public abstract Collection<T> getValues();


    /* Json */

    @Override
    public T fromKey(Identifier name, String key) {
        T value = this.getValue(name);
        if (value != null) {
            return value;
        }
        throw new JsonSyntaxException(this.errorText + name + " at '" + key + '\'');
    }


    /* Network */

    /**
     * Writes the value to the buffer
     */
    @Override
    public void encode(PacketByteBuf buffer, T value) {
        buffer.writeIdentifier(this.getKey(value));
    }

    /**
     * Writes the value to the buffer
     */
    public void encodeOptional(PacketByteBuf buffer, @Nullable T value) {
        // if null, just write an empty string, that is not a valid resource location anyways and saves us a byte
        if (value != null) {
            buffer.writeString(this.getKey(value).toString());
        } else {
            buffer.writeString("");
        }
    }

    /**
     * Reads the given value from the network by resource location
     */
    private T decodeInternal(Identifier name) {
        T value = this.getValue(name);
        if (value == null) {
            throw new DecoderException(this.errorText + name);
        }
        return value;
    }

    /**
     * Parse the value from JSON
     */
    @Override
    public T decode(PacketByteBuf buffer) {
        return this.decodeInternal(buffer.readIdentifier());
    }

    /**
     * Parse the value from JSON
     */
    @Nullable
    public T decodeOptional(PacketByteBuf buffer) {
        // empty string is not a valid resource location, so its a nice value to use for null, saves us a byte
        String key = buffer.readString(Short.MAX_VALUE);
        if (key.isEmpty()) {
            return null;
        }
        return this.decodeInternal(new Identifier(key));
    }


    /* Fields */

    @Override
    public <P> LoadableField<T, P> nullableField(String key, Function<P, T> getter) {
        return new NullableField<>(this, key, getter);
    }

    /**
     * Custom implementation of nullable field using our networking optional logic
     */
    private record NullableField<T, P>(AbstractNamedComponentRegistry<T> registry, String key,
                                       Function<P, T> getter) implements LoadableField<T, P> {
        @Nullable
        @Override
        public T get(JsonObject json) {
            return this.registry.getOrDefault(json, this.key, null);
        }

        @Override
        public void serialize(P parent, JsonObject json) {
            T object = this.getter.apply(parent);
            if (object != null) {
                json.add(this.key, this.registry.serialize(object));
            }
        }

        @Nullable
        @Override
        public T decode(PacketByteBuf buffer) {
            return this.registry.decodeOptional(buffer);
        }

        @Override
        public void encode(PacketByteBuf buffer, P parent) {
            this.registry.encodeOptional(buffer, this.getter.apply(parent));
        }
    }
}
