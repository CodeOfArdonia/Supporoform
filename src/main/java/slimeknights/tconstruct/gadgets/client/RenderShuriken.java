package slimeknights.tconstruct.gadgets.client;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import slimeknights.tconstruct.gadgets.entity.shuriken.ShurikenEntityBase;

public class RenderShuriken extends EntityRenderer<ShurikenEntityBase> {
    private final ItemRenderer itemRenderer;

    public RenderShuriken(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ShurikenEntityBase entity, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        if (entity.age >= 2 || !(this.dispatcher.camera.getFocusedEntity().squaredDistanceTo(entity) < 12.25D)) {
            matrixStackIn.push();
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-(entity.age + partialTicks) * 30 % 360));
            matrixStackIn.translate(-0.03125, -0.09375, 0);
            this.itemRenderer.renderItem(entity.getStack(), ModelTransformationMode.GROUND, packedLightIn, OverlayTexture.DEFAULT_UV, matrixStackIn, bufferIn, entity.getWorld(), entity.getId());
            matrixStackIn.pop();
            super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        }
    }

    @Override
    public Identifier getTexture(ShurikenEntityBase entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
