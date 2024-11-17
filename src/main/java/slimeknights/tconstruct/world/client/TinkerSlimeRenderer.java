package slimeknights.tconstruct.world.client;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SlimeEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;

public class TinkerSlimeRenderer extends SlimeEntityRenderer {
    public static final Factory SKY_SLIME_FACTORY = new Factory(TConstruct.getResource("textures/entity/sky_slime.png"));
    public static final Factory ENDER_SLIME_FACTORY = new Factory(TConstruct.getResource("textures/entity/ender_slime.png"));

    private final Identifier texture;

    public TinkerSlimeRenderer(EntityRendererFactory.Context context, Identifier texture) {
        super(context);
        this.texture = texture;
        this.addFeature(new SlimeArmorLayer<>(this, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)), context.getModelLoader(), false));
    }

    @Override
    public Identifier getTexture(SlimeEntity entity) {
        return this.texture;
    }

    private static class Factory implements EntityRendererFactory<SlimeEntity> {
        private final Identifier texture;

        public Factory(Identifier texture) {
            this.texture = texture;
        }

        @Override
        public EntityRenderer<SlimeEntity> create(Context context) {
            return new TinkerSlimeRenderer(context, this.texture);
        }
    }
}
