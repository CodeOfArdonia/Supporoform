package slimeknights.mantle.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers to aid in reading and writing of NBT
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TagHelper {
    /* BlockPos */

    /**
     * Converts a block position to NBT
     *
     * @param pos Position
     * @return NBT compound
     */
    public static NbtCompound writePos(BlockPos pos) {
        NbtCompound tag = new NbtCompound();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    /**
     * Reads a block position from a given tag compound
     *
     * @param tag Tag to read
     * @return BlockPos, or null if invalid
     */
    @Nullable
    public static BlockPos readPos(NbtCompound tag) {
        if (tag.contains("X", NbtElement.NUMBER_TYPE) && tag.contains("Y", NbtElement.NUMBER_TYPE) && tag.contains("Z", NbtElement.NUMBER_TYPE)) {
            return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        }
        return null;
    }
}
