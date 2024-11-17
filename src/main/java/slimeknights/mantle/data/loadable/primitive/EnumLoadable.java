package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonSyntaxException;
import net.minecraft.network.PacketByteBuf;

import java.util.Locale;

/**
 * Loadable for an enum value
 */
public record EnumLoadable<E extends Enum<E>>(Class<E> enumClass, E[] allowedValues) implements StringLoadable<E> {
    public EnumLoadable(Class<E> enumClass) {
        this(enumClass, enumClass.getEnumConstants());
    }

    @Override
    public E parseString(String name, String key) {
        for (E value : this.allowedValues) {
            if (value.name().toLowerCase(Locale.ROOT).equals(name)) {
                return value;
            }
        }
        throw new JsonSyntaxException("Invalid " + this.enumClass.getSimpleName() + " " + name);
    }

    @Override
    public String getString(E object) {
        return object.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public E decode(PacketByteBuf buffer) {
        return buffer.readEnumConstant(this.enumClass);
    }

    @Override
    public void encode(PacketByteBuf buffer, E object) {
        buffer.writeEnumConstant(object);
    }
}
