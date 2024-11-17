package slimeknights.tconstruct.shared;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.utils.DomainDisplayName;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;
import slimeknights.tconstruct.shared.client.FluidParticle;

import java.util.function.Consumer;

@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class CommonsClientEvents extends ClientEventBase {
    @SubscribeEvent
    static void addResourceListeners(RegisterClientReloadListenersEvent event) {
        DomainDisplayName.addResourceListener(event);
    }

    @SubscribeEvent
    static void clientSetup(final FMLClientSetupEvent event) {
        RenderLayers.setRenderLayer(TinkerCommons.glow.get(), RenderLayer.getTranslucent());

        // glass
        RenderLayers.setRenderLayer(TinkerCommons.clearGlass.get(), RenderLayer.getCutout());
        RenderLayers.setRenderLayer(TinkerCommons.clearGlassPane.get(), RenderLayer.getCutout());
        RenderLayers.setRenderLayer(TinkerCommons.clearTintedGlass.get(), RenderLayer.getTranslucent());
        for (GlassColor color : GlassColor.values()) {
            RenderLayers.setRenderLayer(TinkerCommons.clearStainedGlass.get(color), RenderLayer.getTranslucent());
            RenderLayers.setRenderLayer(TinkerCommons.clearStainedGlassPane.get(color), RenderLayer.getTranslucent());
        }
        RenderLayers.setRenderLayer(TinkerCommons.soulGlass.get(), RenderLayer.getTranslucent());
        RenderLayers.setRenderLayer(TinkerCommons.soulGlassPane.get(), RenderLayer.getTranslucent());
        RenderLayers.setRenderLayer(TinkerMaterials.soulsteel.get(), RenderLayer.getTranslucent());
        RenderLayers.setRenderLayer(TinkerMaterials.slimesteel.get(), RenderLayer.getTranslucent());

        RenderLayer cutout = RenderLayer.getCutout();
        RenderLayers.setRenderLayer(TinkerCommons.cheeseBlock.get(), cutout);
        RenderLayers.setRenderLayer(TinkerCommons.goldBars.get(), cutout);
        RenderLayers.setRenderLayer(TinkerCommons.goldPlatform.get(), cutout);
        RenderLayers.setRenderLayer(TinkerCommons.ironPlatform.get(), cutout);
        RenderLayers.setRenderLayer(TinkerCommons.cobaltPlatform.get(), cutout);
        Consumer<Block> setCutout = block -> RenderLayers.setRenderLayer(block, cutout);
        TinkerCommons.copperPlatform.forEach(setCutout);
        TinkerCommons.waxedCopperPlatform.forEach(setCutout);

        TextRenderer unicode = unicodeFontRender();
        TinkerBook.MATERIALS_AND_YOU.fontRenderer = unicode;
        TinkerBook.TINKERS_GADGETRY.fontRenderer = unicode;
        TinkerBook.PUNY_SMELTING.fontRenderer = unicode;
        TinkerBook.MIGHTY_SMELTING.fontRenderer = unicode;
        TinkerBook.FANTASTIC_FOUNDRY.fontRenderer = unicode;
        TinkerBook.ENCYCLOPEDIA.fontRenderer = unicode;
    }

    @SubscribeEvent
    static void registerColorHandlers(RegisterColorHandlersEvent.Item event) {
        // colors apply a constant tint to make models easier
        BlockColors blockColors = event.getBlockColors();
        ItemColors itemColors = event.getItemColors();
        for (GlassColor color : GlassColor.values()) {
            Block block = TinkerCommons.clearStainedGlass.get(color);
            Block pane = TinkerCommons.clearStainedGlassPane.get(color);
            blockColors.registerColorProvider((state, reader, pos, index) -> color.getColor(), block, pane);
            registerBlockItemColorAlias(blockColors, itemColors, block);
            registerBlockItemColorAlias(blockColors, itemColors, pane);
        }
    }

    @SubscribeEvent
    static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        MinecraftClient.getInstance().particleManager.registerFactory(TinkerCommons.fluidParticle.get(), new FluidParticle.Factory());
    }

    private static TextRenderer unicodeRenderer;

    /**
     * Gets the unicode font renderer
     */
    public static TextRenderer unicodeFontRender() {
        if (unicodeRenderer == null)
            unicodeRenderer = new TextRenderer(rl -> {
                FontManager resourceManager = MinecraftClient.getInstance().fontManager;
                return resourceManager.fontStorages.get(MinecraftClient.UNICODE_FONT_ID);
            }, false);

        return unicodeRenderer;
    }
}
