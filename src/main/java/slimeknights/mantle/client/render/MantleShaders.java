package slimeknights.mantle.client.render;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.mantle.Mantle;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Mantle.modId, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MantleShaders {

    private static ShaderProgram blockFullBrightShader;

    @SubscribeEvent
    static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderProgram(event.getResourceProvider(), Mantle.getResource("block_fullbright"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL),
                shader -> blockFullBrightShader = shader
        );
    }

    public static ShaderProgram getBlockFullBrightShader() {
        return blockFullBrightShader;
    }
}
