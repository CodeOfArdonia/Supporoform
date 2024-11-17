package slimeknights.tconstruct.library.client.model.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AllArgsConstructor;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.DynamicTextureLoader;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Model for an item with material texture variants, such as tool parts. Used only for single material items, {@link ToolModel} is used for multi-material items.
 */
@AllArgsConstructor
public class MaterialModel implements IUnbakedGeometry<MaterialModel> {
    /**
     * Shared loader instance
     */
    public static final IGeometryLoader<MaterialModel> LOADER = MaterialModel::deserialize;

    /**
     * If null, uses dynamic material
     */
    @Nullable
    private final MaterialVariantId material;
    /**
     * Tint index and index of part in tool
     */
    private final int index;
    /**
     * Transform matrix to apply to child parts
     */
    private final Vec2f offset;

    @Override
    public Collection<SpriteIdentifier> getMaterials(JsonUnbakedModel owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<SpriteIdentifier> allTextures = Sets.newHashSet();
        getMaterialTextures(allTextures, owner, "texture", material);
        return allTextures;
    }

    /**
     * Gets the list of material textures for the given owner texture
     *
     * @param allTextures Collection of textures
     * @param owner       Model owner
     * @param textureName Texture name to add
     * @param material    List of materials
     */
    public static void getMaterialTextures(Collection<SpriteIdentifier> allTextures, JsonUnbakedModel owner, String textureName, @Nullable MaterialVariantId material) {
        SpriteIdentifier texture = owner.resolveSprite(textureName);
        allTextures.add(texture);

        // if the texture is missing, stop here
        if (!MissingSprite.getMissingSpriteId().equals(texture.getTextureId())) {
            // texture should exist in item/tool, or the validator cannot handle them
            Predicate<SpriteIdentifier> textureAdder = DynamicTextureLoader.getTextureAdder(allTextures, Config.CLIENT.logMissingMaterialTextures.get());
            // if no specific material is set, load all materials as dependencies. If just one material, use just that one
            if (material == null) {
                MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().forEach(info -> info.getTextureDependencies(textureAdder, texture));
            } else {
                MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material).ifPresent(info -> info.getTextureDependencies(textureAdder, texture));
            }
        }
    }

    /**
     * Gets the tinted sprite info for the given material
     *
     * @param spriteGetter Sprite getter instance
     * @param texture      Base texture
     * @param material     Material variant
     * @return Tinted sprite or fallback
     */
    public static TintedSprite getMaterialSprite(Function<SpriteIdentifier, Sprite> spriteGetter, SpriteIdentifier texture, MaterialVariantId material) {
        // if the base material is non-null, try to find the sprite for that material
        // first, find a render info
        Optional<MaterialRenderInfo> optional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
        if (optional.isPresent()) {
            return optional.get().getSprite(texture, spriteGetter);
        }
        return new TintedSprite(spriteGetter.apply(texture), -1, 0);
    }

    /**
     * Gets quads for the given material variant of the texture
     *
     * @param spriteGetter   Sprite getter instance
     * @param texture        Base texture
     * @param material       Material variant
     * @param tintIndex      Tint index for quads
     * @param transformation Transformation to apply
     * @param pixels         Pixels to prevent z-fighting for multiple layers
     * @return Quad list
     */
    public static ImmutableList<BakedQuad> getQuadsForMaterial(Function<SpriteIdentifier, Sprite> spriteGetter, SpriteIdentifier texture, MaterialVariantId material, int tintIndex, AffineTransformation transformation, @Nullable ItemLayerPixels pixels) {
        TintedSprite sprite = getMaterialSprite(spriteGetter, texture, material);
        return MantleItemLayerModel.getQuadsForSprite(sprite.color(), tintIndex, sprite.sprite(), transformation, sprite.emissivity(), pixels);
    }

    /**
     * Same as {@link #bake} , but uses fewer arguments and does not require an instance
     *
     * @param owner        Model configuration
     * @param spriteGetter Sprite getter function
     * @param transform    Transform to apply to the quad fetching. Should not include rotation or it will look wrong in UIs
     * @param material     Material used, if null uses default
     * @param index        Tint index to use if tinted sprite is used
     * @param overrides    Override instance to use, will either be empty or {@link MaterialOverrideHandler}
     * @return Baked model
     */
    private static BakedModel bakeInternal(JsonUnbakedModel owner, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transform, MaterialVariantId material, int index, ModelOverrideList overrides, boolean b) {
        TintedSprite materialSprite = getMaterialSprite(spriteGetter, owner.resolveSprite("texture"), material);
        CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(owner, b, materialSprite.sprite(), overrides, owner.getTransformations());
        // TODO: let material choose its render type
        builder.addQuads(MantleItemLayerModel.getDefaultRenderType(owner), MantleItemLayerModel.getQuadsForSprite(materialSprite.color(), index, materialSprite.sprite(), transform, materialSprite.emissivity()));
        return builder.build();
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelOverrideList vanillaOverrides, Identifier modelLocation, boolean b) {
        // create transforms from offset
        // TODO: figure out forge transforms, can I use them here?
        AffineTransformation transforms;
        if (Vec2f.ZERO.equals(offset)) {
            transforms = AffineTransformation.identity();
        } else {
            // divide by 16 to convert from pixels to base values
            // negate Y as positive is up for transforms but down for pixels
            transforms = new AffineTransformation(new Vector3f(offset.x / 16, -offset.y / 16, 0), null, null, null);
        }

        // if the material is already set, no need to set overrides
        ModelOverrideList overrides = ModelOverrideList.EMPTY;
        if (material == null) {
            overrides = new MaterialOverrideHandler(owner, index, transforms);
        }

        // after that its base logic
        return bakeInternal(owner, spriteGetter, transforms, Objects.requireNonNullElse(material, IMaterial.UNKNOWN_ID), index, overrides);
    }

    /**
     * Dynamic override handler to swap in the material texture
     */
    private static final class MaterialOverrideHandler extends ModelOverrideList {
        // contains all the baked models since they'll never change, cleared automatically as the baked model is discarded
        private final Map<MaterialVariantId, BakedModel> cache = new ConcurrentHashMap<>();

        // parameters needed for rebaking
        private final JsonUnbakedModel owner;
        private final int index;
        private final AffineTransformation itemTransform;

        private MaterialOverrideHandler(JsonUnbakedModel owner, int index, AffineTransformation itemTransform) {
            this.owner = owner;
            this.index = index;
            this.itemTransform = itemTransform;
        }

        @Override
        public BakedModel apply(BakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
            // fetch the material from the stack
            MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
            // cache all baked material models, they will not need to be recreated as materials will not change
            return cache.computeIfAbsent(material, this::bakeDynamic);
        }

        /**
         * Bakes a copy of this model using the given material
         *
         * @param material New material for the model
         * @return Baked model
         */
        private BakedModel bakeDynamic(MaterialVariantId material) {
            // bake internal does not require an instance to bake, we can pass in whatever material we want
            // use empty override list as the sub model never calls overrides, and already has a material
            return bakeInternal(owner, SpriteIdentifier::getSprite, itemTransform, material, this.index, ModelOverrideList.EMPTY);
        }
    }


    /* Helpers */

    /**
     * Loads a material model from JSON
     */
    public static MaterialModel deserialize(JsonObject json, JsonDeserializationContext context) {
        // need tint index for tool models, doubles as part index
        int index = JsonHelper.getInt(json, "index", 0);

        // static material can be defined, if unset uses dynamic material
        MaterialVariantId material = null;
        if (json.has("material")) {
            material = MaterialVariantId.fromJson(json, "material");
        }

        Vec2f offset = Vec2f.ZERO;
        if (json.has("offset")) {
            offset = getVec2(json, "offset");
        }

        return new MaterialModel(material, index, offset);
    }

    /**
     * Converts a JSON float array to the specified object
     *
     * @param json JSON object
     * @param name Name of the array in the object to fetch
     * @return Vector3f of data
     * @throws JsonParseException If there is no array or the length is wrong
     */
    public static Vec2f getVec2(JsonObject json, String name) {
        JsonArray array = JsonHelper.getArray(json, name);
        if (array.size() != 2) {
            throw new JsonParseException("Expected " + 2 + " " + name + " values, found: " + array.size());
        }
        float[] vec = new float[2];
        for (int i = 0; i < 2; ++i) {
            vec[i] = JsonHelper.asFloat(array.get(i), name + "[" + i + "]");
        }
        return new Vec2f(vec[0], vec[1]);
    }
}
