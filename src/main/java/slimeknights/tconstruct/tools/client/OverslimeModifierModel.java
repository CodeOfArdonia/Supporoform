package slimeknights.tconstruct.tools.client;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.math.AffineTransformation;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.modifiers.IUnbakedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.modifiers.slotless.OverslimeModifier;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Modifier model that turns invisible when out of overslime
 */
public class OverslimeModifierModel extends NormalModifierModel {
    /**
     * Constant unbaked model instance, as they are all the same
     */
    public static final IUnbakedModifierModel UNBAKED_INSTANCE = (smallGetter, largeGetter) -> {
        SpriteIdentifier smallTexture = smallGetter.apply("");
        SpriteIdentifier largeTexture = largeGetter.apply("");
        if (smallTexture != null || largeTexture != null) {
            return new OverslimeModifierModel(smallTexture, largeTexture);
        }
        return null;
    };

    public OverslimeModifierModel(@Nullable SpriteIdentifier smallTexture, @Nullable SpriteIdentifier largeTexture) {
        super(smallTexture, largeTexture);
    }

    @Nullable
    @Override
    public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
        if (entry.getModifier() instanceof OverslimeModifier overslime && overslime.getShield(tool) == 0) {
            return null;
        }
        return super.getCacheKey(tool, entry);
    }

    @Override
    public void addQuads(IToolStackView tool, ModifierEntry entry, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
        if (!(entry.getModifier() instanceof OverslimeModifier overslime) || overslime.getShield(tool) != 0) {
            super.addQuads(tool, entry, spriteGetter, transforms, isLarge, startTintIndex, quadConsumer, pixels);
        }
    }
}
