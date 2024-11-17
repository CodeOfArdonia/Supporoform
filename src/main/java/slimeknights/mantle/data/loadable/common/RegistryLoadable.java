package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;
import slimeknights.mantle.util.RegistryHelper;

import java.util.Objects;

/**
 * Loadable for a registry entry
 */
public record RegistryLoadable<T>(Registry<T> registry, Identifier registryId) implements ResourceLocationLoadable<T> {
    public RegistryLoadable(RegistryKey<? extends Registry<T>> registryId) {
        this(Objects.requireNonNull(RegistryHelper.getRegistry(registryId), "Unknown registry " + registryId.getValue()), registryId.getValue());
    }

    @SuppressWarnings("unchecked")
    public RegistryLoadable(Registry<T> registry) {
        this(registry, ((Registry<Registry<?>>) Registries.REGISTRIES).getId(registry));
    }

    @Override
    public T fromKey(Identifier name, String key) {
        if (this.registry.containsId(name)) {
            T value = this.registry.get(name);
            if (value != null) {
                return value;
            }
        }
        throw new JsonSyntaxException("Unable to parse " + key + " as registry " + this.registryId + " does not contain ID " + name);
    }

    @Override
    public Identifier getKey(T object) {
        Identifier location = this.registry.getId(object);
        if (location == null) {
            throw new RuntimeException("Registry " + this.registryId + " does not contain object " + object);
        }
        return location;
    }

    @Override
    public T decode(PacketByteBuf buffer) {
        int id = buffer.readVarInt();
        T value = this.registry.get(id);
        if (value == null) {
            throw new DecoderException("Registry " + this.registryId + " does not contain ID " + id);
        }
        return value;
    }

    @Override
    public void encode(PacketByteBuf buffer, T object) {
        buffer.writeRegistryValue(this.registry, object);
    }
}
