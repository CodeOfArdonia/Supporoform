package slimeknights.tconstruct.library.client;

import slimeknights.tconstruct.TConstruct;

import java.util.OptionalDouble;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public class TinkerRenderTypes extends RenderLayer {
    public TinkerRenderTypes(String name, VertexFormat format, DrawMode mode, int bufferSize, boolean affectsCrumbling, boolean sort, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sort, setupState, clearState);
    }

    /**
     * Render type for the error block that is seen through everything, mostly based on {@link RenderLayer#LINES}
     */
    public static final RenderLayer ERROR_BLOCK = RenderLayer.of(
            TConstruct.resourceString("lines"), VertexFormats.LINES, VertexFormat.DrawMode.LINES, 256, false, false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(LINES_PROGRAM)
                    .lineWidth(new LineWidth(OptionalDouble.empty()))
                    .layering(VIEW_OFFSET_Z_LAYERING)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .target(ITEM_ENTITY_TARGET)
                    .writeMaskState(ALL_MASK)
                    .cull(DISABLE_CULLING)
                    .depthTest(ALWAYS_DEPTH_TEST)
                    .build(false));

    /**
     * Render type for fluids, like {@link slimeknights.mantle.client.render.MantleRenderTypes#FLUID}, but disables cull so both sides show
     */
    public static final RenderLayer SMELTERY_FLUID = RenderLayer.of(
            TConstruct.resourceString("smeltery_fluid"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, VertexFormat.DrawMode.QUADS, 256, false, true,
            MultiPhaseParameters.builder()
                    .lightmap(ENABLE_LIGHTMAP)
                    .program(POSITION_COLOR_TEXTURE_LIGHTMAP_PROGRAM)
                    .texture(MIPMAP_BLOCK_ATLAS_TEXTURE)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .build(false));
}
