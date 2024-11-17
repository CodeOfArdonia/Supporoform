package slimeknights.mantle.client.render;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import slimeknights.mantle.Mantle;

import static net.minecraft.client.render.VertexFormats.*;

/**
 * Class for render types defined by Mantle
 */
public class MantleRenderTypes extends RenderLayer {

    private MantleRenderTypes(String name, VertexFormat format, DrawMode mode, int bufferSize, boolean useDelegate, boolean needsSorting, Runnable setupTaskIn, Runnable clearTaskIn) {
        super(name, format, mode, bufferSize, useDelegate, needsSorting, setupTaskIn, clearTaskIn);
    }

    /**
     * Render type used for the fluid renderer
     */
    public static final RenderLayer FLUID = of(
            Mantle.modId + ":block_render_type",
            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, DrawMode.QUADS, 256, false, true,
            MultiPhaseParameters.builder()
                    .lightmap(ENABLE_LIGHTMAP)
                    .program(POSITION_COLOR_TEXTURE_LIGHTMAP_PROGRAM)
                    .texture(MIPMAP_BLOCK_ATLAS_TEXTURE)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .build(false));

    /**
     * Render type used for the structure renderer
     */
    public static final VertexFormat BLOCK_WITH_OVERLAY = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder().put("Position", POSITION_ELEMENT).put("Color", COLOR_ELEMENT).put("UV0", TEXTURE_ELEMENT).put("UV1", OVERLAY_ELEMENT).put("UV2", LIGHT_ELEMENT).put("Normal", NORMAL_ELEMENT).put("Padding", PADDING_ELEMENT).build());

    public static final RenderLayer TRANSLUCENT_FULLBRIGHT = of(
            Mantle.modId + ":translucent_fullbright",
            BLOCK_WITH_OVERLAY, DrawMode.QUADS, 256, false, false,
            MultiPhaseParameters.builder()
                    .program(new ShaderProgram(MantleShaders::getBlockFullBrightShader))
                    .lightmap(new Lightmap(false))
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .texture(MIPMAP_BLOCK_ATLAS_TEXTURE)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .build(false));
}
