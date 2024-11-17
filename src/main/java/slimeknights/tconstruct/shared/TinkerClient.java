package slimeknights.tconstruct.shared;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.DyedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FirstArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FixedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.MaterialArmorTextureSupplier;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.IColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.ISpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.OffsettingSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.RecolorSpriteTransformer;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.TinkerTransformTypes;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.tables.client.PatternGuiTextureLoader;

import java.util.function.Consumer;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * This class should only be referenced on the client side
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.FORGE)
public class TinkerClient {
    /**
     * Called by TConstruct to handle any client side logic that needs to run during the constructor
     */
    public static void onConstruct() {
        TinkerBook.initBook();
        // needs to register listeners early enough for minecraft to load
        PatternGuiTextureLoader.init();
        ModifierIconManager.init();
        MaterialRenderInfoLoader.init();
        TinkerTransformTypes.init();

        // add the recipe cache invalidator to the client
        Consumer<RecipesUpdatedEvent> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);
        MinecraftForge.EVENT_BUS.addListener(recipesUpdated);

        // register datagen serializers
        ISpriteTransformer.SERIALIZER.registerDeserializer(RecolorSpriteTransformer.NAME, RecolorSpriteTransformer.DESERIALIZER);
        GreyToSpriteTransformer.init();
        ISpriteTransformer.SERIALIZER.registerDeserializer(OffsettingSpriteTransformer.NAME, OffsettingSpriteTransformer.DESERIALIZER);
        IColorMapping.SERIALIZER.registerDeserializer(GreyToColorMapping.NAME, GreyToColorMapping.DESERIALIZER);

        // armor textures
        ArmorTextureSupplier.LOADER.register(getResource("fixed"), FixedArmorTextureSupplier.LOADER);
        ArmorTextureSupplier.LOADER.register(getResource("dyed"), DyedArmorTextureSupplier.LOADER);
        ArmorTextureSupplier.LOADER.register(getResource("first_present"), FirstArmorTextureSupplier.LOADER);
        ArmorTextureSupplier.LOADER.register(getResource("material"), MaterialArmorTextureSupplier.Material.LOADER);
        ArmorTextureSupplier.LOADER.register(getResource("persistent_data"), MaterialArmorTextureSupplier.PersistentData.LOADER);
    }

    @SubscribeEvent
    static void renderBlockOverlay(RenderBlockScreenEffectEvent event) {
        BlockState state = event.getBlockState();
        if (state.isIn(TinkerTags.Blocks.TRANSPARENT_OVERLAY)) {
            MinecraftClient minecraft = MinecraftClient.getInstance();
            assert minecraft.world != null;
            assert minecraft.player != null;
            BlockPos pos = event.getBlockPos();
            float width = minecraft.player.getWidth() * 0.8F;
            // check collision of the block again, for non-full blocks
            if (VoxelShapes.matchesAnywhere(state.getOutlineShape(minecraft.world, pos).offset(pos.getX(), pos.getY(), pos.getZ()), VoxelShapes.cuboid(Box.of(minecraft.player.getEyePos(), width, 1.0E-6D, width)), BooleanBiFunction.AND)) {
                // this is for the most part a clone of the vanilla logic from ScreenEffectRenderer with some changes mentioned below

                Sprite texture = minecraft.getBlockRenderManager().getModels().getTexture(state, minecraft.world, pos);
                RenderSystem.setShaderTexture(0, texture.atlas().location());
                // changed: shader using pos tex
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();

                // change: handle brightness based on renderWater, and enable blend
                PlayerEntity player = minecraft.player;
                BlockPos blockpos = new BlockPos(player.getX(), player.getEyeY(), player.getZ());
                float brightness = LightmapTextureManager.getBrightness(player.world.dimensionType(), player.world.getMaxLocalRawBrightness(blockpos));
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f);

                // draw the quad
                float u0 = texture.getMinU();
                float u1 = texture.getMaxU();
                float v0 = texture.getMinV();
                float v1 = texture.getMaxV();
                Matrix4f matrix4f = event.getPoseStack().last().pose();
                bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                // change: dropped color, see above
                bufferbuilder.vertex(matrix4f, -1, -1, -0.5f).uv(u1, v1).endVertex();
                bufferbuilder.vertex(matrix4f, 1, -1, -0.5f).uv(u0, v1).endVertex();
                bufferbuilder.vertex(matrix4f, 1, 1, -0.5f).uv(u0, v0).endVertex();
                bufferbuilder.vertex(matrix4f, -1, 1, -0.5f).uv(u1, v0).endVertex();
                BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());
                // changed: disable blend
                RenderSystem.disableBlend();
            }
            event.setCanceled(true);
        }
    }
}
