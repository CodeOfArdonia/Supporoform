package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * Provides read only access to the input of a casting table. Prevents extra data from leaking
 */
@RequiredArgsConstructor
public class CastingContainerWrapper implements ICastingContainer {
    private final CastingBlockEntity tile;
    @Setter
    private FluidStack fluid;
    private boolean switchSlots = false;

    @Override
    public ItemStack getStack() {
        ItemStack stack = this.tile.getStack(this.switchSlots ? CastingBlockEntity.OUTPUT : CastingBlockEntity.INPUT);
        if (stack.isIn(this.tile.getEmptyCastTag())) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public Fluid getFluid() {
        return this.fluid.getFluid();
    }

    @Nullable
    @Override
    public NbtCompound getFluidTag() {
        return this.fluid.getTag();
    }

    /**
     * Uses the input for input (default)
     */
    public void useInput() {
        this.switchSlots = false;
    }

    /**
     * Uses the output for input (for multistep casting)
     */
    public void useOutput() {
        this.switchSlots = true;
    }
}
