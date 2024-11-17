package slimeknights.tconstruct.world.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.MagmaCubeEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import slimeknights.tconstruct.TConstruct;

public class TerracubeRenderer extends MobEntityRenderer<SlimeEntity, MagmaCubeEntityModel<SlimeEntity>> {
    private static final Identifier TEXTURE = TConstruct.getResource("textures/entity/terracube.png");

    public TerracubeRenderer(EntityRendererFactory.Context context) {
        super(context, new MagmaCubeEntityModel<>(context.getPart(EntityModelLayers.MAGMA_CUBE)), 0.25F);
        this.addFeature(new SlimeArmorLayer<>(this, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)), context.getModelLoader(), true));
    }

    @Override
    public Identifier getTextureLocation(SlimeEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SlimeEntity slime, MatrixStack matrices, float partialTickTime) {
        int size = slime.getSize();
        float squishFactor = MathHelper.lerp(partialTickTime, slime.lastStretch, slime.stretch) / ((float) size * 0.5F + 1.0F);
        float invertedSquish = 1.0F / (squishFactor + 1.0F);
        matrices.scale(invertedSquish * (float) size, 1.0F / invertedSquish * (float) size, invertedSquish * (float) size);
    }
}
