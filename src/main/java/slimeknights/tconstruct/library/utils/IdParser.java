package slimeknights.tconstruct.library.utils;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.EncoderException;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import org.jetbrains.annotations.Nullable;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.util.function.Function;

/**
 * Helper to parse variants of resource locations, doubles as a loadable.
 */
public record IdParser<T extends Identifier>(Function<String, T> constructor,
                                             String name) implements StringLoadable<T> {
    /**
     * Creates a new ID from the given string
     *
     * @param string String
     * @return ID, or null if invalid
     */
    @Nullable
    public T tryParse(String string) {
        try {
            return constructor.apply(string);
        } catch (InvalidIdentifierException resourcelocationexception) {
            return null;
        }
    }

    @Override
    public T parseString(String text, String key) {
        T location = tryParse(text);
        if (location == null) {
            throw new JsonSyntaxException("Expected " + key + " to be a " + this.name + " ID, was '" + text + "'");
        }
        return location;
    }

    @Override
    public String getString(T object) {
        return object.toString();
    }

    @Override
    public T decode(PacketByteBuf buf) {
        return this.constructor.apply(buf.readString(Short.MAX_VALUE));
    }

    @Override
    public void encode(PacketByteBuf buffer, T object) throws EncoderException {
        buffer.writeIdentifier(object);
    }
}
