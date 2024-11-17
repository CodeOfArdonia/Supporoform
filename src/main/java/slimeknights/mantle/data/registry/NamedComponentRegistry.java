package slimeknights.mantle.data.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Generic registry of a component named by a resource location. Supports any arbitrary object without making any changes to it.
 *
 * @param <T> Type of the component being registered.
 */
public class NamedComponentRegistry<T> extends AbstractNamedComponentRegistry<T> {
    /**
     * Registered box expansion types
     */
    private final BiMap<Identifier, T> values = HashBiMap.create();

    public NamedComponentRegistry(String errorText) {
        super(errorText);
    }

    /**
     * Registers the value with the given name
     */
    public synchronized <V extends T> V register(Identifier name, V value) {
        if (this.values.putIfAbsent(name, value) != null) {
            throw new IllegalArgumentException("Duplicate registration " + name);
        }
        return value;
    }

    @Override
    @Nullable
    public T getValue(Identifier name) {
        return this.values.get(name);
    }

    /**
     * Gets the key associated with a value
     */
    @Nullable
    public Identifier getOptionalKey(T value) {
        return this.values.inverse().get(value);
    }

    @Override
    public Identifier getKey(T value) {
        Identifier key = this.getOptionalKey(value);
        if (key == null) {
            throw new IllegalStateException(this.errorText + value);
        }
        return key;
    }

    @Override
    public Collection<Identifier> getKeys() {
        return this.values.keySet();
    }

    @Override
    public Collection<T> getValues() {
        return this.values.values();
    }


    /* Deprecated aliases */

    /**
     * @deprecated use {@link #decode(PacketByteBuf)}
     */
    @Deprecated(forRemoval = true)
    public void toNetwork(T src, PacketByteBuf buffer) {
        this.encode(buffer, src);
    }

    /**
     * @deprecated use {@link #decode(PacketByteBuf)}
     */
    @Deprecated(forRemoval = true)
    public T fromNetwork(PacketByteBuf buffer) {
        return this.decode(buffer);
    }

    /**
     * @deprecated use {@link #decode(PacketByteBuf)}
     */
    @Deprecated(forRemoval = true)
    public void toNetworkOptional(@Nullable T src, PacketByteBuf buffer) {
        this.encodeOptional(buffer, src);
    }

    /**
     * @deprecated use {@link #decode(PacketByteBuf)}
     */
    @Nullable
    @Deprecated(forRemoval = true)
    public T fromNetworkOptional(PacketByteBuf buffer) {
        return this.decodeOptional(buffer);
    }
}
