package slimeknights.mantle.datagen;

import net.minecraft.data.DataOutput;
import net.minecraft.data.server.tag.vanilla.VanillaFluidTagProvider;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

import java.util.concurrent.CompletableFuture;

import static slimeknights.mantle.datagen.MantleTags.Fluids.LAVA;
import static slimeknights.mantle.datagen.MantleTags.Fluids.WATER;

/**
 * Provider for tags added by mantle, generally not useful for other mods
 */
public class MantleFluidTagProvider extends VanillaFluidTagProvider {
    public MantleFluidTagProvider(DataOutput output, CompletableFuture<WrapperLookup> holders) {
        super(output, holders);
    }

    @Override
    protected void configure(WrapperLookup pProvider) {
        this.getOrCreateTagBuilder(WATER).add(Fluids.WATER, Fluids.FLOWING_WATER);
        this.getOrCreateTagBuilder(LAVA).add(Fluids.LAVA, Fluids.FLOWING_LAVA);
    }

    @Override
    public String getName() {
        return "Mantle Fluid Tag Provider";
    }
}
