package slimeknights.tconstruct.library.tools.stat;

import com.google.gson.JsonElement;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;

import java.text.DecimalFormat;

/**
 * Interface for all tool stats, can implement to determine the behavior of stats in the modifier stat builder
 *
 * @param <T>
 */
public interface IToolStat<T> {

    /**
     * Gets the name of this stat for serializing to NBT
     */
    ToolStatId getName();

    /**
     * Gets the default value for this stat
     */
    T getDefaultValue();

    /**
     * Clamps the value into a valid range
     */
    default T clamp(T value) {
        return value;
    }

    /**
     * Checks if the given item supports this stat. Typically is just a tag check
     *
     * @param item Item to validate
     * @return True if the stat is supported
     */
    default boolean supports(Item item) {
        return true;
    }


    /* Modifier stat builder */

    /**
     * Creates a builder instance for this stat
     *
     * @return Stating value
     */
    Object makeBuilder();

    /**
     * Builds this stat using the given builder
     *
     * @param parent  Builder parent, allows fetching properties from teh parent
     * @param builder Builder object, will be the same object you returned in {@link #makeBuilder()} so unchecked casting is safe
     * @return Final float value
     */
    T build(ModifierStatsBuilder parent, Object builder);

    /**
     * Updates the stat with a new value. The stat can determine how to merge that with existing values
     *
     * @param builder Builder instance
     * @param value   Amount to add
     */
    void update(ModifierStatsBuilder builder, T value);


    /* Storing and parsing */

    /**
     * Parses this stat from NBT, return null if the type is invalid
     */
    @Nullable
    T read(NbtElement tag);

    /**
     * Writes this stat to NBT
     */
    @Nullable
    NbtElement write(T value);

    /**
     * Parses this stat from JSON
     */
    T deserialize(JsonElement json);

    /**
     * Serializes this stat to JSON
     */
    JsonElement serialize(T value);

    /**
     * Parses this stat from from the network
     */
    T fromNetwork(PacketByteBuf buffer);

    /**
     * Writes this stat to the network
     */
    void toNetwork(PacketByteBuf buffer, T value);


    /* Display */

    /**
     * Gets the prefix translation key for displaying stats
     */
    default String getTranslationKey() {
        return Util.makeTranslationKey("tool_stat", this.getName());
    }

    /**
     * Gets the prefix for this tool stat
     */
    default MutableText getPrefix() {
        return Text.translatable(this.getTranslationKey());
    }

    /**
     * Gets the description for this tool stat
     */
    default MutableText getDescription() {
        return Text.translatable(this.getTranslationKey() + ".description");
    }

    /**
     * Formats the value using this tool stat
     */
    Text formatValue(T value);


    /* Formatting helpers */

    /**
     * Creates a text component, coloring the number
     *
     * @param loc    Translation key
     * @param color  Color
     * @param number Number
     * @return Text component
     */
    static Text formatNumber(String loc, TextColor color, int number) {
        return formatNumber(loc, color, (float) number);
    }

    /**
     * Creates a text component, coloring the number
     *
     * @param loc    Translation key
     * @param color  Color
     * @param number Number
     * @return Text component
     */
    static Text formatNumber(String loc, TextColor color, float number) {
        return Text.translatable(loc)
                .append(Text.literal(Util.COMMA_FORMAT.format(number)).styled(style -> style.withColor(color)));
    }

    /**
     * Creates a text component, coloring the number as a percentage
     *
     * @param loc    Translation key
     * @param color  Color
     * @param number Number
     * @return Text component
     */
    static Text formatNumberPercent(String loc, TextColor color, float number) {
        return Text.translatable(loc)
                .append(Text.literal(Util.PERCENT_FORMAT.format(number)).styled(style -> style.withColor(color)));
    }

    /**
     * Formats with hue shifting
     *
     * @param loc    Prefix location
     * @param number Percentage
     * @param format Number formatter
     * @return Colored percent with prefix
     */
    static Text formatColored(String loc, float number, float offset, DecimalFormat format) {
        float hue = MathHelper.floorMod(offset + number, 2f);
        return Text.translatable(loc).append(Text.literal(format.format(number)).styled(style -> style.withColor(TextColor.fromRgb(MathHelper.hsvToRgb(hue / 1.5f, 1.0f, 0.75f)))));
    }

    /**
     * Formats a multiplier with hue shifting
     *
     * @param loc    Prefix location
     * @param number Percentage
     * @return Colored percent with prefix
     */
    static Text formatColoredMultiplier(String loc, float number) {
        // 0.5 is red, 1.0 should be roughly green, 1.5 is blue
        return formatColored(loc, number, -0.5f, Util.MULTIPLIER_FORMAT);
    }

    /**
     * Formats an additive bonus with hue shifting
     *
     * @param loc    Prefix location
     * @param number Percentage
     * @return Colored percent with prefix
     */
    static Text formatColoredBonus(String loc, float number) {
        // -0.5 is red, 0 should be roughly green, +0.5 is blue
        return formatColored(loc, number, 0.5f, Util.BONUS_FORMAT);
    }

    /**
     * Formats a percent boost with hue shifting
     *
     * @param loc    Prefix location
     * @param number Percentage
     * @return Colored percent with prefix
     */
    static Text formatColoredPercentBoost(String loc, float number) {
        // -0.5 is red, 0 should be roughly green, +0.5 is blue
        return formatColored(loc, number, 0.5f, Util.PERCENT_BOOST_FORMAT);
    }
}
