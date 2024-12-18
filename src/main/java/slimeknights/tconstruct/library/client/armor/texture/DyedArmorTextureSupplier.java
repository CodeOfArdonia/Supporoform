package slimeknights.tconstruct.library.client.armor.texture;

import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Armor texture supplier that supplies a fixed texture that is colored using the given persistent data key
 */
public class DyedArmorTextureSupplier implements ArmorTextureSupplier {
    public static final RecordLoadable<DyedArmorTextureSupplier> LOADER = RecordLoadable.create(
            Loadables.RESOURCE_LOCATION.requiredField("prefix", s -> s.prefix),
            ModifierId.PARSER.requiredField("modifier", s -> s.modifier),
            ColorLoadable.NO_ALPHA.nullableField("default_color", s -> s.alwaysRender ? s.defaultColor : null),
            DyedArmorTextureSupplier::new);

    private final Identifier prefix;
    private final ModifierId modifier;
    private final boolean alwaysRender;
    private final int defaultColor;
    private final String[] textures;

    public DyedArmorTextureSupplier(Identifier prefix, ModifierId modifier, @Nullable Integer defaultColor) {
        this.prefix = prefix;
        this.modifier = modifier;
        this.alwaysRender = defaultColor != null;
        this.defaultColor = Objects.requireNonNullElse(defaultColor, -1);
        this.textures = new String[]{
                getTexture(prefix, "armor"),
                getTexture(prefix, "leggings"),
                getTexture(prefix, "wings"),
        };
    }

    public DyedArmorTextureSupplier(Identifier base, String variant, ModifierId modifier, @Nullable Integer defaultColor) {
        this(LocationExtender.INSTANCE.suffix(base, variant), modifier, defaultColor);
    }

    /**
     * Gets a texture if it exists, empty otherwise
     */
    public static String getTexture(Identifier base, String variant) {
        Identifier name = LocationExtender.INSTANCE.suffix(base, variant);
        if (TEXTURE_VALIDATOR.test(name)) {
            return ArmorTextureSupplier.getTexturePath(name);
        }
        return "";
    }

    @Override
    public ArmorTexture getArmorTexture(ItemStack stack, TextureType textureType) {
        String texture = this.textures[textureType.ordinal()];
        if (!texture.isEmpty() && (this.alwaysRender || ModifierUtil.getModifierLevel(stack, this.modifier) > 0)) {
            int color = ModifierUtil.getPersistentInt(stack, this.modifier, this.defaultColor);
            return new ArmorTexture(this.textures[textureType.ordinal()], 0xFF000000 | color);
        }
        return ArmorTexture.EMPTY;
    }

    @Override
    public RecordLoadable<DyedArmorTextureSupplier> getLoader() {
        return LOADER;
    }
}
