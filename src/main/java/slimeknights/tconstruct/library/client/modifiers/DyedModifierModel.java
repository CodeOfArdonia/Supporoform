package slimeknights.tconstruct.library.client.modifiers;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Modifier model that copies dye from a key and is breakable
 */
@RequiredArgsConstructor
public class DyedModifierModel implements IBakedModifierModel {
    /**
     * Constant unbaked model instance, as they are all the same
     */
    public static final IUnbakedModifierModel UNBAKED_INSTANCE = (smallGetter, largeGetter) -> {
        SpriteIdentifier smallTexture = smallGetter.apply("");
        SpriteIdentifier largeTexture = largeGetter.apply("");
        if (smallTexture != null || largeTexture != null) {
            return new DyedModifierModel(smallTexture, largeTexture);
        }
        return null;
    };

    /**
     * Textures to show
     */
    @Nullable
    private final SpriteIdentifier small;
    @Nullable
    private final SpriteIdentifier large;

    @Nullable
    @Override
    public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
        ModifierId modifier = entry.getId();
        IModDataView data = tool.getPersistentData();
        int color = -1;
        if (data.contains(modifier, NbtElement.INT_TYPE)) {
            color = data.getInt(modifier);
        }
        return new CacheKey(modifier, color);
    }

    @Override
    public void addQuads(IToolStackView tool, ModifierEntry modifier, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
        SpriteIdentifier texture = isLarge ? this.large : this.small;
        if (texture != null) {
            IModDataView data = tool.getPersistentData();
            Identifier key = modifier.getId();
            if (data.contains(key, NbtElement.INT_TYPE)) {
                quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(0xFF000000 | data.getInt(key), -1, spriteGetter.apply(texture), transforms, 0, pixels));
            }
        }
    }

    /**
     * Data class to cache a colored texture
     */
    private record CacheKey(ModifierId modifier, int color) {
    }
}
