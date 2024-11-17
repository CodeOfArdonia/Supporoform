package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonSyntaxException;
import net.minecraft.network.PacketByteBuf;

/**
 * Implementation of a loadable for a string. Access through {@link StringLoadable#maxLength(int)}.
 *
 * @param maxLength Maximum length of string allowed
 */
record MaxLengthStringLoadable(int maxLength) implements StringLoadable<String> {
    @Override
    public String parseString(String value, String key) {
        if (value.length() > this.maxLength) {
            throw new JsonSyntaxException(key + " may not be longer than " + this.maxLength);
        }
        return value;
    }

    @Override
    public String getString(String object) {
        if (object.length() > this.maxLength) {
            throw new RuntimeException("String may not be longer than " + this.maxLength);
        }
        return object;
    }

    @Override
    public String decode(PacketByteBuf buffer) {
        return buffer.readString(this.maxLength);
    }

    @Override
    public void encode(PacketByteBuf buffer, String object) {
        buffer.writeString(object, this.maxLength);
    }
}
