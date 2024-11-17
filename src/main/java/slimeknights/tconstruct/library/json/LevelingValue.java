package slimeknights.tconstruct.library.json;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Represents a float value that has a part that scales with level and a part that does not scale.
 *
 * @param flat      Value that does not change with level
 * @param eachLevel Value that changes each level
 * @see RandomLevelingValue
 */
public record LevelingValue(float flat, float eachLevel) {
    /**
     * Loadable instance for parsing
     */
    public static final RecordLoadable<LevelingValue> LOADABLE = RecordLoadable.create(
            FloatLoadable.ANY.defaultField("flat", 0f, LevelingValue::flat),
            FloatLoadable.ANY.defaultField("each_level", 0f, LevelingValue::eachLevel),
            LevelingValue::new);

    /**
     * Computes the value for the given level
     */
    public float compute(float level) {
        return this.flat + this.eachLevel * level;
    }


    /* JSON */

    /**
     * Serializes this to JSON
     */
    public JsonObject serialize(JsonObject json) {
        LOADABLE.serialize(this, json);
        return json;
    }

    /**
     * Deserializes this from JSON
     */
    public static LevelingValue deserialize(JsonObject json) {
        return LOADABLE.deserialize(json);
    }


    /* Network */

    /**
     * Writes this to the network
     */
    public void toNetwork(PacketByteBuf buffer) {
        LOADABLE.encode(buffer, this);
    }

    /**
     * Reads this from teh network
     */
    public static LevelingValue fromNetwork(PacketByteBuf buffer) {
        return LOADABLE.decode(buffer);
    }


    /* Construction */

    /**
     * Creates a value that stays constant each level
     */
    public static LevelingValue flat(float flat) {
        return new LevelingValue(flat, 0);
    }

    /**
     * Creates a value that is multiplied by the level
     */
    public static LevelingValue eachLevel(float eachLevel) {
        return new LevelingValue(0, eachLevel);
    }

    /**
     * Trait to mix into a builder using leveling values
     */
    public interface Builder<M> {
        /**
         * Creates an instance with a flat value and a leveling value
         */
        M amount(float flat, float eachLevel);

        /**
         * Creates an instance with a value that ignores level
         */
        default M flat(float flat) {
            return this.amount(flat, 0);
        }

        /**
         * Creates an instance with a value that increases each level
         */
        default M eachLevel(float eachLevel) {
            return this.amount(0, eachLevel);
        }
    }
}
