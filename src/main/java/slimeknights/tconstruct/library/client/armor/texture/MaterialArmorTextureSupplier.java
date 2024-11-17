package slimeknights.tconstruct.library.client.armor.texture;

import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Optional;
import java.util.function.Function;

/**
 * Logic to create material texture variants for armor
 */
@RequiredArgsConstructor
public abstract class MaterialArmorTextureSupplier implements ArmorTextureSupplier {
    /**
     * Field for parsing the variant from JSON
     */
    private static final LoadableField<Identifier, MaterialArmorTextureSupplier> PREFIX_FIELD = Loadables.RESOURCE_LOCATION.requiredField("prefix", m -> m.prefix);

    /**
     * Makes a texture for the given variant and material, returns null if its missing
     */
    private static ArmorTexture tryTexture(Identifier name, int color, String material) {
        Identifier texture = LocationExtender.INSTANCE.suffix(name, material);
        if (TEXTURE_VALIDATOR.test(texture)) {
            return new ArmorTexture(ArmorTextureSupplier.getTexturePath(texture), color);
        }
        return ArmorTexture.EMPTY;
    }

    /**
     * Makes a material getter for the given base and type
     */
    public static Function<String, ArmorTexture> materialGetter(Identifier name) {
        // if the base texture does not exist, means we decided to skip this piece. Notably used for skipping some layers of wings
        if (!TEXTURE_VALIDATOR.test(name)) {
            return material -> ArmorTexture.EMPTY;
        }
        // TODO: consider memoizing these functions, as if the same name appears twice in different models we can reuse it
        return Util.memoize(materialStr -> {
            if (!materialStr.isEmpty()) {
                MaterialVariantId material = MaterialVariantId.tryParse(materialStr);
                int color = -1;
                if (material != null) {
                    Optional<MaterialRenderInfo> infoOptional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
                    if (infoOptional.isPresent()) {
                        MaterialRenderInfo info = infoOptional.get();
                        Identifier untinted = info.getTexture();
                        if (untinted != null) {
                            ArmorTexture texture = tryTexture(name, -1, '_' + untinted.getNamespace() + '_' + untinted.getPath());
                            if (texture != ArmorTexture.EMPTY) {
                                return texture;
                            }
                        }
                        color = info.getVertexColor();
                        for (String fallback : info.getFallbacks()) {
                            ArmorTexture texture = tryTexture(name, color, '_' + fallback);
                            if (texture != ArmorTexture.EMPTY) {
                                return texture;
                            }
                        }
                    }
                }
                // base texture guaranteed to exist, else we would not be in this function
                return new ArmorTexture(ArmorTextureSupplier.getTexturePath(name), color);
            }
            return ArmorTexture.EMPTY;
        });
    }

    private final Identifier prefix;
    private final Function<String, ArmorTexture>[] textures;

    @SuppressWarnings("unchecked")
    public MaterialArmorTextureSupplier(Identifier prefix) {
        this.prefix = prefix;
        this.textures = new Function[]{
                materialGetter(LocationExtender.INSTANCE.suffix(prefix, "armor")),
                materialGetter(LocationExtender.INSTANCE.suffix(prefix, "leggings")),
                materialGetter(LocationExtender.INSTANCE.suffix(prefix, "wings"))
        };
    }

    /**
     * Gets the material from a given stack
     */
    protected abstract String getMaterial(ItemStack stack);

    @Override
    public ArmorTexture getArmorTexture(ItemStack stack, TextureType textureType) {
        String material = this.getMaterial(stack);
        if (!material.isEmpty()) {
            return this.textures[textureType.ordinal()].apply(material);
        }
        return ArmorTexture.EMPTY;
    }

    /**
     * Material supplier using persistent data
     */
    public static class PersistentData extends MaterialArmorTextureSupplier {
        public static final RecordLoadable<PersistentData> LOADER = RecordLoadable.create(
                PREFIX_FIELD,
                Loadables.RESOURCE_LOCATION.requiredField("material_key", d -> d.key),
                PersistentData::new);

        private final Identifier key;

        public PersistentData(Identifier prefix, Identifier key) {
            super(prefix);
            this.key = key;
        }

        public PersistentData(Identifier base, String suffix, Identifier key) {
            this(LocationExtender.INSTANCE.suffix(base, suffix), key);
        }

        @Override
        protected String getMaterial(ItemStack stack) {
            return ModifierUtil.getPersistentString(stack, this.key);
        }

        @Override
        public RecordLoadable<PersistentData> getLoader() {
            return LOADER;
        }
    }

    /**
     * Material supplier using material data
     */
    public static class Material extends MaterialArmorTextureSupplier {
        public static final RecordLoadable<Material> LOADER = RecordLoadable.create(
                PREFIX_FIELD,
                IntLoadable.FROM_ZERO.requiredField("index", m -> m.index),
                Material::new);

        private final int index;

        public Material(Identifier prefix, int index) {
            super(prefix);
            this.index = index;
        }

        public Material(Identifier base, String variant, int index) {
            this(LocationExtender.INSTANCE.suffix(base, variant), index);
        }

        @Override
        protected String getMaterial(ItemStack stack) {
            NbtCompound tag = stack.getNbt();
            if (tag != null && tag.contains(ToolStack.TAG_MATERIALS, NbtElement.LIST_TYPE)) {
                return tag.getList(ToolStack.TAG_MATERIALS, NbtElement.STRING_TYPE).getString(this.index);
            }
            return "";
        }

        @Override
        public RecordLoadable<Material> getLoader() {
            return LOADER;
        }
    }
}
