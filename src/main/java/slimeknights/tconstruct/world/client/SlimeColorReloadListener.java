package slimeknights.tconstruct.world.client;

import net.minecraft.client.util.RawTextureDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.fml.ModLoader;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.block.FoliageType;

import java.io.IOException;

/**
 * Color reload listener for all slime foliage types
 */
public class SlimeColorReloadListener extends SinglePreparationResourceReloader<int[]> {
    private final FoliageType color;
    private final Identifier path;

    public SlimeColorReloadListener(FoliageType color) {
        this.color = color;
        this.path = TConstruct.getResource("textures/colormap/" + color.asString() + "_grass_color.png");
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    @Override
    protected int[] prepare(ResourceManager resourceManager, Profiler profiler) {
        if (!ModLoader.isLoadingStateValid()) {
            return new int[0];
        }
        try {
            return RawTextureDataLoader.loadRawTextureData(resourceManager, this.path);
        } catch (IOException ioexception) {
            TConstruct.LOG.error("Failed to load slime colors", ioexception);
            return new int[0];
        }
    }

    @Override
    protected void apply(int[] buffer, ResourceManager resourceManager, Profiler profiler) {
        if (buffer.length != 0) {
            SlimeColorizer.setGrassColor(this.color, buffer);
        }
    }
}
