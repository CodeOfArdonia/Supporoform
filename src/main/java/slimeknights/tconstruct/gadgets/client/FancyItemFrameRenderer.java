package slimeknights.tconstruct.gadgets.client;

import com.mojang.math.Vector3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event.Result;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;

import java.util.EnumMap;
import java.util.Map;

public class FancyItemFrameRenderer<T extends FancyItemFrameEntity> extends ItemFrameEntityRenderer<T> {
    public static final Map<FrameType, Identifier> LOCATIONS_MODEL = new EnumMap<>(FrameType.class);
    public static final Map<FrameType, Identifier> LOCATIONS_MODEL_MAP = new EnumMap<>(FrameType.class);

    static {
        for (FrameType type : FrameType.values()) {
            String name = type == FrameType.REVERSED_GOLD ? FrameType.GOLD.asString() : type.asString();
            LOCATIONS_MODEL.put(type, TConstruct.getResource("block/frame/" + name));
            LOCATIONS_MODEL_MAP.put(type, TConstruct.getResource("block/frame/" + name + "_map"));
        }
    }

    public FancyItemFrameRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected int getBlockLightLevel(T frame, BlockPos pPos) {
        int baseLight = super.getBlockLight(frame, pPos);
        return frame.getFrameType() == FrameType.MANYULLYN ? Math.max(7, baseLight) : baseLight;
    }

    @SuppressWarnings("UnstableApiUsage")
    // seriously forge, how am I supposed to implement something like vanilla if I cannot create events?
    @Override
    public void render(T frame, float entityYaw, float partialTicks, MatrixStack matrices, VertexConsumerProvider bufferIn, int packedLight) {
        FrameType frameType = frame.getFrameType();

        // base entity rendering logic, since calling super gives us the item frame renderer
        RenderNameTagEvent renderNameplate = new RenderNameTagEvent(frame, frame.getDisplayName(), this, matrices, bufferIn, packedLight, partialTicks);
        MinecraftForge.EVENT_BUS.post(renderNameplate);
        if (renderNameplate.getResult() == Result.ALLOW || (renderNameplate.getResult() != Result.DENY && this.hasLabel(frame))) {
            this.renderLabelIfPresent(frame, renderNameplate.getContent(), matrices, bufferIn, packedLight);
        }

        // orient the renderer
        matrices.push();
        Direction facing = frame.getHorizontalFacing();
        Vec3d offset = this.getPositionOffset(frame, partialTicks);
        matrices.translate(facing.getOffsetX() * 0.46875D - offset.getX(), facing.getOffsetY() * 0.46875D - offset.getY(), facing.getOffsetZ() * 0.46875D - offset.getZ());
        matrices.multiply(Vector3f.XP.rotationDegrees(frame.getPitch()));
        matrices.multiply(Vector3f.YP.rotationDegrees(180.0F - frame.getYaw()));

        // render the frame
        ItemStack stack = frame.getHeldItemStack();
        boolean isMap = !stack.isEmpty() && stack.getItem() instanceof FilledMapItem;
        // clear does not render the frame if filled
        boolean frameVisible = !frame.isInvisible() && (frameType != FrameType.CLEAR || stack.isEmpty());
        if (frameVisible) {
            matrices.push();
            matrices.translate(-0.5D, -0.5D, -0.5D);
            blockRenderManager.getModelRenderer().renderModel(
                    matrices.peek(), bufferIn.getBuffer(TexturedRenderLayers.getEntityCutout()), null,
                    blockRenderManager.getBlockModelShaper().getModelManager().getModel(isMap ? LOCATIONS_MODEL_MAP.get(frameType) : LOCATIONS_MODEL.get(frameType)),
                    1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        // render the item
        if (!stack.isEmpty()) {
            // if no frame, offset the item farther back
            matrices.translate(0.0D, 0.0D, 0.4375D);

            // determine rotation for the item inside
            MapState mapdata = null;
            if (isMap) {
                mapdata = FilledMapItem.getMapState(stack, frame.world);
            }
            int frameRotation = frame.getRotation();
            // for diamond, render the timer as a partial rotation
            if (frameType == FrameType.DIAMOND) {
                int rotation = mapdata != null ? (frameRotation + 2) % 4 * 4 : frameRotation;
                matrices.multiply(Vector3f.ZP.rotationDegrees(rotation * 360f / 16f));
            } else {
                int rotation = mapdata != null ? (frameRotation + 2) % 4 * 2 : frameRotation;
                matrices.multiply(Vector3f.ZP.rotationDegrees(rotation * 360f / 8f));
            }
            if (!MinecraftForge.EVENT_BUS.post(new RenderItemInFrameEvent(frame, this, matrices, bufferIn, packedLight))) {
                if (mapdata != null) {
                    matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);
                    matrices.translate(-64.0D, -64.0D, -1.0D);
                    int light = frameType == FrameType.MANYULLYN ? 0x00F000F0 : packedLight;
                    Integer mapId = FilledMapItem.getMapId(stack);
                    assert mapId != null;
                    MinecraftClient.getInstance().gameRenderer.getMapRenderer().draw(matrices, bufferIn, mapId, mapdata, true, light);
                } else {
                    float scale = frameType == FrameType.CLEAR ? 0.75f : 0.5f;
                    matrices.scale(scale, scale, scale);
                    int light = frameType == FrameType.MANYULLYN ? 0x00F000F0 : packedLight;
                    this.itemRenderer.renderItem(stack, ItemTransforms.TransformType.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, bufferIn, frame.getId());
                }
            }
        }

        matrices.pop();
    }
}
