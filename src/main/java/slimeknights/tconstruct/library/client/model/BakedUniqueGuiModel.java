package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * Wrapper that swaps the model for the GUI
 */
public class BakedUniqueGuiModel extends BakedModelWrapper<BakedModel> {

    private final BakedModel gui;

    public BakedUniqueGuiModel(BakedModel base, BakedModel gui) {
        super(base);
        this.gui = gui;
    }

    @Override
    public BakedModel applyTransform(TransformType cameraTransformType, MatrixStack mat, boolean applyLeftHandTransform) {
        if (cameraTransformType == TransformType.GUI) {
            return this.gui.applyTransform(cameraTransformType, mat, applyLeftHandTransform);
        }
        return originalModel.applyTransform(cameraTransformType, mat, applyLeftHandTransform);
    }
}
