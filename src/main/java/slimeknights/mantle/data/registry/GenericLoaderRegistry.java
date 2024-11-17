package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.gson.GenericRegisteredSerializer;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

import java.util.function.Function;

/**
 * Generic registry for an object that can both be sent over a friendly byte buffer and serialized into JSON.
 *
 * @param <T> Type of the serializable object
 * @see GenericRegisteredSerializer GenericRegisteredSerializer for an alternative that does not need to handle network syncing
 * @see DefaultingLoaderRegistry
 */
@SuppressWarnings("unused")  // API
public class GenericLoaderRegistry<T extends IHaveLoader> implements Loadable<T> {
    /**
     * Empty object instance for compact deserialization
     */
    protected static final JsonObject EMPTY_OBJECT = new JsonObject();

    /**
     * Display name for this registry
     */
    @Getter
    private final String name;
    /**
     * Map of all serializers for implementations
     */
    protected final NamedComponentRegistry<IGenericLoader<? extends T>> loaders;
    /**
     * If true, single key serializations will not use a JSON object to serialize, ideal for loaders with many singletons
     */
    protected final boolean compact;

    public GenericLoaderRegistry(String name, boolean compact) {
        this.name = name;
        this.compact = compact;
        this.loaders = new NamedComponentRegistry<>("Unknown " + name + " loader");
    }

    /**
     * Registers a deserializer by name
     */
    public void register(Identifier name, IGenericLoader<? extends T> loader) {
        this.loaders.register(name, loader);
    }

    @Override
    public T convert(JsonElement element, String key) {
        // first try object
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return this.loaders.getIfPresent(object, "type").deserialize(object);
        }
        // try primitive if allowed
        if (this.compact && element.isJsonPrimitive()) {
            EMPTY_OBJECT.entrySet().clear();
            return this.loaders.convert(element, "type").deserialize(EMPTY_OBJECT);
        }
        // neither? failed to parse
        throw new JsonSyntaxException("Invalid " + this.name + " JSON at " + key + ", must be a JSON object" + (this.compact ? " or a string" : ""));
    }

    /**
     * Deserializes the object from JSON
     *
     * @param element JSON element
     * @return Deserialized object
     */
    public T deserialize(JsonElement element) {
        return this.convert(element, "[unknown]");
    }

    /**
     * Serializes the object to json, fighting generics
     */
    @SuppressWarnings("unchecked")
    private <L extends IHaveLoader> JsonElement serialize(IGenericLoader<L> loader, T src) {
        JsonObject json = new JsonObject();
        JsonElement type = new JsonPrimitive(this.loaders.getKey((IGenericLoader<? extends T>) loader).toString());
        json.add("type", type);
        loader.serialize((L) src, json);
        if (json.get("type") != type) {
            throw new IllegalStateException(this.name + " serializer " + type.getAsString() + " modified the type key, this is not allowed as it breaks deserialization");
        }
        // nothing to serialize? use type directly
        if (this.compact && json.entrySet().size() == 1) {
            return type;
        }
        return json;
    }

    @Override
    public JsonElement serialize(T src) {
        return this.serialize(src.getLoader(), src);
    }

    /**
     * Writes the object to the network, fighting generics
     */
    @SuppressWarnings("unchecked")
    protected <L extends IHaveLoader> void toNetwork(IGenericLoader<L> loader, T src, PacketByteBuf buffer) {
        loader.toNetwork((L) src, buffer);
    }

    @SuppressWarnings("unchecked")
    // the cast is safe here as its just doing a map lookup, shouldn't cause harm if it fails. Besides, the loader has to extend T to work
    @Override
    public void encode(PacketByteBuf buffer, T src) {
        this.loaders.encode(buffer, (IGenericLoader<? extends T>) src.getLoader());
        this.toNetwork(src.getLoader(), src, buffer);
    }

    @Override
    public T decode(PacketByteBuf buffer) {
        return this.loaders.decode(buffer).fromNetwork(buffer);
    }

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
     * Creates a field that loads this object directly into the parent JSON object, will conflict if the parent already has a type
     */
    public <P> LoadableField<T, P> directField(Function<P, T> getter) {
        return new DirectRegistryField<>(this, getter);
    }

    /**
     * Creates a field that loads this object directly into the parent JSON object by mapping the type key
     */
    public <P> LoadableField<T, P> directField(String typeKey, Function<P, T> getter) {
        return new MergingRegistryField<>(this, typeKey, getter);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "('" + this.name + "')";
    }

    /**
     * @deprecated use {@link slimeknights.mantle.data.loadable.record.RecordLoadable}. Will fully replace it in 1.20.
     */
    @Deprecated
    public interface IGenericLoader<T> {
        /**
         * Deserializes the object from json
         */
        T deserialize(JsonObject json);

        /**
         * Reads the object from the packet buffer
         */
        T fromNetwork(PacketByteBuf buffer);

        /**
         * Writes this object to json
         */
        void serialize(T object, JsonObject json);

        /**
         * Writes this object to the packet buffer
         */
        void toNetwork(T object, PacketByteBuf buffer);
    }

    /**
     * Interface for an object with a loader.
     * TODO 1.20: replace with {@link slimeknights.mantle.data.loadable.IAmLoadable.Record}
     */
    public interface IHaveLoader {
        /**
         * Gets the loader for the object.
         * If you wish to suppress the deprecation warning, change the return type to {@link slimeknights.mantle.data.loadable.record.RecordLoadable}.
         */
        IGenericLoader<? extends IHaveLoader> getLoader();
    }

    /**
     * Loader instance for an object with only a single implementation
     */
    @RequiredArgsConstructor
    public static class SingletonLoader<T> implements IGenericLoader<T> {
        @Getter
        private final T instance;

        /**
         * Helper for creating a loader using an anonymous class
         */
        public SingletonLoader(Function<IGenericLoader<T>, T> creator) {
            this.instance = creator.apply(this);
        }

        @Override
        public T deserialize(JsonObject json) {
            return this.instance;
        }

        @Override
        public T fromNetwork(PacketByteBuf buffer) {
            return this.instance;
        }

        @Override
        public void serialize(T object, JsonObject json) {
        }

        @Override
        public void toNetwork(T object, PacketByteBuf buffer) {
        }

        /**
         * Helper to create a singleton object as an anonymous class
         */
        public static <T> T singleton(Function<IGenericLoader<T>, T> instance) {
            return new SingletonLoader<>(instance).getInstance();
        }
    }
}
