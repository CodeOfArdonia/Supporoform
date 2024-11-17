package slimeknights.tconstruct.tools.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.part.IRepairKitItem;
import slimeknights.tconstruct.library.tools.part.MaterialItem;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import java.util.List;

public class RepairKitItem extends MaterialItem implements IRepairKitItem {
    public RepairKitItem(Settings properties) {
        super(properties);
    }

    @Override
    public boolean canUseMaterial(MaterialId material) {
        return MaterialRegistry.getInstance()
                .getAllStats(material)
                .stream()
                .anyMatch(stats -> stats == StatlessMaterialStats.REPAIR_KIT || stats.getType().canRepair());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flag) {
        if (flag.isAdvanced() && !TooltipUtil.isDisplay(stack)) {
            MaterialVariantId materialVariant = this.getMaterial(stack);
            if (!materialVariant.equals(IMaterial.UNKNOWN_ID)) {
                tooltip.add((Text.translatable(ToolPartItem.MATERIAL_KEY, materialVariant.toString())).formatted(Formatting.DARK_GRAY));
            }
        }
    }

    @Override
    public float getRepairAmount() {
        return Config.COMMON.repairKitAmount.get().floatValue();
    }
}
