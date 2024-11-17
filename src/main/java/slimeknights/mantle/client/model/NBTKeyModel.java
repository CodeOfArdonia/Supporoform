package slimeknights.mantle.client.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraftforge.client.model.geometry.BlockGeometryBakingContext;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.util.JsonHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Model which uses a key in NBT to select which texture variant to load.
 */
@RequiredArgsConstructor
public class NBTKeyModel implements IUnbakedGeometry<NBTKeyModel> {
    /**
     * Model loader instance
     */
    public static final IGeometryLoader<NBTKeyModel> LOADER = NBTKeyModel::deserialize;

    /**
     * Map of statically registered extra textures, used for addon mods
     */
    private static final Multimap<Identifier, Pair<String, Identifier>> EXTRA_TEXTURES = HashMultimap.create();

    /**
     * Registers an extra variant texture for the model with the given key. Note that resource packs can override the extra texture
     *
     * @param key         Model key, should be defined in the model JSON if supported
     * @param textureName Name of the texture defined, corresponds to a possible value of the NBT key
     * @param texture     Texture to use, same format as in resource packs
     */
    @SuppressWarnings("unused")  // API
    public static void registerExtraTexture(Identifier key, String textureName, Identifier texture) {
        EXTRA_TEXTURES.put(key, Pair.of(textureName, texture));
    }

    /**
     * Key to check in item NBT
     */
    private final String nbtKey;
    /**
     * Key denoting which extra textures to fetch from the map
     */
    @Nullable
    private final Identifier extraTexturesKey;

    /**
     * Map of textures for the model
     */
    private Map<String, SpriteIdentifier> textures = Collections.emptyMap();

    @Override
    public void resolveParents(Function<Identifier, UnbakedModel> modelGetter, JsonUnbakedModel owner) {
        this.textures = new HashMap<>();
        // must have a default
        SpriteIdentifier defaultTexture = owner.resolveSprite("default");
        this.textures.put("default", defaultTexture);
        // fetch others, not sure if there is a better way to get all defined textures
        if (owner instanceof BlockGeometryBakingContext blockContext) {
            ModelTextureIteratable iterable = new ModelTextureIteratable(null, blockContext.owner);
            for (Map<String, Either<SpriteIdentifier, String>> map : iterable) {
                for (String key : map.keySet()) {
                    if (!this.textures.containsKey(key) && owner.hasMaterial(key)) {
                        this.textures.put(key, owner.getMaterial(key));
                    }
                }
            }
        }
        // fetch extra textures
        if (this.extraTexturesKey != null) {
            for (Pair<String, Identifier> extra : EXTRA_TEXTURES.get(this.extraTexturesKey)) {
                String key = extra.getFirst();
                if (!this.textures.containsKey(key)) {
                    this.textures.put(key, new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, extra.getSecond()));
                }
            }
        }
    }

    /**
     * Bakes a model for the given texture
     */
    private static BakedModel bakeModel(JsonUnbakedModel owner, SpriteIdentifier texture, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation rotation, ModelOverrideList overrides, boolean b) {
        Sprite sprite = spriteGetter.apply(texture);
        CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(owner, b, sprite, overrides, owner.getTransformations());
        builder.addQuads(MantleItemLayerModel.getDefaultRenderType(owner), MantleItemLayerModel.getQuadsForSprite(-1, -1, sprite, rotation, 0));
        return builder.build();
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelOverrideList overrides, Identifier modelLocation, boolean b) {
        // setup transforms
        AffineTransformation transform = MantleItemLayerModel.applyTransform(modelTransform, owner.getRootTransform()).getRotation();
        // build variants map
        ImmutableMap.Builder<String, BakedModel> variants = ImmutableMap.builder();
        for (Entry<String, SpriteIdentifier> entry : this.textures.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("default")) {
                variants.put(key, bakeModel(owner, entry.getValue(), spriteGetter, transform, ModelOverrideList.EMPTY, b));
            }
        }
        return bakeModel(owner, this.textures.get("default"), spriteGetter, transform, new Overrides(this.nbtKey, this.textures, variants.build()), b);
    }

    /**
     * Overrides list for a tool slot item model
     */
    @RequiredArgsConstructor
    public static class Overrides extends ModelOverrideList {
        private final String nbtKey;
        private final Map<String, SpriteIdentifier> textures;
        private final Map<String, BakedModel> variants;

        @Override
        public BakedModel apply(BakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity, int pSeed) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains(this.nbtKey)) {
                return this.variants.getOrDefault(nbt.getString(this.nbtKey), model);
            }
            return model;
        }

        /**
         * Gets the given texture from the model
         */
        @SuppressWarnings("unused")  // API usage
        public SpriteIdentifier getTexture(String name) {
            SpriteIdentifier texture = this.textures.get(name);
            return texture != null ? texture : this.textures.get("default");
        }
    }

    /**
     * Deserializes this model from JSON
     */
    public static NBTKeyModel deserialize(JsonObject json, JsonDeserializationContext context) {
        String key = net.minecraft.util.JsonHelper.getString(json, "nbt_key");
        Identifier extraTexturesKey = null;
        if (json.has("extra_textures_key")) {
            extraTexturesKey = JsonHelper.getResourceLocation(json, "extra_textures_key");
        }
        return new NBTKeyModel(key, extraTexturesKey);
    }
}
