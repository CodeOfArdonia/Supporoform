package slimeknights.tconstruct.smeltery.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.World;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.client.model.FaucetFluidLoader;
import slimeknights.mantle.client.model.fluid.FluidCuboid;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.library.client.model.block.ChannelModel;
import slimeknights.tconstruct.smeltery.block.ChannelBlock;
import slimeknights.tconstruct.smeltery.block.ChannelBlock.ChannelConnection;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

public class ChannelBlockEntityRenderer implements BlockEntityRenderer<ChannelBlockEntity> {
    public ChannelBlockEntityRenderer(Context context) {
    }

    @Override
    public void render(ChannelBlockEntity te, float partialTicks, MatrixStack matrices, VertexConsumerProvider buffer, int light, int combinedOverlayIn) {
        FluidStack fluid = te.getFluid();
        if (fluid.isEmpty()) {
            return;
        }

        // fetch model properties
        World world = te.getWorld();
        if (world == null) {
            return;
        }
        BlockPos pos = te.getPos();
        BlockState state = te.getCachedState();
        ChannelModel.Baked model = ModelHelper.getBakedModel(state, ChannelModel.Baked.class);
        if (model == null) {
            return;
        }

        // fluid attributes
        IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid.getFluid());
        Sprite still = FluidRenderer.getBlockSprite(attributes.getStillTexture(fluid));
        Sprite flowing = FluidRenderer.getBlockSprite(attributes.getFlowingTexture(fluid));
        VertexConsumer builder = buffer.getBuffer(MantleRenderTypes.FLUID);
        int color = attributes.getTintColor(fluid);
        light = FluidRenderer.withBlockLight(light, fluid.getFluid().getFluidType().getLightLevel(fluid));

        // render sides first, while doing so we will determine center "flow"
        FluidCuboid cube;
        boolean isRotated;
        Direction centerFlow = Direction.UP;
        for (Direction direction : Type.HORIZONTAL) {
            // check if we have that side on the block
            ChannelConnection connection = state.get(ChannelBlock.DIRECTION_MAP.get(direction));
            if (connection.canFlow()) {
                // apply rotation for the side
                isRotated = RenderingHelper.applyRotation(matrices, direction);
                // get the relevant fluid model, render it
                if (te.isFlowing(direction)) {
                    cube = model.getSideFlow(connection == ChannelConnection.OUT);

                    // add to center direction
                    if (connection == ChannelConnection.OUT) {
                        // if unset (up), use this direction
                        if (centerFlow == Direction.UP) {
                            centerFlow = direction;
                            // if set and it disagrees, set the fail state (down)
                        } else if (centerFlow != direction) {
                            centerFlow = Direction.DOWN;
                        }
                    }
                    // render the extra edge against other blocks
                    if (!world.getBlockState(pos.offset(direction)).isOf(state.getBlock())) {
                        FluidRenderer.renderCuboid(matrices, builder, model.getSideEdge(), 0, still, flowing, color, light, false);
                    }
                } else {
                    cube = model.getSideStill();
                }
                FluidRenderer.renderCuboid(matrices, builder, cube, 0, still, flowing, color, light, false);
                // undo rotation
                if (isRotated) {
                    matrices.pop();
                }
            }
        }

        // render center
        isRotated = false;
        if (centerFlow.getAxis().isVertical()) {
            cube = model.getCenterFluid(false);
        } else {
            cube = model.getCenterFluid(true);
            isRotated = RenderingHelper.applyRotation(matrices, centerFlow);
        }
        // render the cube and pop back
        FluidRenderer.renderCuboid(matrices, builder, cube, 0, still, flowing, color, light, false);
        if (isRotated) {
            matrices.pop();
        }

        // render flow downwards
        if (state.get(ChannelBlock.DOWN) && te.isFlowing(Direction.DOWN)) {
            cube = model.getDownFluid();
            FluidRenderer.renderCuboid(matrices, builder, cube, 0, still, flowing, color, light, false);

            // render into the block(s) below
            FaucetFluidLoader.renderFaucetFluids(world, pos, Direction.DOWN, matrices, builder, still, flowing, color, light);
        }
    }
}
