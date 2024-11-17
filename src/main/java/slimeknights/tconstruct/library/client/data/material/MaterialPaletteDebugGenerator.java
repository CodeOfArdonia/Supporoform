package slimeknights.tconstruct.library.client.data.material;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.library.client.data.GenericTextureGenerator;
import slimeknights.tconstruct.library.client.data.material.AbstractMaterialSpriteProvider.MaterialSpriteInfo;
import slimeknights.tconstruct.library.client.data.spritetransformer.IColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.RecolorSpriteTransformer;

import java.io.IOException;
import java.util.Map.Entry;

/**
 * Simple generator that generates a texture showing the entire range for a palette
 */
public class MaterialPaletteDebugGenerator extends GenericTextureGenerator {
    private final String name;
    private final AbstractMaterialSpriteProvider[] materialProviders;

    public MaterialPaletteDebugGenerator(DataGenerator generator, String name, AbstractMaterialSpriteProvider... materialProviders) {
        super(generator, "debug/material_palettes");
        this.name = name;
        this.materialProviders = materialProviders;
    }

    @Override
    public void run(DataWriter cache) throws IOException {
        for (AbstractMaterialSpriteProvider materialProvider : materialProviders) {
            for (Entry<Identifier, MaterialSpriteInfo> entry : materialProvider.getMaterials().entrySet()) {
                if (entry.getValue().getTransformer() instanceof RecolorSpriteTransformer recolor) {
                    IColorMapping colorMapping = recolor.getColorMapping();
                    NativeImage palette = new NativeImage(256, 16, true);
                    for (int grey = 0; grey < 256; grey++) {
                        // set the grey value to RGB, leave alpha as 255
                        int color = colorMapping.mapColor(grey | (grey << 8) | (grey << 16) | 0xFF000000);
                        for (int height = 0; height < 16; height++) {
                            palette.setColor(grey, height, color);
                        }
                    }
                    saveImage(cache, entry.getKey(), palette);
                    palette.close();
                }
            }
        }
    }

    @Override
    public String getName() {
        return this.name + " Material Palette Debug";
    }
}
