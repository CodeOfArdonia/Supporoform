package slimeknights.tconstruct.library.materials.definition;

import lombok.Getter;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

public class Material implements IMaterial {
    /**
     * Default white color
     */
    protected static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    /**
     * This resource location uniquely identifies a material.
     */
    @Getter
    private final MaterialId identifier;
    /**
     * Materials tier, mostly for sorting right now
     */
    @Getter
    private final int tier;
    /**
     * Materials order within the tier, for sorting
     */
    @Getter
    private final int sortOrder;

    /**
     * Material can be crafted into parts in the PartBuilder
     */
    @Getter
    private final boolean craftable;
    /**
     * if true, this material is hidden
     */
    @Getter
    private final boolean hidden;

    /**
     * Materials should only be created by the MaterialManager, except when used for data gen
     * They're synced over the network and other classes might lead to unexpected behaviour.
     */
    public Material(Identifier identifier, int tier, int order, boolean craftable, boolean hidden) {
        this.identifier = new MaterialId(identifier);
        this.tier = tier;
        this.sortOrder = order;
        this.craftable = craftable;
        this.hidden = hidden;
    }

    protected Material(Identifier identifier, boolean craftable, boolean hidden) {
        this(identifier, 0, -1, craftable, hidden);
    }

    @Override
    public String toString() {
        return "Material{" + this.identifier + '}';
    }
}
