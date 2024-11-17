package slimeknights.mantle.client.model.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;

import java.util.Map;

/**
 * Model configuration wrapper to add in an extra set of textures
 */
public class ExtraTextureContext extends GeometryContextWrapper {
    private final Map<String, SpriteIdentifier> textures;

    /**
     * Creates a new wrapper using the given textures
     *
     * @param base     Base configuration
     * @param textures Textures map, any textures in this map will take precedence over those in the base configuration
     */
    public ExtraTextureContext(JsonUnbakedModel base, Map<String, SpriteIdentifier> textures) {
        super(base);
        this.textures = textures;
    }

    /**
     * Creates a new wrapper for a single texture
     *
     * @param base    Base configuration
     * @param name    Texture name, if it matches texture is returned
     * @param texture Texture path
     */
    public ExtraTextureContext(IGeometryBakingContext base, String name, Identifier texture) {
        super(base);
        this.textures = ImmutableMap.of(name, new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, texture));
    }

    @Override
    public SpriteIdentifier getMaterial(String name) {
        SpriteIdentifier connected = this.textures.get(name);
        if (connected != null) {
            return connected;
        }
        return super.getMaterial(name);
    }

    @Override
    public boolean hasMaterial(String name) {
        return this.textures.containsKey(name) || super.hasMaterial(name);
    }
}
