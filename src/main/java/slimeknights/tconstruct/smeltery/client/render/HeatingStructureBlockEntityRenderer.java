package slimeknights.tconstruct.smeltery.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.TinkerRenderTypes;
import slimeknights.tconstruct.library.client.model.TinkerTransformTypes;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;

public class HeatingStructureBlockEntityRenderer implements BlockEntityRenderer<HeatingStructureBlockEntity> {
    private static final float ITEM_SCALE = 15f / 16f;

    public HeatingStructureBlockEntityRenderer(Context context) {
    }

    @Override
    public void render(HeatingStructureBlockEntity smeltery, float partialTicks, MatrixStack matrices, VertexConsumerProvider buffer, int combinedLight, int combinedOverlay) {
        World world = smeltery.getWorld();
        if (world == null) return;
        BlockState state = smeltery.getCachedState();
        StructureData structure = smeltery.getStructure();
        boolean structureValid = state.get(ControllerBlock.IN_STRUCTURE) && structure != null;

        // render erroring block, done whether in the structure or not
        BlockPos errorPos = smeltery.getErrorPos();
        if (errorPos != null && MinecraftClient.getInstance().player != null) {
            // either we must be holding the book, or the structure must be erroring and it be within 10 seconds of last update
            boolean highlightError = smeltery.isHighlightError();
            if ((!structureValid && highlightError) || smeltery.showDebugBlockBorder(MinecraftClient.getInstance().player)) {
                // distance check, 512 is the squared length of the diagonal of a max size structure
                BlockPos pos = smeltery.getPos();
                BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
                int dx = playerPos.getX() - pos.getX();
                int dz = playerPos.getZ() - pos.getZ();
                if ((dx * dx + dz * dz) < 512) {
                    // color will be yellow if the structure is valid (expanding), red if invalid
                    VertexConsumer vertexBuilder = buffer.getBuffer(highlightError ? TinkerRenderTypes.ERROR_BLOCK : RenderLayer.LINES);
                    WorldRenderer.drawCuboidShapeOutline(matrices, vertexBuilder, VoxelShapes.fullCube(), errorPos.getX() - pos.getX(), errorPos.getY() - pos.getY(), errorPos.getZ() - pos.getZ(), 1f, structureValid ? 1f : 0f, 0f, 0.5f);
                }
            }
        }

        // if no structure, nothing else to do
        if (!structureValid) {
            return;
        }

        // relevant positions
        BlockPos pos = smeltery.getPos();
        BlockPos minPos = structure.getMinInside();
        BlockPos maxPos = structure.getMaxInside();

        // offset to make rendering min pos relative
        matrices.push();
        matrices.translate(minPos.getX() - pos.getX(), minPos.getY() - pos.getY(), minPos.getZ() - pos.getZ());
        // render tank fluids, use minPos for brightness
        SmelteryTankRenderer.renderFluids(matrices, buffer, smeltery.getTank(), minPos, maxPos, WorldRenderer.getLightmapCoordinates(world, minPos));

        // render items
        int xd = 1 + maxPos.getX() - minPos.getX();
        int zd = 1 + maxPos.getZ() - minPos.getZ();
        int layer = xd * zd;
        Direction facing = state.get(ControllerBlock.FACING);
        Quaternionf itemRotation = RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F * (float) facing.getHorizontal());
        MeltingModuleInventory inventory = smeltery.getMeltingInventory();
        MinecraftClient mc = MinecraftClient.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        int max = Config.CLIENT.maxSmelteryItemQuads.get();
        if (max != 0) {
            int quadsRendered = 0;
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    // calculate position inside the smeltery from slot index
                    int height = i / layer;
                    int layerIndex = i % layer;
                    int offsetX = layerIndex % xd;
                    int offsetZ = layerIndex / xd;
                    BlockPos itemPos = minPos.add(offsetX, height, offsetZ);

                    // offset to the slot position in the structure, scale, and rotate the item
                    matrices.push();
                    matrices.translate(offsetX + 0.5f, height + 0.5f, offsetZ + 0.5f);
                    matrices.multiply(itemRotation);
                    matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                    BakedModel model = itemRenderer.getModel(stack, world, null, 0);
                    itemRenderer.renderItem(stack, TinkerTransformTypes.MELTER, false, matrices, buffer, WorldRenderer.getLightmapCoordinates(world, itemPos), OverlayTexture.DEFAULT_UV, model);
                    matrices.pop();

                    // done as quads rather than items as its not that expensive to draw blocks, items are the problem
                    if (max != -1) {
                        // builtin has no quads, lets pretend its 100 as they are more expensive
                        if (model.isBuiltin()) {
                            quadsRendered += 100;
                        } else {
                            Random random = smeltery.getWorld().getRandom();
                            // not setting the seed on the random and ignoring the forge layered model stuff means this is just an estimate, but since this is for the sake of performance its not a huge deal for it to be exact
                            for (Direction direction : Direction.values()) {
                                quadsRendered += model.getQuads(null, direction, random).size();
                            }
                            quadsRendered += model.getQuads(null, null, random).size();
                        }
                        if (quadsRendered > max) {
                            break;
                        }
                    }
                }
            }
        }

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(HeatingStructureBlockEntity tile) {
        return tile.getCachedState().get(ControllerBlock.IN_STRUCTURE) && tile.getStructure() != null;
    }
}
