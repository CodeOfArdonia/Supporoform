package slimeknights.mantle.data.loadable.field;

import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.Loadable;

import java.util.function.Function;

/**
 * Common networking logic for loadables that always have a network value
 */
public interface AlwaysPresentLoadableField<T, P> extends LoadableField<T, P> {
    /**
     * Getter for the loadable
     */
    Loadable<T> loadable();

    /**
     * Getter for the given field
     */
    Function<P, T> getter();

    @Override
    default T decode(PacketByteBuf buffer) {
        return this.loadable().decode(buffer);
    }

    @Override
    default void encode(PacketByteBuf buffer, P parent) {
        this.loadable().encode(buffer, this.getter().apply(parent));
    }
}
