package slimeknights.tconstruct.library.tools.stat;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.library.utils.TagUtil;

import org.jetbrains.annotations.Nullable;

/**
 * Tool stat representing a float value, used for most numbers
 */
public class FloatToolStat implements INumericToolStat<Float> {
    /**
     * Name of this tool stat
     */
    @Getter
    private final ToolStatId name;
    /**
     * Color for this stat type
     */
    @Getter
    private final TextColor color;
    /**
     * Gets the default value for this stat
     */
    private final float defaultValue;
    /**
     * Min value for this stat
     */
    @Getter
    private final float minValue;
    /**
     * Max value for this stat
     */
    @Getter
    private final float maxValue;
    @Nullable
    private final TagKey<Item> tag;

    public FloatToolStat(ToolStatId name, int color, float defaultValue, float minValue, float maxValue, @Nullable TagKey<Item> tag) {
        this.name = name;
        this.color = TextColor.fromRgb(color);
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.tag = tag;
    }

    public FloatToolStat(ToolStatId name, int color, float defaultValue, float minValue, float maxValue) {
        this(name, color, defaultValue, minValue, maxValue, null);
    }

    @Override
    public boolean supports(Item item) {
        return tag == null || RegistryHelper.contains(tag, item);
    }

    @Override
    public Float getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Float clamp(Float value) {
        return MathHelper.clamp(value, getMinValue(), getMaxValue());
    }

    @Override
    public FloatBuilder makeBuilder() {
        return new FloatBuilder(defaultValue);
    }

    @Override
    public void update(ModifierStatsBuilder builder, Float value) {
        builder.<FloatBuilder>updateStat(this, b -> {
            b.add += value;
            b.base = 0;
        });
    }

    @Override
    public void add(ModifierStatsBuilder builder, double value) {
        builder.<FloatBuilder>updateStat(this, b -> b.add += value);
    }

    @Override
    public void percent(ModifierStatsBuilder builder, double factor) {
        builder.<FloatBuilder>updateStat(this, b -> b.percent += factor);
    }

    @Override
    public void multiply(ModifierStatsBuilder builder, double factor) {
        builder.<FloatBuilder>updateStat(this, b -> b.multiply *= factor);
    }

    @Override
    public void multiplyAll(ModifierStatsBuilder builder, double factor) {
        builder.<FloatBuilder>updateStat(this, b -> b.multiply *= factor);
        builder.multiplier(this, factor);
    }

    @Override
    public Float build(ModifierStatsBuilder parent, Object builderObj) {
        FloatBuilder builder = (FloatBuilder) builderObj;
        return (builder.base + builder.add) * (1 + builder.percent) * builder.multiply;
    }

    @Nullable
    @Override
    public Float read(NbtElement tag) {
        if (TagUtil.isNumeric(tag)) {
            return ((AbstractNbtNumber) tag).floatValue();
        }
        return null;
    }

    @Override
    public NbtElement write(Float value) {
        return NbtFloat.of(value);
    }

    @Override
    public Float deserialize(JsonElement json) {
        return JsonHelper.asFloat(json, getName().toString());
    }

    @Override
    public JsonElement serialize(Float value) {
        return new JsonPrimitive(value);
    }

    @Override
    public Float fromNetwork(PacketByteBuf buffer) {
        return buffer.readFloat();
    }

    @Override
    public void toNetwork(PacketByteBuf buffer, Float value) {
        buffer.writeFloat(value);
    }

    /**
     * Generic friendly way to format the value
     */
    @Override
    public Text formatValue(float value) {
        return IToolStat.formatNumber(getTranslationKey(), getColor(), value);
    }

    @Override
    public String toString() {
        return "FloatToolStat{" + this.name + '}';
    }

    /**
     * Internal builder to store the add and multiply value
     */
    protected static class FloatBuilder {
        /**
         * Base value of the stat, may get zeroed out
         */
        private float base;
        /**
         * Value summed with the base, applies first
         */
        private float add = 0;
        /**
         * Percent multiplier, applies second
         */
        private float percent = 0;
        /**
         * Value multiplied by the sum, applies second
         */
        private float multiply = 1;

        public FloatBuilder(float base) {
            this.base = base;
        }
    }
}
