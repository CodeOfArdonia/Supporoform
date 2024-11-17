package slimeknights.mantle.client.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.model.util.*;
import slimeknights.mantle.util.RetexturedHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Model that dynamically retextures a list of textures based on data from {@link RetexturedHelper}.
 */
@SuppressWarnings("WeakerAccess")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class RetexturedModel implements IUnbakedGeometry<RetexturedModel> {
    /**
     * Loader instance
     */
    public static IGeometryLoader<RetexturedModel> LOADER = RetexturedModel::deserialize;

    private final SimpleBlockModel model;
    private final Set<String> retextured;

    @Override
    public void resolveParents(Function<Identifier, UnbakedModel> modelGetter, JsonUnbakedModel context) {
        this.model.resolveParents(modelGetter, context);
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location, boolean b) {
        // bake the model and return
        BakedModel baked = this.model.bake(owner, baker, spriteGetter, transform, overrides, location);
        return new Baked(baked, owner, this.model, transform, getAllRetextured(owner, this.model, this.retextured));
    }

    /**
     * Gets a list of all names to retexture based on the block model texture references
     *
     * @param owner       Model config instance
     * @param model       Model fallback
     * @param originalSet Original list of names to retexture
     * @return Set of textures including parent textures
     */
    public static Set<String> getAllRetextured(JsonUnbakedModel owner, SimpleBlockModel model, Set<String> originalSet) {
        Set<String> retextured = Sets.newHashSet(originalSet);
        for (Map<String, Either<SpriteIdentifier, String>> textures : ModelTextureIteratable.of(owner, model)) {
            textures.forEach((name, either) ->
                    either.ifRight(parent -> {
                        if (retextured.contains(parent)) {
                            retextured.add(name);
                        }
                    })
            );
        }
        return ImmutableSet.copyOf(retextured);
    }

    /**
     * Deserializes a retextured model from JSON
     */
    public static RetexturedModel deserialize(JsonObject json, JsonDeserializationContext context) {
        // get base model
        ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
        // get list of textures to retexture
        Set<String> retextured = getRetexturedNames(json);
        // return retextured model
        return new RetexturedModel(model, retextured);
    }

    /**
     * Gets the list of retextured textures from the model
     *
     * @param json Model json
     * @return List of textures
     */
    public static Set<String> getRetexturedNames(JsonObject json) {
        if (json.has("retextured")) {
            // if an array, set from each texture in array
            JsonElement retextured = json.get("retextured");
            if (retextured.isJsonArray()) {
                JsonArray array = retextured.getAsJsonArray();
                if (array.size() == 0) {
                    throw new JsonSyntaxException("Must have at least one texture in retextured");
                }
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                for (int i = 0; i < array.size(); i++) {
                    builder.add(JsonHelper.asString(array.get(i), "retextured[" + i + "]"));
                }
                return builder.build();
            }
            // if string, single texture
            if (retextured.isJsonPrimitive()) {
                return ImmutableSet.of(retextured.getAsString());
            }
        }
        // if neither or missing, error
        throw new JsonSyntaxException("Missing retextured, expected to find a String or a JsonArray");
    }

    /**
     * Baked variant of the model, used to swap out quads based on the texture
     */
    public static class Baked extends DynamicBakedWrapper<BakedModel> {
        /**
         * Cache of texture name to baked model
         */
        private final Map<Identifier, BakedModel> cache = new ConcurrentHashMap<>();
        /* Properties for rebaking */
        private final JsonUnbakedModel owner;
        private final SimpleBlockModel model;
        private final ModelBakeSettings transform;
        /**
         * List of texture names that are retextured
         */
        private final Set<String> retextured;

        public Baked(BakedModel baked, JsonUnbakedModel owner, SimpleBlockModel model, ModelBakeSettings transform, Set<String> retextured) {
            super(baked);
            this.model = model;
            this.owner = owner;
            this.transform = transform;
            this.retextured = retextured;
        }

        /**
         * Gets the model with the given texture applied
         *
         * @param name Texture location
         * @return Retextured model
         */
        private BakedModel getRetexturedModel(Identifier name) {
            return this.model.bakeDynamic(new RetexturedContext(this.owner, this.retextured, name), this.transform);
        }

        /**
         * Gets a cached retextured model, computing it if missing from the cache
         *
         * @param block Block determining the texture
         * @return Retextured model
         */
        private BakedModel getCachedModel(Block block) {
            return this.cache.computeIfAbsent(ModelHelper.getParticleTexture(block), this::getRetexturedModel);
        }

        @Override
        public Sprite getParticleIcon(ModelData data) {
            // if particle is retextured, fetch particle from the cached model
            if (this.retextured.contains("particle")) {
                Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
                if (block != null) {
                    return this.getCachedModel(block).getParticleSprite(data);
                }
            }
            return originalModel.getParticleIcon(data);
        }

        @NotNull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, Random random, ModelData data, @Nullable RenderLayer renderType) {
            Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
            if (block == null) {
                return originalModel.getQuads(state, direction, random, data, null);
            }
            return this.getCachedModel(block).getQuads(state, direction, random, data, null);
        }

        @Override
        public ModelOverrideList getOverrides() {
            return RetexturedOverride.INSTANCE;
        }
    }

    /**
     * Model configuration wrapper to retexture the block
     */
    public static class RetexturedContext extends GeometryContextWrapper {
        /**
         * List of textures to retexture
         */
        private final Set<String> retextured;
        /**
         * Replacement texture
         */
        private final SpriteIdentifier texture;

        /**
         * Creates a new configuration wrapper
         *
         * @param base       Original model configuration
         * @param retextured Set of textures that should be retextured
         * @param texture    New texture to replace those in the set
         */
        public RetexturedContext(JsonUnbakedModel base, Set<String> retextured, Identifier texture) {
            super(base);
            this.retextured = retextured;
            this.texture = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, texture);
        }

        @Override
        public boolean hasMaterial(String name) {
            if (this.retextured.contains(name)) {
                return !MissingSprite.getMissingSpriteId().equals(this.texture.getTextureId());
            }
            return super.hasMaterial(name);
        }

        @Override
        public SpriteIdentifier getMaterial(String name) {
            if (this.retextured.contains(name)) {
                return this.texture;
            }
            return super.getMaterial(name);
        }
    }

    /**
     * Override list to swap the texture in from NBT
     */
    private static class RetexturedOverride extends ModelOverrideList {
        private static final RetexturedOverride INSTANCE = new RetexturedOverride();

        @Nullable
        @Override
        public BakedModel apply(BakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int pSeed) {
            if (stack.isEmpty() || !stack.hasNbt()) {
                return originalModel;
            }

            // get the block first, ensuring its valid
            Block block = RetexturedHelper.getTexture(stack);
            if (block == Blocks.AIR) {
                return originalModel;
            }

            // if valid, use the block
            return ((Baked) originalModel).getCachedModel(block);
        }
    }
}
