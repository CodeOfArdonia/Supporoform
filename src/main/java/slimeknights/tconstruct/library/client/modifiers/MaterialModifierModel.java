package slimeknights.tconstruct.library.client.modifiers;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Model for a modifier that has variants based on a material
 */
@RequiredArgsConstructor
public class MaterialModifierModel implements IBakedModifierModel {
    /**
     * Fetches relevant material textures after checking if the texture exists
     */
    @Nullable
    private static SpriteIdentifier stitchMaterialTextures(Function<String, SpriteIdentifier> textureGetter) {
        SpriteIdentifier baseTexture = textureGetter.apply("");
        if (baseTexture != null) {
            for (MaterialRenderInfo info : MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos()) {
                Identifier texture = info.getTexture();
                if (texture != null) {
                    textureGetter.apply("_" + MaterialRenderInfo.getSuffix(texture));
                }
                for (String fallback : info.getFallbacks()) {
                    textureGetter.apply("_" + fallback);
                }
            }
        }
        return baseTexture;
    }

    /**
     * Constant unbaked model instance, as they are all the same
     */
    public static final IUnbakedModifierModel UNBAKED_INSTANCE = (smallGetter, largeGetter) -> {
        SpriteIdentifier smallTexture = stitchMaterialTextures(smallGetter);
        SpriteIdentifier largeTexture = stitchMaterialTextures(largeGetter);
        if (smallTexture != null || largeTexture != null) {
            return new MaterialModifierModel(smallTexture, largeTexture);
        }
        return null;
    };

    @Nullable
    private final SpriteIdentifier small;
    @Nullable
    private final SpriteIdentifier large;

    @Nullable
    @Override
    public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
        ModifierId modifier = entry.getId();
        return new CacheKey(modifier, tool.getPersistentData().getString(modifier));
    }

    @Nullable
    private static MaterialVariantId getMaterial(IToolStackView tool, Modifier modifier) {
        String material = tool.getPersistentData().getString(modifier.getId());
        if (!material.isEmpty()) {
            return MaterialVariantId.tryParse(material);
        }
        return null;
    }

    @Override
    public void addQuads(IToolStackView tool, ModifierEntry modifier, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
        SpriteIdentifier texture = isLarge ? this.large : this.small;
        if (texture != null) {
            MaterialVariantId material = getMaterial(tool, modifier.getModifier());
            if (material != null) {
                quadConsumer.accept(MaterialModel.getQuadsForMaterial(spriteGetter, texture, material, -1, transforms, pixels));
            }
        }
    }

    /**
     * Data class to cache a mateirla texture
     */
    private record CacheKey(ModifierId modifier, String material) {
    }
}
