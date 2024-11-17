package slimeknights.tconstruct.library.client.book.content;

import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.Identifier;

public class RangedMaterialContent extends AbstractMaterialContent {
    /**
     * Page ID for using this index directly
     */
    public static final Identifier ID = TConstruct.getResource("ranged_material");

    public RangedMaterialContent(MaterialVariantId materialVariant, boolean detailed) {
        super(materialVariant, detailed);
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Nullable
    @Override
    protected MaterialStatsId getStatType(int index) {
        return switch (index) {
            case 0 -> LimbMaterialStats.ID;
            case 1 -> GripMaterialStats.ID;
            case 2 -> StatlessMaterialStats.BOWSTRING.getIdentifier();
            default -> null;
        };
    }

    @Override
    protected String getTextKey(MaterialId material) {
        if (this.detailed) {
            String primaryKey = String.format("material.%s.%s.ranged", material.getNamespace(), material.getPath());
            if (Util.canTranslate(primaryKey)) {
                return primaryKey;
            }
            return String.format("material.%s.%s.encyclopedia", material.getNamespace(), material.getPath());
        }
        return String.format("material.%s.%s.flavor", material.getNamespace(), material.getPath());
    }

    @Override
    protected boolean supportsStatType(MaterialStatsId statsId) {
        return statsId.equals(LimbMaterialStats.ID) || statsId.equals(GripMaterialStats.ID) || statsId.equals(StatlessMaterialStats.BOWSTRING.getIdentifier());
    }
}
