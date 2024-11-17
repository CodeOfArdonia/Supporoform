package slimeknights.mantle.client.model.util;

import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper around a {@link IGeometryBakingContext} instance to allow easier extending, mostly for dynamic textures
 */
@SuppressWarnings("WeakerAccess")
public class GeometryContextWrapper extends JsonUnbakedModel {
    private final JsonUnbakedModel base;

    /**
     * Creates a new configuration wrapper
     *
     * @param base Base model configuration
     */
    public GeometryContextWrapper(JsonUnbakedModel base) {
        super(base.parentId,base.getElements(),base.getElements(),base.useAmbientOcclusion(),base.);
        this.base = base;
    }

    @Override
    public String getModelName() {
        return this.base.getModelName();
    }

    @Override
    public boolean hasMaterial(String name) {
        return this.base.hasMaterial(name);
    }

    @Override
    public SpriteIdentifier getMaterial(String name) {
        return this.base.getMaterial(name);
    }

    @Override
    public boolean isGui3d() {
        return this.base.isGui3d();
    }

    @Override
    public boolean useBlockLight() {
        return this.base.useBlockLight();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.base.useAmbientOcclusion();
    }

    @Override
    public ModelTransformation getTransforms() {
        return this.base.getTransforms();
    }

    @Override
    public AffineTransformation getRootTransform() {
        return this.base.getRootTransform();
    }

    @Override
    public @Nullable Identifier getRenderTypeHint() {
        return this.base.getRenderTypeHint();
    }

    @Override
    public boolean isComponentVisible(String component, boolean fallback) {
        return this.base.isComponentVisible(component, fallback);
    }

    @Override
    public RenderTypeGroup getRenderType(Identifier name) {
        return this.base.getRenderType(name);
    }
}
