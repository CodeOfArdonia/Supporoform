package slimeknights.mantle.client.model.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.Getter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.BasicBakedModel.Builder;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simpler version of {@link JsonUnbakedModel} for use in an {@link IUnbakedGeometry}, as the owner handles most block model properties
 */
@SuppressWarnings("WeakerAccess")
public class SimpleBlockModel implements IUnbakedGeometry<SimpleBlockModel> {
    /**
     * Model loader for vanilla block model, mainly intended for use in fallback registration
     */
    public static final IGeometryLoader<SimpleBlockModel> LOADER = SimpleBlockModel::deserialize;
    /**
     * Location used for baking dynamic models, name does not matter so just using a constant
     */
    static final Identifier BAKE_LOCATION = Mantle.getResource("dynamic_model_baking");

    /**
     * Parent model location, used to fetch parts and for textures if the owner is not a block model
     */
    @Getter
    @Nullable
    private Identifier parentLocation;
    /**
     * Model parts for baked model, if empty uses parent parts
     */
    private final List<ModelElement> parts;
    /**
     * Fallback textures in case the owner does not contain a block model
     */
    @Getter
    private final Map<String, Either<SpriteIdentifier, String>> textures;
    @Getter
    private JsonUnbakedModel parent;

    /**
     * Creates a new simple block model
     *
     * @param parentLocation Location of the parent model, if unset has no parent
     * @param textures       List of textures for iteration, in case the owner is not BlockModel
     * @param parts          List of parts in the model
     */
    public SimpleBlockModel(@Nullable Identifier parentLocation, Map<String, Either<SpriteIdentifier, String>> textures, List<ModelElement> parts) {
        this.parts = parts;
        this.textures = textures;
        this.parentLocation = parentLocation;
    }

    public SimpleBlockModel(SimpleBlockModel base) {
        this.parts = base.parts;
        this.textures = base.textures;
        this.parentLocation = base.parentLocation;
        this.parent = base.parent;
    }


    /* Properties */

    /**
     * Gets the elements in this simple block model
     *
     * @return Elements in the model
     */
    public List<ModelElement> getElements() {
        return this.parts.isEmpty() && this.parent != null ? this.parent.getElements() : this.parts;
    }

    /* Textures */

    @Override
    public void resolveParents(Function<Identifier, UnbakedModel> modelGetter, JsonUnbakedModel owner) {
        // no work if no parent or the parent is fetched already
        if (this.parent != null || this.parentLocation == null) {
            return;
        }

        // iterate through model parents
        Set<UnbakedModel> chain = Sets.newLinkedHashSet();

        // load the first model directly
        this.parent = getParent(modelGetter, chain, this.parentLocation, owner.getModelName());
        // null means no model, so set missing
        if (this.parent == null) {
            this.parent = getMissing(modelGetter);
            this.parentLocation = ModelLoader.MISSING_ID;
        }

        // loop through each parent, adding in parents
        for (JsonUnbakedModel link = this.parent; link.parentId != null && link.parent == null; link = link.parent) {
            chain.add(link);

            // fetch model parent
            link.parent = getParent(modelGetter, chain, link.parentId, link.id);

            // null means no model, so set missing
            if (link.parent == null) {
                link.parent = getMissing(modelGetter);
                link.parentId = ModelLoader.MISSING_ID;
            }
        }
    }

    /**
     * Gets the parent for a model
     *
     * @param modelGetter Model getter function
     * @param chain       Chain of models that are in progress
     * @param location    Location to fetch
     * @param name        Name of the model being fetched
     * @return Block model instance, null if there was an error
     */
    @Nullable
    private static JsonUnbakedModel getParent(Function<Identifier, UnbakedModel> modelGetter, Set<UnbakedModel> chain, Identifier location, String name) {
        // model must exist
        UnbakedModel unbaked = modelGetter.apply(location);
        if (unbaked == null) {
            Mantle.logger.warn("No parent '{}' while loading model '{}'", location, name);
            return null;
        }
        // no loops in chain
        if (chain.contains(unbaked)) {
            Mantle.logger.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", name, chain.stream().map(Object::toString).collect(Collectors.joining(" -> ")), location);
            return null;
        }
        // model must be block model, this is a serious error in vanilla
        if (!(unbaked instanceof JsonUnbakedModel)) {
            throw new IllegalStateException("BlockModel parent has to be a block model.");
        }
        return (JsonUnbakedModel) unbaked;
    }

    /**
     * Gets the missing model, ensuring its the right type
     *
     * @param modelGetter Model getter function
     * @return Missing model as a {@link JsonUnbakedModel}
     */
    @NotNull
    private static JsonUnbakedModel getMissing(Function<Identifier, UnbakedModel> modelGetter) {
        UnbakedModel model = modelGetter.apply(ModelLoader.MISSING_ID);
        if (!(model instanceof JsonUnbakedModel)) {
            throw new IllegalStateException("Failed to load missing model");
        }
        return (JsonUnbakedModel) model;
    }

    /* Baking */

    /**
     * Creates a new builder instance from the given context
     */
    public static Builder bakedBuilder(JsonUnbakedModel owner, ModelOverrideList overrides) {
        return new Builder(owner.useAmbientOcclusion(), owner.useBlockLight(), owner.isGui3d(), owner.getTransforms(), overrides);
    }

    /**
     * Bakes a single part of the model into the builder
     *
     * @param builder         Baked model builder
     * @param owner           Model owner
     * @param part            Part to bake
     * @param spriteGetter    Sprite getter
     * @param transform       Model transforms
     * @param quadTransformer Additional forge transforms
     * @param location        Model location
     */
    public static void bakePart(Builder builder, JsonUnbakedModel owner, ModelElement part, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, IQuadTransformer quadTransformer, Identifier location) {
        for (Direction direction : part.faces.keySet()) {
            ModelElementFace face = part.faces.get(direction);
            // ensure the name is not prefixed (it always is)
            String texture = face.textureId;
            if (texture.charAt(0) == '#') {
                texture = texture.substring(1);
            }
            // bake the face
            Sprite sprite = spriteGetter.apply(owner.resolveSprite(texture));
            BakedQuad bakedQuad = JsonUnbakedModel.createQuad(part, face, sprite, direction, transform, location);
            quadTransformer.processInPlace(bakedQuad);
            // apply cull face
            //noinspection ConstantConditions  Its nullable, just annotated wrongly
            if (face.cullFace == null) {
                builder.addQuad(bakedQuad);
            } else {
                builder.addQuad(Direction.transform(transform.getRotation().getMatrix(), face.cullFace), bakedQuad);
            }
        }
    }

    /**
     * Gets the render type group from the given model context
     */
    public static RenderTypeGroup getRenderTypeGroup(JsonUnbakedModel owner) {
        Identifier renderTypeHint = owner.getRenderTypeHint();
        return renderTypeHint != null ? owner.getRenderType(renderTypeHint) : RenderTypeGroup.EMPTY;
    }

    /**
     * Applies the transformation to the model state for an item layer model.
     */
    public static RenderContext.QuadTransform applyTransform(ModelBakeSettings modelState, AffineTransformation transformation) {
        if (transformation.isIdentity()) {
            return QuadTransformers.empty();
        } else {
            return UnbakedGeometryHelper.applyRootTransform(modelState, transformation);
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
    public static BakedModel bakeModel(JsonUnbakedModel owner, List<ModelElement> elements, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location) {
        // iterate parts, adding to the builder
        Sprite particle = spriteGetter.apply(owner.resolveSprite("particle"));
        Builder builder = bakedBuilder(owner, overrides).particle(particle);
        IQuadTransformer quadTransformer = applyTransform(transform, owner.getRootTransform());
        for (ModelElement part : elements) {
            bakePart(builder, owner, part, spriteGetter, transform, quadTransformer, location);
        }
        return builder.build(getRenderTypeGroup(owner));
    }

    /**
     * Same as {@link #bakeModel(IGeometryBakingContext, List, Function, ModelState, ItemOverrides, ResourceLocation)}, but passes in sensible defaults for values unneeded in dynamic models
     *
     * @param owner     Model configuration
     * @param elements  Elements to bake
     * @param transform Model transform
     * @return Baked model
     */
    public static BakedModel bakeDynamic(JsonUnbakedModel owner, List<ModelElement> elements, ModelBakeSettings transform) {
        return bakeModel(owner, elements, SpriteIdentifier::getSprite, transform, ModelOverrideList.EMPTY, BAKE_LOCATION);
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location) {
        return bakeModel(owner, this.getElements(), spriteGetter, transform, overrides, location);
    }

    /**
     * Same as {@link #bake(IGeometryBakingContext, ModelBaker, Function, ModelState, ItemOverrides, ResourceLocation)}, but passes in sensible defaults for values unneeded in dynamic models
     *
     * @param owner     Model configuration
     * @param transform Transform to apply
     * @return Baked model
     */
    public BakedModel bakeDynamic(JsonUnbakedModel owner, ModelBakeSettings transform) {
        return bakeDynamic(owner, this.getElements(), transform);
    }


    /* Deserializing */

    /**
     * Deserializes a SimpleBlockModel from JSON
     *
     * @param json    Json element containing the model
     * @param context Json Context
     * @return Serialized JSON
     */
    public static SimpleBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
        // parent, null if missing
        String parentName = JsonHelper.getString(json, "parent", "");
        Identifier parent = parentName.isEmpty() ? null : new Identifier(parentName);

        // textures, empty map if missing
        Map<String, Either<SpriteIdentifier, String>> textureMap;
        if (json.has("textures")) {
            ImmutableMap.Builder<String, Either<SpriteIdentifier, String>> builder = new ImmutableMap.Builder<>();
            Identifier atlas = PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
            JsonObject textures = JsonHelper.getObject(json, "textures");
            for (Entry<String, JsonElement> entry : textures.entrySet()) {
                builder.put(entry.getKey(), JsonUnbakedModel.Deserializer.resolveReference(atlas, entry.getValue().getAsString()));
            }
            textureMap = builder.build();
        } else {
            textureMap = Collections.emptyMap();
        }

        // elements, empty list if missing
        List<ModelElement> parts;
        if (json.has("elements")) {
            parts = getModelElements(context, JsonHelper.getArray(json, "elements"), "elements");
        } else {
            parts = Collections.emptyList();
        }
        return new SimpleBlockModel(parent, textureMap, parts);
    }

    /**
     * Gets a list of models from a JSON array
     *
     * @param context Json Context
     * @param array   Json array
     * @return Model list
     */
    public static List<ModelElement> getModelElements(JsonDeserializationContext context, JsonElement array, String name) {
        // if just one element, array is optional
        if (array.isJsonObject()) {
            return ImmutableList.of(context.deserialize(array.getAsJsonObject(), ModelElement.class));
        }
        // if an array, get array of elements
        if (array.isJsonArray()) {
            ImmutableList.Builder<ModelElement> builder = ImmutableList.builder();
            for (JsonElement json : array.getAsJsonArray()) {
                builder.add((ModelElement) context.deserialize(json, ModelElement.class));
            }
            return builder.build();
        }

        throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray or JsonObject");
    }
}
