package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import lombok.Getter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.BasicBakedModel.Builder;
import net.minecraft.client.render.model.json.ModelRotation;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.LogicHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Block model for setting color, luminosity, and per element uv lock. Similar to {@link MantleItemLayerModel} but for blocks
 */
@SuppressWarnings("unused")  // API
public class ColoredBlockModel extends SimpleBlockModel {
    /**
     * Model loader to allow doing basic coloring outside of other models
     */
    public static final IGeometryLoader<SimpleBlockModel> LOADER = ColoredBlockModel::deserialize;

    /**
     * Colors to use for each piece
     */
    @Getter
    private final List<ColorData> colorData;

    /**
     * Creates a new colored block model
     *
     * @param parentLocation Location of the parent model, if unset has no parent
     * @param textures       List of textures for iteration, in case the owner is not BlockModel
     * @param parts          List of parts in the model
     * @param colorData      Additional information about colors in the model
     */
    public ColoredBlockModel(@Nullable Identifier parentLocation, Map<String, Either<SpriteIdentifier, String>> textures, List<ModelElement> parts, List<ColorData> colorData) {
        super(parentLocation, textures, parts);
        this.colorData = colorData;
    }

    public ColoredBlockModel(SimpleBlockModel base, List<ColorData> colorData) {
        super(base);
        this.colorData = colorData;
    }

    /**
     * Bakes a single part of the model into the builder
     *
     * @param builder         Baked model builder
     * @param owner           Model owner
     * @param part            Part to bake
     * @param emissivity      Emissivity for fullbright, -1 will leave forge in charge, 0-15 will override the forge value
     * @param spriteGetter    Sprite getter
     * @param transform       Transform for the face
     * @param quadTransformer Forge transformations for the face, this is notably where you should handle color transformations
     * @param uvlock          UV lock for the face, separated to allow overriding the model state
     * @param location        Model location
     */
    public static void bakePart(Builder builder, JsonUnbakedModel owner, ModelElement part, int emissivity, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transform, IQuadTransformer quadTransformer, boolean uvlock, Identifier location) {
        for (Direction direction : part.faces.keySet()) {
            ModelElementFace face = part.faces.get(direction);
            // ensure the name is not prefixed (it always is)
            String texture = face.textureId;
            if (texture.charAt(0) == '#') {
                texture = texture.substring(1);
            }
            // bake the face with the extra colors
            Sprite sprite = spriteGetter.apply(owner.resolveSprite(texture));
            BakedQuad quad = bakeFace(part, face, sprite, direction, transform, uvlock, emissivity, location);
            quadTransformer.processInPlace(quad);
            // apply cull face
            //noinspection ConstantConditions  the annotation is a liar
            if (face.cullFace == null) {
                builder.addQuad(quad);
            } else {
                builder.addQuad(Direction.transform(transform.getMatrix(), face.cullFace), quad);
            }
        }
    }

    /**
     * Bakes a list of block part elements into a model
     *
     * @param owner        Model configuration
     * @param elements     Model elements
     * @param spriteGetter Sprite getter instance
     * @param transform    Model transform
     * @param overrides    Model overrides
     * @param location     Model bake location
     * @return Baked model
     */
    public static BakedModel bakeModel(JsonUnbakedModel owner, List<ModelElement> elements, List<ColorData> colorData, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location) {
        // iterate parts, adding to the builder
        Sprite particle = spriteGetter.apply(owner.resolveSprite("particle"));
        Builder builder = bakedBuilder(owner, overrides).particle(particle);
        int size = elements.size();
        IQuadTransformer quadTransformer = applyTransform(transform, owner.getRootTransform());
        AffineTransformation transformation = transform.getRotation();
        boolean uvlock = transform.isUvLocked();
        for (int i = 0; i < size; i++) {
            ModelElement part = elements.get(i);
            ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
            if (colors.luminosity != -1 && !location.equals(BAKE_LOCATION)) {
                Mantle.logger.warn("Using deprecated 'luminosity' field on ColoredBlockModel color data for {}, this will be removed in 1.20 in favor of Forge's 'emissivity'.", location);
            }
            IQuadTransformer partTransformer = colors.color == -1 ? quadTransformer : quadTransformer.andThen(applyColorQuadTransformer(colors.color));
            bakePart(builder, owner, part, colors.luminosity, spriteGetter, transformation, partTransformer, colors.isUvLock(uvlock), location);
        }
        return builder.build(getRenderTypeGroup(owner));
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelOverrideList overrides, Identifier modelLocation, boolean b) {
        return bakeModel(owner, this.getElements(), this.colorData, spriteGetter, modelTransform, overrides, modelLocation);
    }

    @Override
    public BakedModel bakeDynamic(JsonUnbakedModel owner, ModelBakeSettings transform) {
        return bakeModel(owner, this.getElements(), this.colorData, SpriteIdentifier::getSprite, transform, ModelOverrideList.EMPTY, BAKE_LOCATION);
    }

    /**
     * Data class for setting properties when baking colored elements
     */
    public record ColorData(int color, @Deprecated int luminosity, @Nullable Boolean uvlock) {
        public static final ColorData DEFAULT = new ColorData(-1, -1, null);

        /**
         * Gets the UV lock for the given part
         */
        public boolean isUvLock(boolean defaultLock) {
            if (this.uvlock == null) {
                return defaultLock;
            }
            return this.uvlock;
        }

        /**
         * Parses the color data from JSON
         */
        public static ColorData fromJson(JsonObject json) {
            int color = ColorLoadable.ALPHA.getOrDefault(json, "color", -1);
            int luminosity = net.minecraft.util.JsonHelper.getInt(json, "luminosity", -1);
            Boolean uvlock = null;
            if (json.has("uvlock")) {
                uvlock = net.minecraft.util.JsonHelper.getBoolean(json, "uvlock");
            }
            return new ColorData(color, luminosity, uvlock);
        }
    }


    /* Deserializing */

    /**
     * Deserializes the model from JSON
     */
    public static ColoredBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
        SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
        List<ColorData> colorData = json.has("colors") ? JsonHelper.parseList(json, "colors", ColorData::fromJson) : Collections.emptyList();
        return new ColoredBlockModel(model, colorData);
    }


    /* Face bakery */

    /**
     * Converts an ARGB color to an ABGR color, as the commonly used color format is not the format colors end up packed into.
     * This function doubles as its own inverse, not that its needed.
     *
     * @param color ARGB color
     * @return ABGR color
     */
    public static int swapColorRedBlue(int color) {
        return (color & 0xFF00FF00) // alpha and green same spot
                | ((color >> 16) & 0x000000FF) // red moves to blue
                | ((color << 16) & 0x00FF0000); // blue moves to red
    }

    /**
     * Quad transformer applying a static color
     */
    public static RenderContext.QuadTransform applyColorQuadTransformer(int color) {
        int abgr = swapColorRedBlue(color);
        return quad -> {
            int[] vertices = quad.getVertices();
            for (int i = 0; i < 4; i++) {
                vertices[i * IQuadTransformer.STRIDE + IQuadTransformer.COLOR] = abgr;
            }
            return true;
        };
    }

    /**
     * Extension of {@code BlockModel#bakeFace(BlockPart, BlockPartFace, TextureAtlasSprite, Direction, IModelTransform, ResourceLocation)} with emissivity and UV lock arguments
     *
     * @param part       Part containing the face
     * @param face       Face data
     * @param sprite     Sprite for the face
     * @param facing     Direction of the face
     * @param transform  Transform for the face
     * @param uvlock     UV lock for the face, separated to allow overriding the model state
     * @param emissivity Emissivity for fullbright, -1 will leave forge in charge, 0-15 will override the forge value
     * @param location   Model location for errors
     */
    public static BakedQuad bakeFace(ModelElement part, ModelElementFace face, Sprite sprite, Direction facing, AffineTransformation transform, boolean uvlock, int emissivity, Identifier location) {
        return bakeQuad(part.from, part.to, face, sprite, facing, transform, uvlock, part.rotation, part.shade, emissivity, location);
    }

    /**
     * Extension of {@link BakedQuadFactory#bake(Vector3f, Vector3f, ModelElementFace, Sprite, Direction, ModelBakeSettings, ModelRotation, boolean, Identifier)} with emissivity and UV lock overrides
     *
     * @param posFrom      Face start position
     * @param posTo        Face end position
     * @param face         Face data
     * @param sprite       Sprite for the face
     * @param facing       Direction of the face
     * @param transform    Transform for the face
     * @param uvlock       UV lock for the face, separated to allow overriding the model state
     * @param partRotation Rotation for the part
     * @param shade        If true, shades the part
     * @param emissivity   Emissivity for fullbright, -1 will leave forge in charge, 0-15 will override the forge value
     * @param location     Model location for errors
     * @return Baked quad
     */
    public static BakedQuad bakeQuad(Vector3f posFrom, Vector3f posTo, ModelElementFace face, Sprite sprite,
                                     Direction facing, AffineTransformation transform, boolean uvlock, @Nullable ModelRotation partRotation,
                                     boolean shade, int emissivity, Identifier location) {
        ModelElementTexture faceUV = face.textureData;
        if (uvlock) {
            faceUV = BakedQuadFactory.uvLock(face.textureData, facing, transform, location);
        }

        float[] originalUV = new float[faceUV.uvs.length];
        System.arraycopy(faceUV.uvs, 0, originalUV, 0, originalUV.length);
        float shrinkRatio = sprite.getAnimationFrameDelta();
        float u = (faceUV.uvs[0] + faceUV.uvs[0] + faceUV.uvs[2] + faceUV.uvs[2]) / 4.0F;
        float v = (faceUV.uvs[1] + faceUV.uvs[1] + faceUV.uvs[3] + faceUV.uvs[3]) / 4.0F;
        faceUV.uvs[0] = MathHelper.lerp(shrinkRatio, faceUV.uvs[0], u);
        faceUV.uvs[2] = MathHelper.lerp(shrinkRatio, faceUV.uvs[2], u);
        faceUV.uvs[1] = MathHelper.lerp(shrinkRatio, faceUV.uvs[1], v);
        faceUV.uvs[3] = MathHelper.lerp(shrinkRatio, faceUV.uvs[3], v);

        // call the vanilla face bakery, we will pass in emmisivity and color via quad transformers
        // note that in prior versions of mantle we reimplemented the face bakery methods to pass in colors to the face baking directly
        int[] vertexData = FACE_BAKERY.makeVertices(faceUV, sprite, facing, FACE_BAKERY.setupShape(posFrom, posTo), transform, partRotation, shade);
        Direction direction = BakedQuadFactory.decodeDirection(vertexData);
        System.arraycopy(originalUV, 0, faceUV.uvs, 0, originalUV.length);
        if (partRotation == null) {
            FACE_BAKERY.recalculateWinding(vertexData, direction);
        }
        //noinspection UnstableApiUsage  We are replicating the vanilla method, so we call the forge method
        ForgeHooksClient.fillNormal(vertexData, direction);

        // bake final quad
        BakedQuad quad = new BakedQuad(vertexData, face.tintIndex, direction, sprite, shade);
        // use our override if specified, fallback to Forge
        // TODO: forge colors
        if (emissivity == -1) {
            emissivity = face.getFaceData().blockLight();
        }
        if (emissivity > 0) {
            QuadTransformers.settingEmissivity(emissivity).processInPlace(quad);
        }
        return quad;
    }
}
