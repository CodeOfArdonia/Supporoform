package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.Loadable;

/**
 * Loadable for a float
 *
 * @param min Minimum allowed value
 * @param max Maximum allowed value
 */
public record FloatLoadable(float min, float max) implements Loadable<Float> {
    /**
     * Loadable ranging from negative infinity to positive infinity
     */
    public static final FloatLoadable ANY = range(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    /**
     * Loadable ranging from 0 to positive infinity
     */
    public static final FloatLoadable FROM_ZERO = min(0);
    /**
     * Loadable ranging from 0 to 1
     */
    public static final FloatLoadable PERCENT = range(0, 1);

    /**
     * Creates a loadable with the given range
     */
    public static FloatLoadable range(float min, float max) {
        return new FloatLoadable(min, max);
    }

    /**
     * Creates a loadable ranging from the parameter to short max
     */
    public static FloatLoadable min(float min) {
        return new FloatLoadable(min, Float.POSITIVE_INFINITY);
    }

    private float validate(float value, String key) {
        if (this.min <= value && value <= this.max) {
            return value;
        }
        if (this.min == Float.NEGATIVE_INFINITY) {
            throw new JsonSyntaxException(key + " must not be greater than " + this.max);
        }
        if (this.max == Float.POSITIVE_INFINITY) {
            throw new JsonSyntaxException(key + " must not be less than " + this.min);
        }
        throw new JsonSyntaxException(key + " must be between " + this.min + " and " + this.max);
    }

    @Override
    public Float convert(JsonElement element, String key) {
        return this.validate(JsonHelper.asFloat(element, key), key);
    }

    @Override
    public Float decode(PacketByteBuf buffer) {
        return buffer.readFloat();
    }

    @Override
    public JsonElement serialize(Float object) {
        return new JsonPrimitive(this.validate(object, "Value"));
    }

    @Override
    public void encode(PacketByteBuf buffer, Float object) {
        buffer.writeFloat(object);
    }
}
