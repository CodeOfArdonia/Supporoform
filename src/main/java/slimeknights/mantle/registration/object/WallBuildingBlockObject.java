package slimeknights.mantle.registration.object;

import net.minecraft.block.Block;
import net.minecraft.block.WallBlock;
import net.minecraft.registry.Registries;
import slimeknights.mantle.registration.RegistrationHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Object containing a block with slab, stairs, and wall variants
 */
@SuppressWarnings("unused")
public class WallBuildingBlockObject extends BuildingBlockObject {
    private final Supplier<? extends WallBlock> wall;

    /**
     * Creates a new object from a building block object plus a wall.
     *
     * @param object Previous building block object
     * @param wall   Wall object
     */
    public WallBuildingBlockObject(BuildingBlockObject object, Supplier<? extends WallBlock> wall) {
        super(object);
        this.wall = wall;
    }

    /**
     * Creates a new wall building block object from the given blocks
     *
     * @param object Building block object
     * @param wall   Wall entry
     */
    public WallBuildingBlockObject(BuildingBlockObject object, Block wall) {
        this(object, RegistrationHelper.getCastedHolder(Registries.BLOCK, wall));
    }

    /**
     * Gets the wall for this block
     */
    public WallBlock getWall() {
        return Objects.requireNonNull(this.wall.get(), "Wall Building Block Object missing wall");
    }

    @Override
    public List<Block> values() {
        return Arrays.asList(this.get(), this.getSlab(), this.getStairs(), this.getWall());
    }
}
