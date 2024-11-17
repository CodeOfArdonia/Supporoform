package slimeknights.tconstruct.library.client.materials;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Determines the type of texture used for rendering a specific material
 */
@Getter
@RequiredArgsConstructor
public class MaterialRenderInfo {
    /**
     * ID of this render info
     */
    private final MaterialVariantId identifier;
    @Nullable
    private final Identifier texture;
    private final String[] fallbacks;
    /**
     * color used to tint quads of this texture when the fallback is used
     */
    private final int vertexColor;
    /**
     * Extra light to add to the material, allows some materials to appear to glow slightly
     */
    private final int luminosity; // TODO: rename to emissivity

    /**
     * Tries to get a sprite for the given texture
     *
     * @param base         Base texture
     * @param suffix       Sprite suffix
     * @param spriteGetter Logic to get the sprite
     * @return Sprite if valid, null if missing
     */
    @Nullable
    private Sprite trySprite(SpriteIdentifier base, String suffix, Function<SpriteIdentifier, Sprite> spriteGetter) {
        Sprite sprite = spriteGetter.apply(getMaterial(base.getTextureId(), suffix));
        if (!MissingSprite.getMissingSpriteId().equals(sprite.getAtlasId())) {
            return sprite;
        }
        return null;
    }

    /**
     * Gets the texture for this render material
     *
     * @param base         Base texture
     * @param spriteGetter Logic to get a sprite
     * @return Pair of the sprite, and a boolean indicating whether the sprite should be tinted
     */
    public TintedSprite getSprite(SpriteIdentifier base, Function<SpriteIdentifier, Sprite> spriteGetter) {
        Sprite sprite;
        if (this.texture != null) {
            sprite = this.trySprite(base, getSuffix(this.texture), spriteGetter);
            if (sprite != null) {
                return new TintedSprite(sprite, -1, this.getLuminosity());
            }
        }
        for (String fallback : this.fallbacks) {
            sprite = this.trySprite(base, fallback, spriteGetter);
            if (sprite != null) {
                return new TintedSprite(sprite, this.vertexColor, this.getLuminosity());
            }
        }
        return new TintedSprite(spriteGetter.apply(base), this.vertexColor, this.getLuminosity());
    }

    /**
     * Gets all dependencies for this render info
     *
     * @param textures Texture consumer
     * @param base     Base texture, will be used to generate texture names
     */
    public void getTextureDependencies(Predicate<SpriteIdentifier> textures, SpriteIdentifier base) {
        if (this.texture != null) {
            if (textures.test(getMaterial(base.getTextureId(), getSuffix(this.texture)))) {
                return;
            }
        }
        for (String fallback : this.fallbacks) {
            if (textures.test(getMaterial(base.getTextureId(), fallback))) {
                break;
            }
        }
    }

    /**
     * Converts a material ID into a sprite suffix
     *
     * @param material Material ID
     * @return Sprite name
     */
    public static String getSuffix(Identifier material) {
        // namespace will only be minecraft for a texture override, so this lets you select to always use an untinted base texture as the materials texture
        if ("minecraft".equals(material.getNamespace())) {
            return material.getPath();
        }
        return material.getNamespace() + "_" + material.getPath();
    }

    /**
     * Gets a material for the given resource locations
     *
     * @param texture Texture path
     * @param suffix  Material or fallback suffix name
     * @return Material instance
     */
    private static SpriteIdentifier getMaterial(Identifier texture, String suffix) {
        return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(texture.getNamespace(), texture.getPath() + "_" + suffix));
    }

    /**
     * Data class for a sprite that may be tinted
     */
    public record TintedSprite(Sprite sprite, int color, int emissivity) {
    }
}
