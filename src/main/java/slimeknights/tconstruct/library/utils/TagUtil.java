package slimeknights.tconstruct.library.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers related to Tag
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
    /* Helper functions */

    /**
     * Reads a block position from Tag
     *
     * @param parent Parent tag
     * @param key    Position key
     * @param offset Amount to offset position by
     * @return Block position, or null if invalid or missing
     */
    @Nullable
    public static BlockPos readOptionalPos(NbtCompound parent, String key, BlockPos offset) {
        if (parent.contains(key, NbtElement.COMPOUND_TYPE)) {
            return NbtHelper.toBlockPos(parent.getCompound(key)).add(offset);
        }
        return null;
    }

    /**
     * Checks if the given tag is a numeric type
     *
     * @param tag Tag to check
     * @return True if the type matches
     */
    public static boolean isNumeric(NbtElement tag) {
        byte type = tag.getType();
        return type == NbtElement.BYTE_TYPE || type == NbtElement.SHORT_TYPE || type == NbtElement.INT_TYPE || type == NbtElement.LONG_TYPE || type == NbtElement.FLOAT_TYPE || type == NbtElement.DOUBLE_TYPE;
    }
}
