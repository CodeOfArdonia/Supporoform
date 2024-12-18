package slimeknights.tconstruct.tools.stats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.text.Text;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.materials.stats.IRepairableMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

/**
 * Stats for melee/harvest head stats
 */
public record HeadMaterialStats(int durability, float miningSpeed, ToolMaterial tier,
                                float attack) implements IRepairableMaterialStats {
    public static final MaterialStatsId ID = new MaterialStatsId(TConstruct.getResource("head"));
    public static final MaterialStatType<HeadMaterialStats> TYPE = new MaterialStatType<>(ID, new HeadMaterialStats(1, 1f, ToolMaterials.WOOD, 1f), RecordLoadable.create(
            IRepairableMaterialStats.DURABILITY_FIELD,
            FloatLoadable.FROM_ZERO.defaultField("mining_speed", 1f, true, HeadMaterialStats::miningSpeed),
            TinkerLoadables.TIER.defaultField("mining_tier", ToolMaterials.WOOD, true, HeadMaterialStats::tier),
            FloatLoadable.FROM_ZERO.defaultField("melee_attack", 1f, true, HeadMaterialStats::attack),
            HeadMaterialStats::new));

    // tooltip descriptions
    private static final List<Text> DESCRIPTION = ImmutableList.of(ToolStats.DURABILITY.getDescription(), ToolStats.HARVEST_TIER.getDescription(), ToolStats.MINING_SPEED.getDescription(), ToolStats.ATTACK_DAMAGE.getDescription());

    @Override
    public MaterialStatType<?> getType() {
        return TYPE;
    }

    @Override
    public List<Text> getLocalizedInfo() {
        List<Text> info = Lists.newArrayList();
        info.add(ToolStats.DURABILITY.formatValue(this.durability));
        info.add(ToolStats.HARVEST_TIER.formatValue(this.tier));
        info.add(ToolStats.MINING_SPEED.formatValue(this.miningSpeed));
        info.add(ToolStats.ATTACK_DAMAGE.formatValue(this.attack));
        return info;
    }

    @Override
    public List<Text> getLocalizedDescriptions() {
        return DESCRIPTION;
    }

    @Override
    public void apply(ModifierStatsBuilder builder, float scale) {
        // update for floats cancels out the base stats the first time used, makes the behavior more predictable between this and the stats module
        ToolStats.DURABILITY.update(builder, this.durability * scale);
        ToolStats.ATTACK_DAMAGE.update(builder, this.attack * scale);
        ToolStats.MINING_SPEED.update(builder, this.miningSpeed * scale);
        // no need to scale tier, we just take the max across everything
        ToolStats.HARVEST_TIER.update(builder, this.tier);
    }
}
