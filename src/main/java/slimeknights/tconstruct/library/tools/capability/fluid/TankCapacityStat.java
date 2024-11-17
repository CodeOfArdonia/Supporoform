package slimeknights.tconstruct.library.tools.capability.fluid;

import net.minecraft.text.Text;
import slimeknights.mantle.Mantle;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.utils.Util;

/**
 * Tool stat formatting as millibuckets
 */
public class TankCapacityStat extends FloatToolStat {
    public static final String MB_FORMAT = Mantle.makeDescriptionId("gui", "fluid.millibucket");

    public TankCapacityStat(ToolStatId name, int color, float defaultValue, float maxValue) {
        super(name, color, defaultValue, 0, maxValue);
    }

    @Override
    public Text formatValue(float value) {
        return Text.translatable(getTranslationKey())
                .append(Text.translatable(MB_FORMAT, Util.COMMA_FORMAT.format(value))
                        .styled(style -> style.withColor(getColor())));
    }
}
