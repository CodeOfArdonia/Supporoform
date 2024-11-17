package slimeknights.tconstruct.library.materials.definition;

import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.utils.IdParser;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * This is just a copy of ResourceLocation for type safety.
 */
public final class MaterialId extends Identifier implements MaterialVariantId {
    public static final IdParser<MaterialId> PARSER = new IdParser<>(MaterialId::new, "Material");

    public MaterialId(String resourceName) {
        super(resourceName);
    }

    public MaterialId(String namespaceIn, String pathIn) {
        super(namespaceIn, pathIn);
    }

    public MaterialId(Identifier resourceLocation) {
        super(resourceLocation.getNamespace(), resourceLocation.getPath());
    }

    /**
     * Checks if this ID matches the given material
     */
    public boolean matches(IMaterial material) {
        return this.equals(material.getIdentifier());
    }

    /**
     * Checks if this ID matches the given stack
     */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && this.equals(IMaterialItem.getMaterialFromStack(stack));
    }

    @Override
    public MaterialId getId() {
        return this;
    }

    @Override
    public String getVariant() {
        return "";
    }

    @Override
    public boolean hasVariant() {
        return false;
    }

    @Override
    public Identifier getLocation(char separator) {
        return this;
    }

    @Override
    public String getSuffix() {
        return getNamespace() + '_' + getPath();
    }

    @Override
    public boolean matchesVariant(MaterialVariantId other) {
        return this.equals(other.getId());
    }

    /* Helpers */

    /**
     * Creates a new material ID from the given string
     *
     * @param string String
     * @return Material ID, or null if invalid
     */
    @Nullable
    public static MaterialId tryParse(String string) {
        return PARSER.tryParse(string);
    }
}
