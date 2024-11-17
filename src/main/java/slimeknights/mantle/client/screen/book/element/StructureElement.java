package slimeknights.mantle.client.screen.book.element;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import slimeknights.mantle.client.book.structure.StructureInfo;
import slimeknights.mantle.client.book.structure.level.TemplateLevel;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.screen.book.BookScreen;

import java.util.List;
import java.util.stream.IntStream;

public class StructureElement extends SizedBookElement {

    public boolean canTick = false;

    public float scale = 50f;
    public float transX = 0;
    public float transY = 0;
    public AffineTransformation additionalTransform;
    public final StructureInfo renderInfo;
    public final TemplateLevel structureWorld;

    public long lastStep = -1;
    public long lastPrintedErrorTimeMs = -1;

    public StructureElement(int x, int y, int width, int height, StructureTemplate template, List<StructureTemplate.StructureBlockInfo> structure) {
        super(x, y, width, height);

        int[] size = {template.getSize().getX(), template.getSize().getY(), template.getSize().getZ()};

        this.scale = 100f / (float) IntStream.of(size).max().getAsInt();

        float sx = (float) width / (float) BookScreen.PAGE_WIDTH;
        float sy = (float) height / (float) BookScreen.PAGE_HEIGHT;

        this.scale *= Math.min(sx, sy);

        this.renderInfo = new StructureInfo(structure);

        this.structureWorld = new TemplateLevel(structure, this.renderInfo);

        this.transX = x + width / 2F;
        this.transY = y + height / 2F;

        this.additionalTransform = new AffineTransformation(null, new Quaternionf().rotateYXZ(0, (float) (25 * Math.PI / 180f), 0), null, new Quaternionf().rotateYXZ((float) (-45 * Math.PI / 180f), 0, 0));
    }

    @Override
    public void draw(DrawContext graphics, int mouseX, int mouseY, float partialTicks, TextRenderer fontRenderer) {
        VertexConsumerProvider.Immediate buffer = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        MatrixStack transform = graphics.getMatrices();
        MatrixStack.Entry lastEntryBeforeTry = transform.peek();

        try {
            long currentTime = System.currentTimeMillis();

            if (this.lastStep < 0)
                this.lastStep = currentTime;
            else if (this.canTick && currentTime - this.lastStep > 200) {
                this.renderInfo.step();
                this.lastStep = currentTime;
            }

            if (!this.canTick) {
                this.renderInfo.reset();
            }

            int structureLength = this.renderInfo.structureLength;
            int structureWidth = this.renderInfo.structureWidth;
            int structureHeight = this.renderInfo.structureHeight;

            transform.push();

            final BlockRenderManager blockRender = MinecraftClient.getInstance().getBlockRenderManager();

            transform.translate(this.transX, this.transY, Math.max(structureHeight, Math.max(structureWidth, structureLength)));
            transform.scale(this.scale, -this.scale, 1);
            transform.pushTransformation(this.additionalTransform);
            transform.multiply(new Quaternionf().rotateYXZ(0, 0, 0));

            transform.translate(structureLength / -2f, structureHeight / -2f, structureWidth / -2f);

            for (int h = 0; h < structureHeight; h++) {
                for (int l = 0; l < structureLength; l++) {
                    for (int w = 0; w < structureWidth; w++) {
                        BlockPos pos = new BlockPos(l, h, w);
                        BlockState state = this.structureWorld.getBlockState(pos);

                        if (!state.isAir()) {
                            transform.push();
                            transform.translate(l, h, w);

                            int overlay;

                            if (pos.equals(new BlockPos(1, 1, 1)))
                                overlay = OverlayTexture.getUv(0, true);
                            else
                                overlay = OverlayTexture.DEFAULT_UV;

                            ModelData modelData = ModelData.EMPTY;
                            BlockEntity te = this.structureWorld.getBlockEntity(pos);

                            if (te != null) {
                                modelData = te.getModelData();
                            }

                            // TODO: verify that we should be using all types here
                            BakedModel model = blockRender.getModel(state);
                            for (RenderLayer renderType : model.getRenderTypes(state, this.structureWorld.random, modelData)) {
                                blockRender.getModelRenderer().render(
                                        this.structureWorld, blockRender.getModel(state), state, pos, transform,
                                        buffer.getBuffer(MantleRenderTypes.TRANSLUCENT_FULLBRIGHT), false, this.structureWorld.random, state.getRenderingSeed(pos),
                                        overlay, modelData, renderType);
                            }

                            transform.pop();
                        }
                    }
                }
            }

            transform.pop();
            transform.pop();

        } catch (Exception e) {
            final long now = System.currentTimeMillis();

            if (now > this.lastPrintedErrorTimeMs + 1000) {
                e.printStackTrace();
                this.lastPrintedErrorTimeMs = now;
            }

            while (lastEntryBeforeTry != transform.peek())
                transform.pop();
        }

        buffer.draw();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseDragged(double clickX, double clickY, double mouseX, double mouseY, double lastX, double lastY, int button) {
        double dx = mouseX - lastX;
        double dy = mouseY - lastY;
        this.additionalTransform = this.forRotation(dx * 80D / 104, dy * 0.8).multiply(this.additionalTransform);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int clickedMouseButton) {
        super.mouseReleased(mouseX, mouseY, clickedMouseButton);
    }

    private AffineTransformation forRotation(double rX, double rY) {
        Vector3f axis = new Vector3f((float) rY, (float) rX, 0);
        float dot = axis.dot(axis);
        if (dot < Float.MIN_NORMAL) {
            return AffineTransformation.identity();
        }

        float angle = (float) (Math.sqrt(axis.dot(axis)) * Math.PI / 180f);
        axis.normalize();
        return new AffineTransformation(null, new Quaternionf(new AxisAngle4f(angle, axis)), null, null);
    }
}
