package slimeknights.tconstruct.library.tools.stat;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * Tool stat that keeps the largest integer value given
 */
@AllArgsConstructor
public class MaxToolStat implements IToolStat<Integer> {
    @Getter
    private final ToolStatId name;
    private final int defaultValue;
    private final IntFunction<Text> displayName;
    @Nullable
    private final TagKey<Item> tag;

    public MaxToolStat(ToolStatId name, int defaultValue, IntFunction<Text> displayName) {
        this(name, defaultValue, displayName, null);
    }

    @Override
    public boolean supports(Item item) {
        return tag == null || RegistryHelper.contains(tag, item);
    }

    @Override
    public Integer getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Integer clamp(Integer value) {
        return MathHelper.clamp(value, 0, Integer.MAX_VALUE);
    }

    @Override
    public TierBuilder makeBuilder() {
        return new TierBuilder();
    }

    /**
     * Sets the tier to the new tier, keeping the largest
     *
     * @param builder Builder
     * @param value   Value
     */
    @Override
    public void update(ModifierStatsBuilder builder, Integer value) {
        builder.<TierBuilder>updateStat(this, b -> b.tier = Math.max(b.tier, value));
    }

    @Override
    public Integer build(ModifierStatsBuilder parent, Object builder) {
        return ((TierBuilder) builder).tier;
    }

    @Nullable
    @Override
    public Integer read(NbtElement tag) {
        if (TagUtil.isNumeric(tag)) {
            return ((AbstractNbtNumber) tag).intValue();
        }
        return null;
    }

    @Nullable
    @Override
    public NbtElement write(Integer value) {
        return NbtInt.of(value);
    }

    @Override
    public Integer deserialize(JsonElement json) {
        return JsonHelper.asInt(json, getName().toString());
    }

    @Override
    public JsonElement serialize(Integer value) {
        return new JsonPrimitive(value);
    }

    @Override
    public Integer fromNetwork(PacketByteBuf buffer) {
        return buffer.readVarInt();
    }

    @Override
    public void toNetwork(PacketByteBuf buffer, Integer value) {
        buffer.writeVarInt(value);
    }

    @Override
    public Text formatValue(Integer number) {
        return Text.translatable(Util.makeTranslationKey("tool_stat", getName())).append(this.displayName.apply(number));
    }

    @Override
    public String toString() {
        return "TierToolStat{" + this.name + '}';
    }

    /**
     * Internal int storage, basically just a int wrapper
     */
    protected static class TierBuilder {
        private int tier = 0;
    }
}
