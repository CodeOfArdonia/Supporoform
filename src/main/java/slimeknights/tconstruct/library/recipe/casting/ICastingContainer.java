package slimeknights.tconstruct.library.recipe.casting;

import slimeknights.mantle.recipe.container.ISingleStackContainer;

import org.jetbrains.annotations.Nullable;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;

/**
 * Inventory containing a single item and a fluid
 */
public interface ICastingContainer extends ISingleStackContainer {
    /**
     * Gets the contained fluid in this inventory
     *
     * @return Contained fluid
     */
    Fluid getFluid();

    /**
     * Gets the NBT for the contained fluid
     *
     * @return Fluid's NBT
     */
    @Nullable
    default NbtCompound getFluidTag() {
        return null;
    }
}
