package slimeknights.tconstruct.tables.block.entity.inventory;

import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.material.MaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderContainer;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Objects;

public class PartBuilderContainerWrapper implements IPartBuilderContainer {
    private final PartBuilderBlockEntity builder;
    /**
     * If true, the material recipe is out of date
     */
    private boolean materialNeedsUpdate = true;
    /**
     * Cached material recipe, may be null if not a material item
     */
    @Nullable
    private IMaterialValue material = null;

    public PartBuilderContainerWrapper(PartBuilderBlockEntity builder) {
        this.builder = builder;
    }

    @Override
    public ItemStack getStack() {
        return builder.getStack(PartBuilderBlockEntity.MATERIAL_SLOT);
    }

    @Override
    public ItemStack getPatternStack() {
        return builder.getStack(PartBuilderBlockEntity.PATTERN_SLOT);
    }

    /**
     * Gets the tiles world
     */
    protected World getWorld() {
        return Objects.requireNonNull(builder.getWorld(), "Tile entity world must be nonnull");
    }

    /**
     * Refreshes the stored material
     */
    public void refreshMaterial() {
        this.materialNeedsUpdate = true;
        this.material = null;
    }

    @Override
    @Nullable
    public IMaterialValue getMaterial() {
        if (this.materialNeedsUpdate) {
            this.materialNeedsUpdate = false;
            ItemStack stack = getStack();
            if (stack.isEmpty()) {
                this.material = null;
            } else if (stack.isIn(TinkerTags.Items.TOOL_PARTS)) {
                MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
                if (IMaterial.UNKNOWN_ID.matchesVariant(material)) {
                    this.material = null;
                } else {
                    this.material = new MaterialValue(material, MaterialCastingLookup.getItemCost(stack.getItem()));
                }
            } else {
                World world = getWorld();
                this.material = world.getRecipeManager().getFirstMatch(TinkerRecipeTypes.MATERIAL.get(), this, world).orElse(null);
            }
        }
        return this.material;
    }
}
