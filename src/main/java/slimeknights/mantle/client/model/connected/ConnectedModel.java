package slimeknights.mantle.client.model.connected;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.IMultipartConnectedBlock;
import slimeknights.mantle.client.model.util.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Model that handles generating variants for connected textures
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ConnectedModel implements IUnbakedGeometry<ConnectedModel> {
    /**
     * Loader instance
     */
    public static IGeometryLoader<ConnectedModel> LOADER = ConnectedModel::deserialize;

    /**
     * Property of the connections cache key. Contains a 6 bit number with each bit representing a direction
     */
    private static final ModelProperty<Byte> CONNECTIONS = new ModelProperty<>();

    /**
     * Parent model
     */
    private final SimpleBlockModel model;
    /**
     * Map of texture name to index of suffixes (indexed as 0bENWS)
     */
    private final Map<String, String[]> connectedTextures;
    /**
     * Function to run to check if this block connects to another
     */
    private final BiPredicate<BlockState, BlockState> connectionPredicate;
    /**
     * List of sides to check when getting block directions
     */
    private final Set<Direction> sides;

    /**
     * Map of full texture name to the resulting material, filled during {@link #resolveParents(Function, IGeometryBakingContext)}
     */
    private Map<String, SpriteIdentifier> extraTextures;

    @Override
    public void resolveParents(Function<Identifier, UnbakedModel> modelGetter, JsonUnbakedModel owner) {
        this.model.resolveParents(modelGetter, owner);
        // for all connected textures, add suffix textures
        Map<String, SpriteIdentifier> extraTextures = new HashMap<>();
        for (Entry<String, String[]> entry : this.connectedTextures.entrySet()) {
            // fetch data from the base texture
            String name = entry.getKey();
            // skip if missing
            if (!owner.hasMaterial(name)) {
                continue;
            }
            SpriteIdentifier base = owner.getMaterial(name);
            Identifier atlas = base.getAtlasId();
            Identifier texture = base.getTextureId();
            String namespace = texture.getNamespace();
            String path = texture.getPath();

            // use base atlas and texture, but suffix the name
            String[] suffixes = entry.getValue();
            for (String suffix : suffixes) {
                if (suffix.isEmpty()) {
                    continue;
                }
                // skip running if we have seen it before
                String suffixedName = name + "_" + suffix;
                if (!extraTextures.containsKey(suffixedName)) {
                    SpriteIdentifier mat;
                    // allow overriding a specific texture
                    if (owner.hasMaterial(suffixedName)) {
                        mat = owner.getMaterial(suffixedName);
                    } else {
                        mat = new SpriteIdentifier(atlas, new Identifier(namespace, path + "/" + suffix));
                    }
                    // cache the texture name, we use it a lot in rebaking
                    extraTextures.put(suffixedName, mat);
                }
            }
        }
        // copy into immutable for better performance
        this.extraTextures = ImmutableMap.copyOf(extraTextures);
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location, boolean b) {
        BakedModel baked = this.model.bake(owner, baker, spriteGetter, transform, overrides, location);
        return new Baked(this, new ExtraTextureContext(owner, this.extraTextures), transform, baked);
    }

    @SuppressWarnings("WeakerAccess")
    protected static class Baked extends DynamicBakedWrapper<BakedModel> {
        private final ConnectedModel parent;
        private final JsonUnbakedModel owner;
        private final ModelBakeSettings transforms;
        private final BakedModel[] cache = new BakedModel[64];
        private final Map<String, String> nameMappingCache = new ConcurrentHashMap<>();
        private final ModelTextureIteratable modelTextures;

        public Baked(ConnectedModel parent, JsonUnbakedModel owner, ModelBakeSettings transforms, BakedModel baked) {
            super(baked);
            this.parent = parent;
            this.owner = owner;
            this.transforms = transforms;
            this.modelTextures = ModelTextureIteratable.of(owner, parent.model);
            // all directions false gives cache key of 0, that is ourself
            this.cache[0] = baked;
        }

        /**
         * Gets the direction rotated
         *
         * @param direction Original direction to rotate
         * @param rotation  Rotation origin, aka the face of the block we are looking at. As a result, UP is identity
         * @return Rotated direction
         */
        private static Direction rotateDirection(Direction direction, Direction rotation) {
            if (rotation == Direction.UP) {
                return direction;
            }
            if (rotation == Direction.DOWN) {
                // Z is backwards on the bottom
                if (direction.getAxis() == Axis.Z) {
                    return direction.getOpposite();
                }
                // X is normal
                return direction;
            }
            // sides all just have the next side for left and right, and consistent up and down
            switch (direction) {
                case NORTH:
                    return Direction.UP;
                case SOUTH:
                    return Direction.DOWN;
                case EAST:
                    return rotation.rotateYCounterclockwise();
                case WEST:
                    return rotation.rotateYClockwise();
            }
            throw new IllegalArgumentException("Direction must be horizontal axis");
        }

        /**
         * Gets a transform function based on the block part UV and block face
         *
         * @param face Block face in question
         * @param uv   Block UV data
         * @return Direction transform function
         */
        private static Function<Direction, Direction> getTransform(Direction face, ModelElementTexture uv) {
            // TODO: how do I apply UV lock?
            // final transform switches from face (NSWE) to world direction, the rest are composed in to apply first
            Function<Direction, Direction> transform = (d) -> rotateDirection(d, face);

            // flipping
            boolean flipV = uv.uvs[1] > uv.uvs[3];
            if (uv.uvs[0] > uv.uvs[2]) {
                // flip both
                if (flipV) {
                    transform = transform.compose(Direction::getOpposite);
                } else {
                    // flip U
                    transform = transform.compose((d) -> {
                        if (d.getAxis() == Axis.X) {
                            return d.getOpposite();
                        }
                        return d;
                    });
                }
            } else if (flipV) {
                transform = transform.compose((d) -> {
                    if (d.getAxis() == Axis.Z) {
                        return d.getOpposite();
                    }
                    return d;
                });
            }

            // rotation
            return switch (uv.rotation) {
                // 90 degrees
                case 90 -> transform.compose(Direction::rotateYClockwise);
                case 180 -> transform.compose(Direction::getOpposite);
                case 270 -> transform.compose(Direction::rotateYCounterclockwise);
                default -> transform;
            };
        }

        /**
         * Uncached variant of {@link #getConnectedName(String)}, used internally
         */
        private String getConnectedNameUncached(String key) {
            // otherwise, iterate into the parent models, trying to find a match
            String check = key;
            String found = "";
            for (Map<String, Either<SpriteIdentifier, String>> textures : this.modelTextures) {
                Either<SpriteIdentifier, String> either = textures.get(check);
                if (either != null) {
                    // if no name, its not connected
                    Optional<String> newName = either.right();
                    if (newName.isEmpty()) {
                        break;
                    }
                    // if the name is connected, we are done
                    check = newName.get();
                    if (this.parent.connectedTextures.containsKey(check)) {
                        found = check;
                        break;
                    }
                }
            }
            return found;
        }

        /**
         * Gets the name of this texture that supports connected textures, or null if never is connected
         *
         * @param key Name of the part texture
         * @return Name of the connected texture
         */
        private String getConnectedName(String key) {
            if (key.charAt(0) == '#') {
                key = key.substring(1);
            }
            // if the name is connected, we are done
            if (this.parent.connectedTextures.containsKey(key)) {
                return key;
            }
            return this.nameMappingCache.computeIfAbsent(key, this::getConnectedNameUncached);
        }

        /**
         * Gets the texture suffix
         *
         * @param texture     Texture name, must be a connected texture
         * @param connections Connections byte
         * @param transform   Rotations to apply to faces
         * @return Key used to cache it
         */
        private String getTextureSuffix(String texture, byte connections, Function<Direction, Direction> transform) {
            int key = 0;
            for (Direction dir : Type.HORIZONTAL) {
                int flag = 1 << transform.apply(dir).getId();
                if ((connections & flag) == flag) {
                    key |= 1 << dir.getHorizontal();
                }
            }
            // if empty, do not prefix
            String[] suffixes = this.parent.connectedTextures.get(texture);
            assert suffixes != null;
            String suffix = suffixes[key];
            if (suffix.isEmpty()) {
                return suffix;
            }
            return "_" + suffix;
        }

        /**
         * Gets the model based on the connections in the given model data
         *
         * @param connections Array of face connections, true at indexes of connected sides
         * @return Model with connections applied
         */
        private BakedModel applyConnections(byte connections) {
            // copy each element with updated faces
            List<ModelElement> elements = Lists.newArrayList();
            for (ModelElement part : this.parent.model.getElements()) {
                Map<Direction, ModelElementFace> partFaces = new EnumMap<>(Direction.class);
                for (Entry<Direction, ModelElementFace> entry : part.faces.entrySet()) {
                    // first, determine which texture to use on this side
                    Direction dir = entry.getKey();
                    ModelElementFace original = entry.getValue();
                    ModelElementFace face = original;

                    // follow the texture name back to the original name
                    // if it never reaches a connected texture, skip
                    String connectedTexture = this.getConnectedName(original.textureId);
                    if (!connectedTexture.isEmpty()) {
                        // if empty string, we can keep the old face
                        String suffix = this.getTextureSuffix(connectedTexture, connections, getTransform(dir, original.textureData));
                        if (!suffix.isEmpty()) {
                            // suffix the texture
                            String fullTexture = connectedTexture + suffix;
                            face = new ModelElementFace(original.cullFace, original.tintIndex, "#" + fullTexture, original.textureData);
                        }
                    }
                    // add the updated face
                    partFaces.put(dir, face);
                }
                // add the updated parts into a new model part
                elements.add(new ModelElement(part.from, part.to, partFaces, part.rotation, part.shade));
            }

            // bake the model
            return SimpleBlockModel.bakeDynamic(this.owner, elements, this.transforms);
        }

        /**
         * Gets an array of directions to whether a block exists on the side, indexed using direction indexes
         *
         * @param predicate Function that returns true if the block is connected on the given side
         * @return Boolean array of data
         */
        private static byte getConnections(Predicate<Direction> predicate) {
            byte connections = 0;
            for (Direction dir : Direction.values()) {
                if (predicate.test(dir)) {
                    connections |= 1 << dir.getId();
                }
            }
            return connections;
        }

        @NotNull
        @Override
        public ModelData getModelData(BlockRenderView world, BlockPos pos, BlockState state, ModelData tileData) {
            // if the data is already defined, return it, will happen in multipart models
            if (tileData.get(CONNECTIONS) != null) {
                return tileData;
            }

            // gather connections data
            AffineTransformation rotation = this.transforms.getRotation();
            return tileData.derive()
                    .with(CONNECTIONS, getConnections(dir -> this.parent.sides.contains(dir) && this.parent.connectionPredicate.test(state, world.getBlockState(pos.offset(rotation.rotateTransform(dir))))))
                    .build();
        }

        /**
         * Shared logic to get quads from a connections array
         *
         * @param connections Byte with 6 bits for the 6 different sides
         * @param state       Block state instance
         * @param side        Cullface
         * @param rand        Random instance
         * @param data        Model data instance
         * @return Model quads for the given side
         */
        protected synchronized List<BakedQuad> getCachedQuads(byte connections, @Nullable BlockState state, @Nullable Direction side, Random rand, ModelData data, @Nullable RenderLayer renderType) {
            // bake a new model if the orientation is not yet baked
            if (this.cache[connections] == null) {
                this.cache[connections] = this.applyConnections(connections);
            }

            // get the model for the given orientation
            return this.cache[connections].getQuads(state, side, rand, data, renderType);
        }


        @NotNull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, ModelData data, @Nullable RenderLayer renderType) {
            // try model data first
            Byte connections = data.get(CONNECTIONS);
            // if model data failed, try block state
            // temporary fallback until Forge has model data in multipart/weighted random
            if (connections == null) {
                // no state? return original
                if (state == null) {
                    return originalModel.getQuads(null, side, rand, data, renderType);
                }
                // this will return original if the state is missing all properties
                AffineTransformation rotation = this.transforms.getRotation();
                connections = getConnections((dir) -> {
                    if (!this.parent.sides.contains(dir)) {
                        return false;
                    }
                    BooleanProperty prop = IMultipartConnectedBlock.CONNECTED_DIRECTIONS.get(rotation.rotateTransform(dir));
                    return state.contains(prop) && state.get(prop);
                });
            }
            // get quads using connections
            return this.getCachedQuads(connections, state, side, rand, data, renderType);
        }
    }

    /**
     * Loader class containing singleton instance
     */
    public static ConnectedModel deserialize(JsonObject json, JsonDeserializationContext context) {
        ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);

        // root object for all model data
        JsonObject data = JsonHelper.getObject(json, "connection");

        // need at least one connected texture
        JsonObject connected = JsonHelper.getObject(data, "textures");
        if (connected.size() == 0) {
            throw new JsonSyntaxException("Must have at least one texture in connected");
        }

        // build texture list
        ImmutableMap.Builder<String, String[]> connectedTextures = new ImmutableMap.Builder<>();
        for (Entry<String, JsonElement> entry : connected.entrySet()) {
            // don't validate texture as it may be contained in a child model that is not yet loaded
            // get type, put in map
            String name = entry.getKey();
            connectedTextures.put(name, ConnectedModelRegistry.deserializeType(entry.getValue(), "textures[" + name + "]"));
        }

        // get a list of sides to pay attention to
        Set<Direction> sides;
        if (data.has("sides")) {
            JsonArray array = JsonHelper.getArray(data, "sides");
            sides = EnumSet.noneOf(Direction.class);
            for (int i = 0; i < array.size(); i++) {
                String side = JsonHelper.asString(array.get(i), "sides[" + i + "]");
                Direction dir = Direction.byName(side);
                if (dir == null) {
                    throw new JsonParseException("Invalid side " + side);
                }
                sides.add(dir);
            }
        } else {
            sides = EnumSet.allOf(Direction.class);
        }

        // other data
        BiPredicate<BlockState, BlockState> predicate = ConnectedModelRegistry.deserializePredicate(data, "predicate");

        // final model instance
        return new ConnectedModel(model, connectedTextures.build(), predicate, sides);
    }
}
