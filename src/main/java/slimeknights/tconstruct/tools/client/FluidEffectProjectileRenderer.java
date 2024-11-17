package slimeknights.tconstruct.tools.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.fluid.FluidCuboid;
import slimeknights.mantle.client.model.fluid.FluidCuboid.FluidFace;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.tools.entity.FluidEffectProjectile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FluidEffectProjectileRenderer extends EntityRenderer<FluidEffectProjectile> {
    // TODO: make public in mantle
    private static final Map<Direction, FluidFace> FACES;

    static {
        FACES = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            FACES.put(direction, FluidFace.NORMAL);
        }
    }

    private final List<FluidCuboid> fluids;

    public FluidEffectProjectileRenderer(Context context) {
        super(context);
        this.fluids = List.of(
                new FluidCuboid(new Vector3f(-4, 0, 0), new Vector3f(-2, 2, 2), FACES),
                new FluidCuboid(new Vector3f(0, -4, 0), new Vector3f(2, -2, 2), FACES),
                new FluidCuboid(new Vector3f(0, 0, -4), new Vector3f(2, 2, -2), FACES),
                new FluidCuboid(new Vector3f(2, 0, 0), new Vector3f(4, 2, 2), FACES),
                new FluidCuboid(new Vector3f(0, 0, 0), new Vector3f(2, 4, 2), FACES),
                new FluidCuboid(new Vector3f(0, 0, 2), new Vector3f(2, 2, 4), FACES));
    }

    @Override
    public void render(FluidEffectProjectile pEntity, float pEntityYaw, float pPartialTicks, MatrixStack pMatrixStack, VertexConsumerProvider pBuffer, int pPackedLight) {
        pMatrixStack.push();
        pMatrixStack.translate(0.0D, 0.15F, 0.0D);
        pMatrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(pPartialTicks, pEntity.prevYaw, pEntity.getYaw()) - 90.0F));
        pMatrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(pPartialTicks, pEntity.prevPitch, pEntity.getPitch())));
        FluidRenderer.renderCuboids(pMatrixStack, pBuffer.getBuffer(MantleRenderTypes.FLUID), this.fluids, pEntity.getFluid(), pPackedLight);
        pMatrixStack.pop();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public Identifier getTexture(FluidEffectProjectile pEntity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
