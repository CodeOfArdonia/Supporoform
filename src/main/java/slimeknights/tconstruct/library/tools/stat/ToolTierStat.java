package slimeknights.tconstruct.library.tools.stat;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.util.TierSortingRegistry;
import io.netty.handler.codec.DecoderException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.utils.HarvestTiers;
import slimeknights.tconstruct.library.utils.Util;

import java.util.Objects;

/**
 * Tool stat for comparing tool tiers
 */
@SuppressWarnings("ClassCanBeRecord")
@Getter
@RequiredArgsConstructor
public class ToolTierStat implements IToolStat<ToolMaterial> {
    /**
     * Name of this tool stat
     */
    private final ToolStatId name;

    @Override
    public boolean supports(Item item) {
        return RegistryHelper.contains(TinkerTags.Items.HARVEST, item);
    }

    @Override
    public ToolMaterial getDefaultValue() {
        return HarvestTiers.minTier();
    }

    @Override
    public Object makeBuilder() {
        return new TierBuilder(getDefaultValue());
    }

    @Override
    public ToolMaterial build(ModifierStatsBuilder parent, Object builder) {
        return ((TierBuilder) builder).value;
    }

    /**
     * Sets the tier to the new tier, keeping the largest
     *
     * @param builder Builder instance
     * @param value   Amount to add
     */
    @Override
    public void update(ModifierStatsBuilder builder, ToolMaterial value) {
        builder.<TierBuilder>updateStat(this, b -> b.value = HarvestTiers.max(b.value, value));
    }

    @Nullable
    @Override
    public ToolMaterial read(NbtElement tag) {
        if (tag.getType() == NbtElement.STRING_TYPE) {
            Identifier tierId = Identifier.tryParse(tag.asString());
            if (tierId != null) {
                return TierSortingRegistry.byName(tierId);
            }
        }
        return null;
    }

    @Override
    public NbtElement write(ToolMaterial value) {
        Identifier id = TierSortingRegistry.getName(value);
        if (id != null) {
            return NbtString.of(id.toString());
        }
        return null;
    }

    @Override
    public ToolMaterial deserialize(JsonElement json) {
        Identifier id = JsonHelper.convertToResourceLocation(json, getName().toString());
        ToolMaterial tier = TierSortingRegistry.byName(id);
        if (tier != null) {
            return tier;
        }
        throw new JsonSyntaxException("Unknown tool tier " + id);
    }

    @Override
    public JsonElement serialize(ToolMaterial value) {
        return new JsonPrimitive(Objects.requireNonNull(TierSortingRegistry.getName(value)).toString());
    }

    @Override
    public ToolMaterial fromNetwork(PacketByteBuf buffer) {
        Identifier id = buffer.readIdentifier();
        ToolMaterial tier = TierSortingRegistry.byName(id);
        if (tier != null) {
            return tier;
        }
        throw new DecoderException("Unknown tool tier " + id);
    }

    @Override
    public void toNetwork(PacketByteBuf buffer, ToolMaterial value) {
        buffer.writeIdentifier(Objects.requireNonNull(TierSortingRegistry.getName(value)));
    }

    @Override
    public Text formatValue(ToolMaterial value) {
        return Text.translatable(Util.makeTranslationKey("tool_stat", getName())).append(HarvestTiers.getName(value));
    }

    @Override
    public String toString() {
        return "ToolTierStat{" + this.name + '}';
    }

    /**
     * Builder for a tier object
     */
    @AllArgsConstructor
    private static class TierBuilder {
        private ToolMaterial value;
    }
}
