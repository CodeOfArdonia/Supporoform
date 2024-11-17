package slimeknights.tconstruct.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.library.client.particle.SlimeParticle;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.client.SkullModelHelper;
import slimeknights.tconstruct.world.client.SlimeColorReloadListener;
import slimeknights.tconstruct.world.client.SlimeColorizer;
import slimeknights.tconstruct.world.client.TerracubeRenderer;
import slimeknights.tconstruct.world.client.TinkerSlimeRenderer;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class WorldClientEvents extends ClientEventBase {
    @SubscribeEvent
    static void addResourceListener(RegisterClientReloadListenersEvent event) {
        for (FoliageType type : FoliageType.values()) {
            event.registerReloadListener(new SlimeColorReloadListener(type));
        }
    }

    @SubscribeEvent
    static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        ParticleManager engine = MinecraftClient.getInstance().particleManager;
        engine.registerFactory(TinkerWorld.skySlimeParticle.get(), new SlimeParticle.Factory(SlimeType.SKY));
        engine.registerFactory(TinkerWorld.enderSlimeParticle.get(), new SlimeParticle.Factory(SlimeType.ENDER));
        engine.registerFactory(TinkerWorld.terracubeParticle.get(), new SlimeParticle.Factory(Items.CLAY_BALL));
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        Supplier<TexturedModelData> normalHead = Lazy.of(SkullEntityModel::getSkullTexturedModelData);
        Supplier<TexturedModelData> headOverlayCustom = Lazy.of(() -> SkullModelHelper.createHeadHatLayer(0, 16, 32, 32));
        registerLayerDefinition(event, TinkerHeadType.BLAZE, normalHead);
        registerLayerDefinition(event, TinkerHeadType.ENDERMAN, Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 32, 16)));
        registerLayerDefinition(event, TinkerHeadType.STRAY, headOverlayCustom);

        // zombie
        registerLayerDefinition(event, TinkerHeadType.HUSK, Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 64, 64)));
        registerLayerDefinition(event, TinkerHeadType.DROWNED, headOverlayCustom);

        // spiders
        Supplier<TexturedModelData> spiderHead = Lazy.of(() -> SkullModelHelper.createHeadLayer(32, 4, 64, 32));
        registerLayerDefinition(event, TinkerHeadType.SPIDER, spiderHead);
        registerLayerDefinition(event, TinkerHeadType.CAVE_SPIDER, spiderHead);

        // piglin
        Supplier<TexturedModelData> piglinHead = Lazy.of(SkullModelHelper::createPiglinHead);
        registerLayerDefinition(event, TinkerHeadType.PIGLIN, piglinHead);
        registerLayerDefinition(event, TinkerHeadType.PIGLIN_BRUTE, piglinHead);
        registerLayerDefinition(event, TinkerHeadType.ZOMBIFIED_PIGLIN, piglinHead);
    }

    @SubscribeEvent
    static void registerSkullModels(EntityRenderersEvent.CreateSkullModels event) {
        EntityModelLoader modelSet = event.getEntityModelSet();
        SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> event.registerSkullModel(type, new SkullEntityModel(modelSet.getModelPart(layer))));
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TinkerWorld.skySlimeEntity.get(), TinkerSlimeRenderer.SKY_SLIME_FACTORY);
        event.registerEntityRenderer(TinkerWorld.enderSlimeEntity.get(), TinkerSlimeRenderer.ENDER_SLIME_FACTORY);
        event.registerEntityRenderer(TinkerWorld.terracubeEntity.get(), TerracubeRenderer::new);
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        RenderLayer cutout = RenderLayer.getCutout();
        RenderLayer cutoutMipped = RenderLayer.getCutoutMipped();

        // render types - slime plants
        for (FoliageType type : FoliageType.values()) {
            if (type != FoliageType.BLOOD) {
                RenderLayers.setRenderLayer(TinkerWorld.slimeLeaves.get(type), cutoutMipped);
            }
            RenderLayers.setRenderLayer(TinkerWorld.vanillaSlimeGrass.get(type), cutoutMipped);
            RenderLayers.setRenderLayer(TinkerWorld.earthSlimeGrass.get(type), cutoutMipped);
            RenderLayers.setRenderLayer(TinkerWorld.skySlimeGrass.get(type), cutoutMipped);
            RenderLayers.setRenderLayer(TinkerWorld.enderSlimeGrass.get(type), cutoutMipped);
            RenderLayers.setRenderLayer(TinkerWorld.ichorSlimeGrass.get(type), cutoutMipped);
            RenderLayers.setRenderLayer(TinkerWorld.slimeFern.get(type), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.pottedSlimeFern.get(type), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.slimeTallGrass.get(type), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.slimeSapling.get(type), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.pottedSlimeSapling.get(type), cutout);
        }
        RenderLayers.setRenderLayer(TinkerWorld.enderSlimeVine.get(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.skySlimeVine.get(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.enderbarkRoots.get(), cutout);

        // render types - slime blocks
        RenderLayer translucent = RenderLayer.getTranslucent();
        for (SlimeType type : SlimeType.TINKER) {
            RenderLayers.setRenderLayer(TinkerWorld.slime.get(type), translucent);
        }

        // doors
        RenderLayers.setRenderLayer(TinkerWorld.greenheart.getDoor(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.greenheart.getTrapdoor(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.skyroot.getDoor(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.skyroot.getTrapdoor(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.bloodshroom.getDoor(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.bloodshroom.getTrapdoor(), cutout);
        RenderLayers.setRenderLayer(TinkerWorld.enderbark.getTrapdoor(), cutout);

        // geodes
        for (BudSize size : BudSize.values()) {
            RenderLayers.setRenderLayer(TinkerWorld.earthGeode.getBud(size), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.skyGeode.getBud(size), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.ichorGeode.getBud(size), cutout);
            RenderLayers.setRenderLayer(TinkerWorld.enderGeode.getBud(size), cutout);
        }

        // skull textures
        event.enqueueWork(() -> {
            registerHeadModel(TinkerHeadType.BLAZE, MaterialIds.blazingBone, new ResourceLocation("textures/entity/blaze.png"));
            registerHeadModel(TinkerHeadType.ENDERMAN, MaterialIds.enderPearl, TConstruct.getResource("textures/entity/skull/enderman.png"));
            SlimeskullArmorModel.registerHeadModel(MaterialIds.glass, ModelLayers.CREEPER_HEAD, new ResourceLocation("textures/entity/creeper/creeper.png"));
            // skeleton
            SlimeskullArmorModel.registerHeadModel(MaterialIds.bone, ModelLayers.SKELETON_SKULL, new ResourceLocation("textures/entity/skeleton/skeleton.png"));
            SlimeskullArmorModel.registerHeadModel(MaterialIds.necroticBone, ModelLayers.WITHER_SKELETON_SKULL, new ResourceLocation("textures/entity/skeleton/wither_skeleton.png"));
            registerHeadModel(TinkerHeadType.STRAY, MaterialIds.venombone, TConstruct.getResource("textures/entity/skull/stray.png"));
            // zombies
            SlimeskullArmorModel.registerHeadModel(MaterialIds.rottenFlesh, ModelLayers.ZOMBIE_HEAD, new ResourceLocation("textures/entity/zombie/zombie.png"));
            registerHeadModel(TinkerHeadType.HUSK, MaterialIds.iron, new ResourceLocation("textures/entity/zombie/husk.png"));
            registerHeadModel(TinkerHeadType.DROWNED, MaterialIds.copper, TConstruct.getResource("textures/entity/skull/drowned.png"));
            // spider
            registerHeadModel(TinkerHeadType.SPIDER, MaterialIds.string, new ResourceLocation("textures/entity/spider/spider.png"));
            registerHeadModel(TinkerHeadType.CAVE_SPIDER, MaterialIds.darkthread, new ResourceLocation("textures/entity/spider/cave_spider.png"));
            // piglins
            registerHeadModel(TinkerHeadType.PIGLIN, MaterialIds.gold, new ResourceLocation("textures/entity/piglin/piglin.png"));
            registerHeadModel(TinkerHeadType.PIGLIN_BRUTE, MaterialIds.roseGold, new ResourceLocation("textures/entity/piglin/piglin_brute.png"));
            registerHeadModel(TinkerHeadType.ZOMBIFIED_PIGLIN, MaterialIds.pigIron, new ResourceLocation("textures/entity/piglin/zombified_piglin.png"));
        });
    }

    @SubscribeEvent
    static void registerBlockColorHandlers(RegisterColorHandlersEvent.Block event) {
        // slime plants - blocks
        for (FoliageType type : FoliageType.values()) {
            event.register(
                    (state, reader, pos, index) -> getSlimeColorByPos(pos, type, null),
                    TinkerWorld.vanillaSlimeGrass.get(type), TinkerWorld.earthSlimeGrass.get(type), TinkerWorld.skySlimeGrass.get(type),
                    TinkerWorld.enderSlimeGrass.get(type), TinkerWorld.ichorSlimeGrass.get(type));
            event.register(
                    (state, reader, pos, index) -> getSlimeColorByPos(pos, type, SlimeColorizer.LOOP_OFFSET),
                    TinkerWorld.slimeLeaves.get(type));
            event.register(
                    (state, reader, pos, index) -> getSlimeColorByPos(pos, type, null),
                    TinkerWorld.slimeFern.get(type), TinkerWorld.slimeTallGrass.get(type), TinkerWorld.pottedSlimeFern.get(type));
        }

        // vines
        event.register(
                (state, reader, pos, index) -> getSlimeColorByPos(pos, FoliageType.SKY, SlimeColorizer.LOOP_OFFSET),
                TinkerWorld.skySlimeVine.get());
        event.register(
                (state, reader, pos, index) -> getSlimeColorByPos(pos, FoliageType.ENDER, SlimeColorizer.LOOP_OFFSET),
                TinkerWorld.enderSlimeVine.get());
    }

    @SubscribeEvent
    static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {
        BlockColors blockColors = event.getBlockColors();
        ItemColors itemColors = event.getItemColors();
        // slime grass items
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.vanillaSlimeGrass);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.earthSlimeGrass);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.skySlimeGrass);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.enderSlimeGrass);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.ichorSlimeGrass);
        // plant items
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.slimeLeaves);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.slimeFern);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.slimeTallGrass);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.skySlimeVine);
        registerBlockItemColorAlias(blockColors, itemColors, TinkerWorld.enderSlimeVine);
    }

    /**
     * Block colors for a slime type
     *
     * @param pos  Block position
     * @param type Slime foilage color
     * @param add  Offset position
     * @return Color for the given position, or the default if position is null
     */
    private static int getSlimeColorByPos(@Nullable BlockPos pos, FoliageType type, @Nullable BlockPos add) {
        if (pos == null) {
            return type.getColor();
        }
        if (add != null) {
            pos = pos.add(add);
        }

        return SlimeColorizer.getColorForPos(pos, type);
    }

    /**
     * Registers a skull with the entity renderer and the slimeskull renderer
     */
    private static void registerHeadModel(TinkerHeadType skull, MaterialId materialId, Identifier texture) {
        SkullBlockEntityRenderer.TEXTURES.put(skull, texture);
        SlimeskullArmorModel.registerHeadModel(materialId, SkullModelHelper.HEAD_LAYERS.get(skull), texture);
    }

    /**
     * Register a layer without being under the minecraft domain. TODO: is this needed?
     */
    private static EntityModelLayer registerLayer(String name) {
        EntityModelLayer location = new EntityModelLayer(TConstruct.getResource(name), "main");
        if (!EntityModelLayers.LAYERS.add(location)) {
            throw new IllegalStateException("Duplicate registration for " + location);
        } else {
            return location;
        }
    }

    /**
     * Register a head layer definition with forge
     */
    private static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event, TinkerHeadType head, Supplier<TexturedModelData> supplier) {
        event.registerLayerDefinition(SkullModelHelper.HEAD_LAYERS.get(head), supplier);
    }
}
