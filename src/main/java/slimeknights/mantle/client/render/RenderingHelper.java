package slimeknights.mantle.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.inventory.ModelItem;

@SuppressWarnings("WeakerAccess")
public class RenderingHelper {
    /* Rotation */

    /**
     * Applies horizontal rotation to the given TESR
     *
     * @param matrices Matrix stack
     * @param state    Block state, checked for {@link Properties#HORIZONTAL_FACING}
     * @return True if rotation was applied. Caller is expected to call {@link MatrixStack#pop()} if true
     */
    public static boolean applyRotation(MatrixStack matrices, BlockState state) {
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return applyRotation(matrices, state.get(Properties.HORIZONTAL_FACING));
        }
        return false;
    }

    /**
     * Applies horizontal rotation to the given TESR
     *
     * @param matrices Matrix stack
     * @param facing   Direction of rotation
     * @return True if rotation was applied. Caller is expected to call {@link MatrixStack#pop()} if true
     */
    public static boolean applyRotation(MatrixStack matrices, Direction facing) {
        // south has a facing of 0, no rotation needed
        if (facing.getAxis().isHorizontal() && facing != Direction.SOUTH) {
            matrices.push();
            matrices.translate(0.5, 0, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f * (facing.getHorizontal())));
            matrices.translate(-0.5, 0, -0.5);
            return true;
        }
        return false;
    }


    /* Items */

    /**
     * Renders a single item in a TESR
     *
     * @param matrices  Matrix stack inst ance
     * @param buffer    Buffer instance
     * @param item      Item to render
     * @param modelItem Model items for render information
     * @param light     Model light
     */
    public static void renderItem(MatrixStack matrices, VertexConsumerProvider buffer, ItemStack item, ModelItem modelItem, int light) {
        // if the item says skip, skip
        if (modelItem.isHidden()) return;
        // if no stack, skip
        if (item.isEmpty()) return;

        // start rendering
        matrices.push();
        Vector3f center = modelItem.getCenterScaled();
        matrices.translate(center.x(), center.y(), center.z());

        // scale
        float scale = modelItem.getSizeScaled();
        matrices.scale(scale, scale, scale);

        // rotate X, then Y
        float x = modelItem.getX();
        if (x != 0) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(x));
        }
        float y = modelItem.getY();
        if (y != 0) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(y));
        }

        // render the actual item
        MinecraftClient.getInstance().getItemRenderer().renderItem(item, modelItem.getTransform(), light, OverlayTexture.DEFAULT_UV, matrices, buffer, MinecraftClient.getInstance().world, 0);
        matrices.pop();
    }
}
