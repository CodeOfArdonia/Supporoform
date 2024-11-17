package slimeknights.tconstruct.library.tools.part;

import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Extension of {@link MaterialItem} which adds stats to the tooltip and has a set stat type
 */
public class ToolPartItem extends MaterialItem implements IToolPart {
    private static final Text MISSING_INFO = TConstruct.makeTranslation("tooltip", "part.missing_info");
    private static final String MISSING_MATERIAL_KEY = TConstruct.makeTranslationKey("tooltip", "part.missing_material");
    private static final String MISSING_STATS_KEY = TConstruct.makeTranslationKey("tooltip", "part.missing_stats");
    public static final String MATERIAL_KEY = TConstruct.makeTranslationKey("tooltip", "part.material_id");

    public final MaterialStatsId materialStatId;

    public ToolPartItem(Settings properties, MaterialStatsId id) {
        super(properties);

        this.materialStatId = id;
    }

    @Override
    public MaterialStatsId getStatType() {
        return this.materialStatId;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flag) {
        if (TooltipUtil.isDisplay(stack)) {
            return;
        }

        // add all traits to the info
        MaterialVariantId materialVariant = this.getMaterial(stack);
        MaterialId id = materialVariant.getId();
        if (!materialVariant.equals(IMaterial.UNKNOWN_ID)) {
            if (flag.isAdvanced()) {
                tooltip.add((Text.translatable(MATERIAL_KEY, materialVariant.toString())).formatted(Formatting.DARK_GRAY));
            }
            if (this.canUseMaterial(id)) {
                for (ModifierEntry entry : MaterialRegistry.getInstance().getTraits(id, this.getStatType())) {
                    Text name = entry.getDisplayName();
                    if (flag.isAdvanced() && Config.CLIENT.modifiersIDsInAdvancedTooltips.get()) {
                        tooltip.add(Text.translatable(TooltipUtil.KEY_ID_FORMAT, name, Text.literal(entry.getModifier().getId().toString())).formatted(Formatting.DARK_GRAY));
                    } else {
                        tooltip.add(name);
                    }
                }
                // add stats
                if (Config.CLIENT.extraToolTips.get()) {
                    TooltipKey key = SafeClientAccess.getTooltipKey();
                    if (key == TooltipKey.SHIFT || key == TooltipKey.UNKNOWN) {
                        this.addStatInfoTooltip(id, tooltip);
                    } else {
                        // info tooltip for detailed and component info
                        tooltip.add(Text.empty());
                        tooltip.add(TooltipUtil.TOOLTIP_HOLD_SHIFT);
                    }
                }
            } else {
                // is the material missing, or is it not valid for this stat type?
                IMaterial material = MaterialRegistry.getMaterial(id);
                if (material == IMaterial.UNKNOWN) {
                    tooltip.add(Text.translatable(MISSING_MATERIAL_KEY, id));
                } else {
                    tooltip.add(Text.translatable(MISSING_STATS_KEY, this.materialStatId).formatted(Formatting.GRAY));
                }
            }
        }
        // mod handled by getCreatorModId
    }

    /**
     * Adds the stat info for the given part to the tooltip
     *
     * @param tooltip  Tooltip list
     * @param material Material to add
     */
    protected void addStatInfoTooltip(MaterialId material, List<Text> tooltip) {
        MaterialRegistry.getInstance().getMaterialStats(material, this.materialStatId).ifPresent((stat) -> {
            List<Text> text = stat.getLocalizedInfo();
            if (!text.isEmpty()) {
                tooltip.add(Text.empty());
                tooltip.add(stat.getLocalizedName().formatted(Formatting.WHITE, Formatting.UNDERLINE));
                tooltip.addAll(stat.getLocalizedInfo());
            }
        });
    }
}
