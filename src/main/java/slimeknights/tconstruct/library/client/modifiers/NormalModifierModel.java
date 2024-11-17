package slimeknights.tconstruct.library.client.modifiers;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AffineTransformation;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Default modifier model loader, loads a single texture from the standard path
 */
@RequiredArgsConstructor
public class NormalModifierModel implements IBakedModifierModel {
    /**
     * Constant unbaked model instance, as they are all the same
     */
    public static final IUnbakedModifierModel UNBAKED_INSTANCE = new Unbaked(-1, 0);

    /**
     * Textures to show
     */
    @Nullable
    private final SpriteIdentifier small;
    @Nullable
    private final SpriteIdentifier large;
    /**
     * Color to apply to the texture
     */
    private final int color;
    /**
     * Luminosity to apply to the texture
     */
    private final int luminosity;

    public NormalModifierModel(@Nullable SpriteIdentifier smallTexture, @Nullable SpriteIdentifier largeTexture) {
        this(smallTexture, largeTexture, -1, 0);
    }

    @Override
    public void addQuads(IToolStackView tool, ModifierEntry entry, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
        SpriteIdentifier spriteName = isLarge ? this.large : this.small;
        if (spriteName != null) {
            quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(this.color, -1, spriteGetter.apply(spriteName), transforms, this.luminosity, pixels));
        }
    }

    private record Unbaked(int color, int luminosity) implements IUnbakedModifierModel {
        @Nullable
        @Override
        public IBakedModifierModel forTool(Function<String, SpriteIdentifier> smallGetter, Function<String, SpriteIdentifier> largeGetter) {
            SpriteIdentifier smallTexture = smallGetter.apply("");
            SpriteIdentifier largeTexture = largeGetter.apply("");
            if (smallTexture != null || largeTexture != null) {
                return new NormalModifierModel(smallTexture, largeTexture, this.color, this.luminosity);
            }
            return null;
        }

        @Override
        public IUnbakedModifierModel configure(JsonObject data) {
            // parse the two keys, if we ended up with something new create an instance
            int color = ColorLoadable.ALPHA.getOrDefault(data, "color", -1);
            int luminosity = JsonHelper.getInt(data, "luminosity");
            if (color != this.color || luminosity != this.luminosity) {
                return new Unbaked(color, luminosity);
            }
            return this;
        }
    }
}
