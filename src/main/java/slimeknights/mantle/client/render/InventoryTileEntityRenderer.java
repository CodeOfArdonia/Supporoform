package slimeknights.mantle.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import slimeknights.mantle.client.model.inventory.InventoryModel;
import slimeknights.mantle.client.model.inventory.ModelItem;
import slimeknights.mantle.client.model.util.ModelHelper;

import java.util.List;

public class InventoryTileEntityRenderer<T extends BlockEntity & Inventory> implements BlockEntityRenderer<T> {

    public InventoryTileEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(T inventory, float partialTicks, MatrixStack matrices, VertexConsumerProvider buffer, int light, int combinedOverlayIn) {
        if (inventory.isEmpty()) return;

        // first, find the model for item display locations
        BlockState state = inventory.getCachedState();
        InventoryModel.Baked model = ModelHelper.getBakedModel(state, InventoryModel.Baked.class);
        if (model != null) {
            // if the block is rotatable, rotate item display
            boolean isRotated = RenderingHelper.applyRotation(matrices, state);

            // render items
            List<ModelItem> modelItems = model.getItems();
            for (int i = 0; i < modelItems.size(); i++) {
                RenderingHelper.renderItem(matrices, buffer, inventory.getStack(i), modelItems.get(i), light);
            }

            // pop back rotation
            if (isRotated) {
                matrices.pop();
            }
        }
    }
}
