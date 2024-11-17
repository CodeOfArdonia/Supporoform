package slimeknights.tconstruct.tools.stats;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.text.Text;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IRepairableMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.item.ArmorSlotType.ArmorShieldBuilder;

import java.util.List;

/**
 * Material stat class handling all four plating types
 */
public record PlatingMaterialStats(MaterialStatType<?> getType, int durability, float armor, float toughness,
                                   float knockbackResistance) implements IRepairableMaterialStats {
    private static final LoadableField<Float, PlatingMaterialStats> TOUGHNESS = FloatLoadable.FROM_ZERO.defaultField("toughness", 0f, PlatingMaterialStats::toughness);
    private static final LoadableField<Float, PlatingMaterialStats> KNOCKBACK_RESISTANCE = FloatLoadable.FROM_ZERO.defaultField("knockback_resistance", 0f, PlatingMaterialStats::knockbackResistance);
    private static final RecordLoadable<PlatingMaterialStats> LOADABLE = RecordLoadable.create(
            MaterialStatType.CONTEXT_KEY.requiredField(),
            IRepairableMaterialStats.DURABILITY_FIELD,
            FloatLoadable.FROM_ZERO.defaultField("armor", 0f, true, PlatingMaterialStats::armor),
            TOUGHNESS,
            KNOCKBACK_RESISTANCE,
            PlatingMaterialStats::new);
    private static final List<Text> DESCRIPTION = List.of(
            ToolStats.DURABILITY.getDescription(),
            ToolStats.ARMOR.getDescription(),
            ToolStats.ARMOR_TOUGHNESS.getDescription(),
            ToolStats.KNOCKBACK_RESISTANCE.getDescription());
    private static final List<Text> SHIELD_DESCRIPTION = List.of(
            ToolStats.DURABILITY.getDescription(),
            ToolStats.ARMOR_TOUGHNESS.getDescription(),
            ToolStats.KNOCKBACK_RESISTANCE.getDescription());
    /* Types */
    public static final MaterialStatType<PlatingMaterialStats> HELMET = makeType("plating_helmet");
    public static final MaterialStatType<PlatingMaterialStats> CHESTPLATE = makeType("plating_chestplate");
    public static final MaterialStatType<PlatingMaterialStats> LEGGINGS = makeType("plating_leggings");
    public static final MaterialStatType<PlatingMaterialStats> BOOTS = makeType("plating_boots");
    /**
     * Shield loadable does not support armor
     */
    public static final MaterialStatType<PlatingMaterialStats> SHIELD = new MaterialStatType<PlatingMaterialStats>(new MaterialStatsId(TConstruct.MOD_ID, "plating_shield"), type -> new PlatingMaterialStats(type, 1, 0, 0, 0), RecordLoadable.create(
            MaterialStatType.CONTEXT_KEY.requiredField(), IRepairableMaterialStats.DURABILITY_FIELD, TOUGHNESS, KNOCKBACK_RESISTANCE,
            (type, durability, toughness, knockbackResistance) -> new PlatingMaterialStats(type, durability, 0, toughness, knockbackResistance)));
    /**
     * All types including shield
     */
    public static final List<MaterialStatType<PlatingMaterialStats>> TYPES = List.of(BOOTS, LEGGINGS, CHESTPLATE, HELMET, SHIELD);

    @Override
    public List<Text> getLocalizedInfo() {
        Text durability = ToolStats.DURABILITY.formatValue(this.durability);
        Text toughness = ToolStats.ARMOR_TOUGHNESS.formatValue(this.toughness);
        Text knockbackResistance = ToolStats.KNOCKBACK_RESISTANCE.formatValue(this.knockbackResistance * 10); // multiply by 10 as vanilla multiplies toughness by 10 for display
        if (this.getType == SHIELD) {
            return List.of(durability, toughness, knockbackResistance);
        }
        return List.of(durability, ToolStats.ARMOR.formatValue(this.armor), toughness, knockbackResistance);
    }

    @Override
    public List<Text> getLocalizedDescriptions() {
        return this.getType == SHIELD ? SHIELD_DESCRIPTION : DESCRIPTION;
    }

    @Override
    public void apply(ModifierStatsBuilder builder, float scale) {
        ToolStats.DURABILITY.update(builder, this.durability * scale);
        ToolStats.ARMOR.update(builder, this.armor * scale);
        ToolStats.ARMOR_TOUGHNESS.update(builder, this.toughness * scale);
        ToolStats.KNOCKBACK_RESISTANCE.update(builder, this.knockbackResistance * scale);
    }

    /**
     * Makes a stat type for the given name
     */
    private static MaterialStatType<PlatingMaterialStats> makeType(String name) {
        return new MaterialStatType<PlatingMaterialStats>(new MaterialStatsId(TConstruct.MOD_ID, name), type -> new PlatingMaterialStats(type, 1, 0, 0, 0), LOADABLE);
    }


    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create plating material stats for all four pieces
     */
    @Setter
    @Accessors(fluent = true)
    public static class Builder implements ArmorShieldBuilder<PlatingMaterialStats> {
        private final int[] durability = new int[4];
        private int shieldDurability = 0;
        private final float[] armor = new float[4];
        private float toughness = 0;
        private float knockbackResistance = 0;

        private Builder() {
        }

        /**
         * Sets the durability for the piece based on the given factor
         */
        public Builder durabilityFactor(float maxDamageFactor) {
            for (ArmorSlotType slotType : ArmorSlotType.values()) {
                int index = slotType.getIndex();
                this.durability[index] = (int) (ArmorSlotType.MAX_DAMAGE_ARRAY[index] * maxDamageFactor);
            }
            if (this.shieldDurability == 0) {
                this.shieldDurability = (int) (maxDamageFactor * 18);
            }
            return this;
        }

        /**
         * Sets the armor value for each piece
         */
        public Builder armor(float boots, float leggings, float chestplate, float helmet) {
            this.armor[ArmorSlotType.BOOTS.getIndex()] = boots;
            this.armor[ArmorSlotType.LEGGINGS.getIndex()] = leggings;
            this.armor[ArmorSlotType.CHESTPLATE.getIndex()] = chestplate;
            this.armor[ArmorSlotType.HELMET.getIndex()] = helmet;
            return this;
        }

        @Override
        public PlatingMaterialStats build(ArmorSlotType slot) {
            int index = slot.getIndex();
            return new PlatingMaterialStats(TYPES.get(index), this.durability[index], this.armor[index], this.toughness, this.knockbackResistance);
        }

        @Override
        public PlatingMaterialStats buildShield() {
            return new PlatingMaterialStats(PlatingMaterialStats.SHIELD, this.shieldDurability, 0, this.toughness, this.knockbackResistance);
        }
    }
}
