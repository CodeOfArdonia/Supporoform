package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;

import java.util.Locale;
import java.util.function.Function;

/**
 * Loadable for a boolean
 */
public enum BooleanLoadable implements StringLoadable<Boolean> {
    INSTANCE;

    @Override
    public Boolean convert(JsonElement element, String key) {
        return JsonHelper.asBoolean(element, key);
    }

    @Override
    public JsonElement serialize(Boolean object) {
        return new JsonPrimitive(object);
    }

    @Override
    public Boolean decode(PacketByteBuf buffer) {
        return buffer.readBoolean();
    }

    @Override
    public void encode(PacketByteBuf buffer, Boolean object) {
        buffer.writeBoolean(object);
    }

    @Override
    public <P> LoadableField<Boolean, P> defaultField(String key, Boolean defaultValue, Function<P, Boolean> getter) {
        // booleans are cleaner if they serialize by default
        return this.defaultField(key, defaultValue, true, getter);
    }


    /* String loadable */

    @Override
    public Boolean parseString(String value, String key) {
        // Boolean#valueOf and Boolean#parseBoolean both just treat all non-true as false, which is less desirable for well-formed JSON
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new JsonSyntaxException("Invalid boolean '" + value + '\'');
        };
    }

    @Override
    public String getString(Boolean object) {
        return object.toString();
    }
}
