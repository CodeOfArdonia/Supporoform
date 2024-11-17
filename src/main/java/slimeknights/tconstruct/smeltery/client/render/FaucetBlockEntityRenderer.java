package slimeknights.tconstruct.smeltery.client.render;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import slimeknights.mantle.client.model.FaucetFluidLoader;
import slimeknights.mantle.client.model.fluid.FluidCuboid;
import slimeknights.mantle.client.model.fluid.FluidsModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

import java.util.function.Function;

public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity> {
    public FaucetBlockEntityRenderer(Context context) {
    }

    @Override
    public void render(FaucetBlockEntity tileEntity, float partialTicks, MatrixStack matrices, VertexConsumerProvider bufferIn, int combinedLightIn, int combinedOverlayIn) {
        FluidStack renderFluid = tileEntity.getRenderFluid();
        if (!tileEntity.isPouring() || renderFluid.isEmpty()) {
            return;
        }

        // safety
        World world = tileEntity.getWorld();
        if (world == null) {
            return;
        }

        // fetch faucet model to determine where to render fluids
        BlockState state = tileEntity.getCachedState();
        FluidsModel.Baked model = ModelHelper.getBakedModel(state, FluidsModel.Baked.class);
        if (model != null) {
            // if side, rotate fluid model
            Direction direction = state.get(FaucetBlock.FACING);
            boolean isRotated = RenderingHelper.applyRotation(matrices, direction);

            // fluid props
            IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(renderFluid.getFluid());
            int color = attributes.getTintColor(renderFluid);
            Function<Identifier, Sprite> spriteGetter = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            Sprite still = spriteGetter.apply(attributes.getStillTexture(renderFluid));
            Sprite flowing = spriteGetter.apply(attributes.getFlowingTexture(renderFluid));
            FluidType fluidType = renderFluid.getFluid().getFluidType();
            boolean isGas = fluidType.isLighterThanAir();
            combinedLightIn = FluidRenderer.withBlockLight(combinedLightIn, fluidType.getLightLevel(renderFluid));

            // render all cubes in the model
            VertexConsumer buffer = bufferIn.getBuffer(MantleRenderTypes.FLUID);
            for (FluidCuboid cube : model.getFluids()) {
                FluidRenderer.renderCuboid(matrices, buffer, cube, 0, still, flowing, color, combinedLightIn, isGas);
            }

            // render into the block(s) below
            FaucetFluidLoader.renderFaucetFluids(world, tileEntity.getPos(), direction, matrices, buffer, still, flowing, color, combinedLightIn);

            // if rotated, pop back rotation
            if (isRotated) {
                matrices.pop();
            }
        }
    }
}
