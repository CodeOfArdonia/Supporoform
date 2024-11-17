package slimeknights.tconstruct.smeltery.block.entity.multiblock;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.common.multiblock.IServantLogic;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Data class representing the size and contents of a multiblock
 */
public class MultiblockStructureData {
    public static final String TAG_EXTRA_POS = "extra";
    public static final String TAG_MIN = "min";
    public static final String TAG_MAX = "max";

    /**
     * Smallest block position in the structure
     */
    @Getter
    private final BlockPos minPos;
    /**
     * Largest block position in the structure
     */
    @Getter
    private final BlockPos maxPos;

    /**
     * Contains all positions not in the standard areas, typically inside
     */
    protected final Set<BlockPos> extra;

    /**
     * Booleans to determine bound check parameters
     */
    private final boolean hasCeiling, hasFrame, hasFloor;

    /**
     * Smallest position inside the structure walls
     */
    @Getter
    private final BlockPos minInside;
    /**
     * Largest position inside the structure walls
     */
    @Getter
    private final BlockPos maxInside;

    /**
     * Inside sizes
     */
    @Getter
    private final int innerX, innerY, innerZ;

    /**
     * Bounding box representing the area inside the structure
     */
    @Getter
    private final Box bounds;

    public MultiblockStructureData(BlockPos minPos, BlockPos maxPos, Set<BlockPos> extraPositons, boolean hasFloor, boolean hasFrame, boolean hasCeiling) {
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.extra = extraPositons;
        this.hasFloor = hasFloor;
        this.hasFrame = hasFrame;
        this.hasCeiling = hasCeiling;

        // inner positions
        this.minInside = minPos.add(1, hasFloor ? 1 : 0, 1);
        this.maxInside = maxPos.add(-1, hasCeiling ? -1 : 0, -1);
        this.innerX = this.maxInside.getX() - this.minInside.getX() + 1;
        this.innerY = this.maxInside.getY() - this.minInside.getY() + 1;
        this.innerZ = this.maxInside.getZ() - this.minInside.getZ() + 1;
        this.bounds = new Box(this.minInside, this.maxInside.add(1, 1, 1));
    }

    /**
     * Checks if a positon is within the cube made from two other positions
     *
     * @param pos Position to check
     * @param min Min position
     * @param max Max position
     * @return True if within the positions
     */
    public static boolean isWithin(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getY() >= min.getY() && pos.getZ() >= min.getZ()
                && pos.getX() <= max.getX() && pos.getY() <= max.getY() && pos.getZ() <= max.getZ();
    }

    /**
     * Checks if the block position is within the bounds of the structure
     *
     * @param pos Position to check
     * @return True if the position is within the bounds
     */
    public boolean withinBounds(BlockPos pos) {
        return isWithin(pos, this.minPos, this.maxPos);
    }

    /**
     * Checks if the position is within the inside of the structure
     *
     * @param pos Position to check
     * @return True if within the central bounds
     */
    public boolean isInside(BlockPos pos) {
        return isWithin(pos, this.minInside, this.maxInside);
    }

    /**
     * Checks if the given block position is part of this structure.
     *
     * @param pos Position to check
     * @return True if its part of this structure
     */
    public boolean contains(BlockPos pos) {
        return this.withinBounds(pos) && this.containsBase(pos);
    }

    /**
     * Checks if the given block position is part of this structure. Slightly simplier logic assuming the position is within bounds
     *
     * @param pos Position to check
     * @return True if its part of this structure
     */
    private boolean containsBase(BlockPos pos) {
        // blocks in the inner region are added to the extra positions, fall back to that
        if (!this.isInside(pos)) {
            // if there is a frame, shape is a full cube so the subtraction is all we need
            if (this.hasFrame) {
                return true;
            }

            // otherwise we have to count edges to make sure its not on a frame
            // frame is any blocks touching two edges
            int edges = 0;
            if (pos.getX() == this.minPos.getX() || pos.getX() == this.maxPos.getX()) edges++;
            if (pos.getZ() == this.minPos.getZ() || pos.getZ() == this.maxPos.getZ()) edges++;
            if ((this.hasFloor && pos.getY() == this.minPos.getY()) ||
                    (this.hasCeiling && pos.getX() == this.maxPos.getX())) edges++;
            if (edges < 2) {
                return true;
            }
        }

        // inner blocks and frame blocks (no frame) can both be added
        // though note checking code does not currently support finding extra frame blocks
        return this.extra.contains(pos);
    }

    /**
     * Checks if the block position is directly above the structure
     *
     * @param pos Position to check
     * @return True if the position is exactly one block above the structure
     */
    public boolean isDirectlyAbove(BlockPos pos) {
        return pos.getX() >= this.minPos.getX() && pos.getZ() >= this.minPos.getZ()
                && pos.getX() <= this.maxPos.getX() && pos.getZ() <= this.maxPos.getZ()
                && pos.getY() == this.maxPos.getY() + 1;
    }

    /**
     * Iterates over each position contained in this structure
     *
     * @param consumer Position consumer, note the position is mutable, so call {@link BlockPos#toImmutable()} if you have to store it
     */
    public void forEachContained(Consumer<BlockPos.Mutable> consumer) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = this.minPos.getX(); x <= this.maxPos.getX(); x++) {
            for (int y = this.minPos.getY(); y <= this.maxPos.getY(); y++) {
                for (int z = this.minPos.getZ(); z <= this.maxPos.getZ(); z++) {
                    mutable.set(x, y, z);
                    if (this.containsBase(mutable)) {
                        consumer.accept(mutable);
                    }
                }
            }
        }
    }

    /**
     * Updates the master at the given position
     */
    private static void updateMaster(World world, BlockPos pos, IMasterLogic master, boolean add) {
        // update the structure property first, this may add or remove the block entity
        BlockState state = world.getBlockState(pos);
        if (state.contains(SearedBlock.IN_STRUCTURE) && state.get(SearedBlock.IN_STRUCTURE) != add) {
            world.setBlockState(pos, state.with(SearedBlock.IN_STRUCTURE, add), Block.NOTIFY_LISTENERS);
        }
        // if the BE is there, set its property
        BlockEntityHelper.get(IServantLogic.class, world, pos).ifPresent(
                add ? te -> te.setPotentialMaster(master) : te -> te.removeMaster(master));
    }

    /**
     * Assigns the master to all servants in this structure
     *
     * @param master       Master to assign
     * @param oldStructure Previous structure instance. Reduces the number of masters assigned and removes old masters
     */
    public <T extends MantleBlockEntity & IMasterLogic> void assignMaster(T master, @Nullable MultiblockStructureData oldStructure) {
        Predicate<BlockPos> shouldUpdate;
        if (oldStructure == null) {
            shouldUpdate = pos -> true;
        } else {
            shouldUpdate = pos -> !oldStructure.contains(pos);
        }

        World world = master.getWorld();
        assert world != null;


        // assign master to each servant
        this.forEachContained(pos -> {
            if (shouldUpdate.test(pos) && world.isChunkLoaded(pos)) {
                updateMaster(world, pos, master, true);
            }
        });

        // remove master from anything only in the old structure
        if (oldStructure != null) {
            oldStructure.forEachContained(pos -> {
                if (!this.contains(pos) && world.isChunkLoaded(pos)) {
                    updateMaster(world, pos, master, false);
                }
            });
        }
    }

    /**
     * Clears the master on all blocks in this structure
     *
     * @param master Master to remove
     */
    public <T extends MantleBlockEntity & IMasterLogic> void clearMaster(T master) {
        World world = master.getWorld();
        assert world != null;
        this.forEachContained(pos -> {
            if (world.isChunkLoaded(pos)) {
                updateMaster(world, pos, master, false);
            }
        });
    }

    /**
     * Writes this structure to NBT for the client, client does not need a full list of positions, just render bounds
     *
     * @param controllerPos Position of the controller for relative saving, use {@link BlockPos#ORIGIN} for absolute.
     * @return structure as NBT
     */
    public NbtCompound writeClientTag(BlockPos controllerPos) {
        NbtCompound nbt = new NbtCompound();
        nbt.put(TAG_MIN, NbtHelper.fromBlockPos(this.minPos.subtract(controllerPos)));
        nbt.put(TAG_MAX, NbtHelper.fromBlockPos(this.maxPos.subtract(controllerPos)));
        return nbt;
    }

    /**
     * Writes the full NBT data for writing to disk
     *
     * @param controllerPos Position of the controller for relative saving, use {@link BlockPos#ORIGIN} for absolute.
     * @return structure as NBT
     */
    public NbtCompound writeToTag(BlockPos controllerPos) {
        NbtCompound nbt = this.writeClientTag(controllerPos);
        if (!this.extra.isEmpty()) {
            nbt.put(TAG_EXTRA_POS, writePosList(this.extra, controllerPos));
        }
        return nbt;
    }

    /**
     * Writes a lit of positions to NBT
     *
     * @param collection Position collection
     * @param basePos    Base position for relative saving, use {@link BlockPos#ORIGIN} for absolute positions.
     * @return NBT list
     */
    protected static NbtList writePosList(Collection<BlockPos> collection, BlockPos basePos) {
        NbtList list = new NbtList();
        for (BlockPos pos : collection) {
            list.add(NbtHelper.fromBlockPos(pos.subtract(basePos)));
        }
        return list;
    }
}
