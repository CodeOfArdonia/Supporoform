package slimeknights.mantle.client.model.util;

import com.mojang.datafixers.util.Either;
import lombok.AllArgsConstructor;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraftforge.client.model.geometry.BlockGeometryBakingContext;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

@AllArgsConstructor
public class ModelTextureIteratable implements Iterable<Map<String, Either<SpriteIdentifier, String>>> {
    /**
     * Initial map for iteration
     */
    @Nullable
    private final Map<String, Either<SpriteIdentifier, String>> startMap;
    /**
     * Initial model for iteration
     */
    @Nullable
    private final JsonUnbakedModel startModel;

    /**
     * Creates an iterable over the given model
     *
     * @param model Model
     */
    public ModelTextureIteratable(JsonUnbakedModel model) {
        this(null, model);
    }

    /**
     * @param owner    Model configuration owner
     * @param fallback Fallback in case the owner does not contain a block model
     * @return Iteratable over block model texture maps
     */
    public static ModelTextureIteratable of(JsonUnbakedModel owner, SimpleBlockModel fallback) {
        if (owner instanceof BlockGeometryBakingContext blockOwner) {
            return new ModelTextureIteratable(null, blockOwner.owner);
        }
        return new ModelTextureIteratable(fallback.getTextures(), fallback.getParent());
    }

    @Override
    public MapIterator iterator() {
        return new MapIterator(this.startMap, this.startModel);
    }

    @AllArgsConstructor
    private static class MapIterator implements Iterator<Map<String, Either<SpriteIdentifier, String>>> {
        /**
         * Initial map for iteration
         */
        @Nullable
        private Map<String, Either<SpriteIdentifier, String>> initial;
        /**
         * current model in the iterator
         */
        @Nullable
        private JsonUnbakedModel model;

        @Override
        public boolean hasNext() {
            return this.initial != null || this.model != null;
        }

        @Override
        public Map<String, Either<SpriteIdentifier, String>> next() {
            Map<String, Either<SpriteIdentifier, String>> map;
            if (this.initial != null) {
                map = this.initial;
                this.initial = null;
            } else if (this.model != null) {
                map = this.model.textureMap;
                this.model = this.model.parent;
            } else {
                throw new NoSuchElementException();
            }
            return map;
        }
    }
}
