package slimeknights.tconstruct.library.materials.definition;

import net.minecraft.util.Identifier;

/**
 * Internal record to represent a material ID with a variant. Use {@link MaterialVariantId} to create if needed
 */
record MaterialVariantIdImpl(MaterialId material, String variant) implements MaterialVariantId {

    @Override
    public MaterialId getId() {
        return this.material;
    }

    @Override
    public String getVariant() {
        return this.variant;
    }

    @Override
    public boolean hasVariant() {
        return true;
    }

    @Override
    public boolean matchesVariant(MaterialVariantId other) {
        // special case: material#default will match against the empty variant, to allow recipes to look for a material with no variant
        if (DEFAULT_VARIANT.equals(this.variant)) {
            String otherVariant = other.getVariant();
            return this.material.equals(other.getId()) && (otherVariant.isEmpty() || DEFAULT_VARIANT.equals(otherVariant));
        }
        return this.sameVariant(other);
    }

    @Override
    public Identifier getLocation(char separator) {
        return new Identifier(this.material.getNamespace(), this.material.getPath() + separator + this.variant);
    }

    @Override
    public String getSuffix() {
        return this.material.getNamespace() + '_' + this.material.getPath() + '_' + this.variant;
    }

    @Override
    public String toString() {
        return this.material + "#" + this.variant;
    }
}
