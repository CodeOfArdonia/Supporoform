package slimeknights.mantle.datagen;

import net.minecraft.data.DataOutput;
import net.minecraftforge.fluids.FluidType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.tooltip.AbstractFluidTooltipProvider;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;

/**
 * Mantle datagen for fluid tooltips. For mods, don't use this, use {@link AbstractFluidTooltipProvider}
 */
public class MantleFluidTooltipProvider extends AbstractFluidTooltipProvider {
    public MantleFluidTooltipProvider(DataOutput packOutput) {
        super(packOutput, Mantle.modId);
    }

    @Override
    protected void addFluids() {
        this.add("buckets")
                .addUnit("kilobucket", FluidType.BUCKET_VOLUME * 1000)
                .addUnit("bucket", FluidType.BUCKET_VOLUME);
        this.addRedirect(FluidTooltipHandler.DEFAULT_ID, this.id("buckets"));
    }

    @Override
    public String getName() {
        return "Mantle Fluid Tooltip Provider";
    }
}
