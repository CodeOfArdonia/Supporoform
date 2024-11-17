package slimeknights.mantle.registration.object;

import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.registry.Registries;
import slimeknights.mantle.registration.RegistrationHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Object containing a block with slab, stairs, and fence variants
 */
@SuppressWarnings("unused")
public class FenceBuildingBlockObject extends BuildingBlockObject {
    private final Supplier<? extends FenceBlock> fence;

    /**
     * Creates a new object from a building block object plus a fence.
     *
     * @param object Previous building block object
     * @param fence  Fence object
     */
    public FenceBuildingBlockObject(BuildingBlockObject object, Supplier<? extends FenceBlock> fence) {
        super(object);
        this.fence = fence;
    }

    /**
     * Creates a new object from a building block object plus a fence entry
     *
     * @param object Previous building block object
     * @param fence  Fence entry
     */
    public FenceBuildingBlockObject(BuildingBlockObject object, Block fence) {
        this(object, RegistrationHelper.getCastedHolder(Registries.BLOCK, fence));
    }

    /**
     * Gets the fence for this block
     */
    public FenceBlock getFence() {
        return Objects.requireNonNull(this.fence.get(), "Fence Building Block Object missing fence");
    }

    @Override
    public List<Block> values() {
        return Arrays.asList(this.get(), this.getSlab(), this.getStairs(), this.getFence());
    }
}
