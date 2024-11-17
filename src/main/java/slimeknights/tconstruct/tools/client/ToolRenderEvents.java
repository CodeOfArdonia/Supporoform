package slimeknights.tconstruct.tools.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Iterator;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToolRenderEvents {
    /**
     * Maximum number of blocks from the iterator to render
     */
    private static final int MAX_BLOCKS = 60;

    /**
     * Renders the outline on the extra blocks
     *
     * @param event the highlight event
     */
    @SubscribeEvent
    static void renderBlockHighlights(RenderHighlightEvent.Block event) {
        World world = MinecraftClient.getInstance().world;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (world == null || player == null) {
            return;
        }
        // must have the right tags
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || !stack.isIn(TinkerTags.Items.HARVEST)) {
            return;
        }
        // must be targeting a block
        HitResult result = MinecraftClient.getInstance().crosshairTarget;
        if (result == null || result.getType() != Type.BLOCK) {
            return;
        }
        // must not be broken, must be right interface
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        BlockHitResult blockTrace = event.getTarget();
        BlockPos origin = blockTrace.getBlockPos();
        BlockState state = world.getBlockState(origin);
        // must not be broken, and the tool definition must be effective
        if (!IsEffectiveToolHook.isEffective(tool, state)) {
            return;
        }
        Iterator<BlockPos> extraBlocks = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, stack, player, world.getBlockState(origin), world, origin, blockTrace.getSide(), AreaOfEffectIterator.AOEMatchType.BREAKING).iterator();
        if (!extraBlocks.hasNext()) {
            return;
        }

        // set up renderer
        WorldRenderer worldRender = event.getLevelRenderer();
        MatrixStack matrices = event.getPoseStack();
        VertexConsumerProvider.Immediate buffers = worldRender.bufferBuilders.bufferSource();
        VertexConsumer vertexBuilder = buffers.getBuffer(RenderLayer.getLines());
        matrices.push();

        // start drawing
        Camera renderInfo = MinecraftClient.getInstance().gameRenderer.getCamera();
        Entity viewEntity = renderInfo.getFocusedEntity();
        Vec3d vector3d = renderInfo.getPos();
        double x = vector3d.getX();
        double y = vector3d.getY();
        double z = vector3d.getZ();
        int rendered = 0;
        do {
            BlockPos pos = extraBlocks.next();
            if (world.getWorldBorder().contains(pos)) {
                rendered++;
                worldRender.drawBlockOutline(matrices, vertexBuilder, viewEntity, x, y, z, pos, world.getBlockState(pos));
            }
        } while (rendered < MAX_BLOCKS && extraBlocks.hasNext());
        matrices.pop();
        buffers.draw();
    }

    /**
     * Renders the block damage process on the extra blocks
     */
    @SubscribeEvent
    static void renderBlockDamageProgress(RenderLevelLastEvent event) {
        // validate required variables are set
        ClientPlayerInteractionManager controller = MinecraftClient.getInstance().interactionManager;
        if (controller == null || !controller.isBreakingBlock()) {
            return;
        }
        World world = MinecraftClient.getInstance().world;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (world == null || player == null || MinecraftClient.getInstance().getCameraEntity() == null) {
            return;
        }
        // must have the right tags
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || !stack.isIn(TinkerTags.Items.HARVEST)) {
            return;
        }
        // must be targeting a block
        HitResult result = MinecraftClient.getInstance().crosshairTarget;
        if (result == null || result.getType() != Type.BLOCK) {
            return;
        }
        // must not be broken, must be right interface
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        // find breaking progress
        BlockHitResult blockTrace = (BlockHitResult) result;
        BlockPos target = blockTrace.getBlockPos();
        BlockBreakingInfo progress = null;
        for (Int2ObjectMap.Entry<BlockBreakingInfo> entry : MinecraftClient.getInstance().worldRenderer.blockBreakingInfos.int2ObjectEntrySet()) {
            if (entry.getValue().getPos().equals(target)) {
                progress = entry.getValue();
                break;
            }
        }
        if (progress == null) {
            return;
        }
        // determine extra blocks to highlight
        BlockState state = world.getBlockState(target);
        // must not be broken, and the tool definition must be effective
        if (!IsEffectiveToolHook.isEffective(tool, state)) {
            return;
        }
        Iterator<BlockPos> extraBlocks = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, stack, player, state, world, target, blockTrace.getSide(), AreaOfEffectIterator.AOEMatchType.BREAKING).iterator();
        if (!extraBlocks.hasNext()) {
            return;
        }

        // set up buffers
        MatrixStack matrices = event.getPoseStack();
        matrices.push();
        VertexConsumerProvider.Immediate vertices = event.getLevelRenderer().renderBuffers.crumblingBufferSource();
        VertexConsumer vertexBuilder = vertices.getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(progress.getStage()));

        // finally, render the blocks
        Camera renderInfo = MinecraftClient.getInstance().gameRenderer.getCamera();
        double x = renderInfo.getPos().x;
        double y = renderInfo.getPos().y;
        double z = renderInfo.getPos().z;
        BlockRenderManager dispatcher = MinecraftClient.getInstance().getBlockRenderManager();
        int rendered = 0;
        do {
            BlockPos pos = extraBlocks.next();
            matrices.push();
            matrices.translate(pos.getX() - x, pos.getY() - y, pos.getZ() - z);
            MatrixStack.Entry entry = matrices.peek();
            VertexConsumer blockBuilder = new OverlayVertexConsumer(vertexBuilder, entry.getPositionMatrix(), entry.getNormalMatrix());
            dispatcher.renderDamage(world.getBlockState(pos), pos, world, matrices, blockBuilder);
            matrices.pop();
            rendered++;
        } while (rendered < MAX_BLOCKS && extraBlocks.hasNext());
        // finish rendering
        matrices.pop();
        vertices.draw();
    }
}
