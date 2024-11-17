/* Code for ctl and shift down from TicTooltips by squeek502
 * https://github.com/squeek502/TiC-Tooltips/blob/1.7.10/java/squeek/tictooltips/helpers/KeyHelper.java
 */

package slimeknights.tconstruct.library.utils;

import io.github.fabricators_of_create.porting_lib.util.ForgeI18n;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Util {
    public static final Marker TCONSTRUCT = MarkerManager.getMarker("TCONSTRUCT");

    public static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###,###.##", DecimalFormatSymbols.getInstance(Locale.US));
    public static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#%");
    public static final DecimalFormat BONUS_FORMAT = new DecimalFormat("#.##");
    public static final DecimalFormat MULTIPLIER_FORMAT = new DecimalFormat("#.##x");
    public static final DecimalFormat PERCENT_BOOST_FORMAT = new DecimalFormat("#%");

    static {
        BONUS_FORMAT.setPositivePrefix("+");
        PERCENT_BOOST_FORMAT.setPositivePrefix("+");
    }

    /**
     * Gets a logger for the given name
     */
    public static Logger getLogger(String type) {
        return LogManager.getLogger(TConstruct.MOD_ID + "-" + type);
    }

    /**
     * Gets the currently active mod, assuming its not Tinkers
     *
     * @return Currently active mod ID
     */
    public static Optional<String> getCurrentlyActiveExternalMod() {
        return Optional.ofNullable(ModLoadingContext.get().getActiveContainer().getModId())
                .filter(activeModId -> !TConstruct.MOD_ID.equals(activeModId));
    }

    /**
     * Checks if the given key can be translated
     *
     * @param key Key to check
     * @return True if it can be translated
     */
    public static boolean canTranslate(String key) {
        return !ForgeI18n.getPattern(key).equals(key);
    }

    /**
     * Makes a translation key for the given name, redirect to the vanilla method
     *
     * @param base Base name, such as "block" or "gui"
     * @param name Object name
     * @return Translation key
     */
    public static String makeTranslationKey(String base, @Nullable Identifier name) {
        return net.minecraft.util.Util.createTranslationKey(base, name);
    }

    /**
     * Same as {@link net.minecraft.util.Util#make(Supplier)}
     */
    public static <T> T make(Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Same as {@link net.minecraft.util.Util#make(Object, Consumer)}
     */
    public static <T> T make(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    /**
     * Helper to create a indented list of entries in a single message.
     * Takes a list of objects, and converts them into a string with one entry on each line, prefixed by a tab for indentation.
     * The strings are created using the objects toString representation.
     *
     * @param list A list of objects to create a list of lines from
     * @return A single string with all entries seperated into a new line, and indented.
     */
    public static String toIndentedStringList(Collection<?> list) {
        return list.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n\t", "\n\t", ""));
    }

    /**
     * Gets the sign of a number
     *
     * @param value Number
     * @return Sign
     */
    public static int sign(int value) {
        if (value == 0) {
            return 0;
        }
        return value > 0 ? 1 : -1;
    }

    /**
     * Obtains a direction based on the difference between two positions
     *
     * @param pos      Tile position
     * @param neighbor Position of offset
     * @return Direction, or down if missing
     */
    public static Direction directionFromOffset(BlockPos pos, BlockPos neighbor) {
        BlockPos offset = neighbor.subtract(pos);
        for (Direction direction : Direction.values()) {
            if (direction.getVector().equals(offset)) {
                return direction;
            }
        }
        TConstruct.LOG.error("No direction for position {} and neighbor {}", pos, neighbor);
        return Direction.DOWN;
    }

    /**
     * Converts an ARGB color to a ABGR color or vice versa
     */
    public static int translateColorBGR(int color) {
        return (color & 0xFF00FF00) | (((color & 0x00FF0000) >> 16) & 0x000000FF) | (((color & 0x000000FF) << 16) & 0x00FF0000);
    }

    /**
     * Gets the slot type from a hand
     */
    public static EquipmentSlot getSlotType(Hand hand) {
        return hand == Hand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    /**
     * Gets the opposite hand of the given hand
     */
    public static Hand getOpposite(Hand hand) {
        return hand == Hand.OFF_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }

    /**
     * Converts a position and a side hit into a hit vector
     */
    public static Vec3d toHitVec(BlockPos pos, Direction sideHit) {
        return new Vec3d(
                pos.getX() + 0.5D + sideHit.getOffsetX() * 0.5D,
                pos.getY() + 0.5D + sideHit.getOffsetY() * 0.5D,
                pos.getZ() + 0.5D + sideHit.getOffsetZ() * 0.5D
        );
    }

    /**
     * Creates a block raytrace from the given position and side, targets the block center
     */
    public static BlockHitResult createTraceResult(BlockPos pos, Direction sideHit, boolean empty) {
        return new BlockHitResult(toHitVec(pos, empty ? sideHit.getOpposite() : sideHit), sideHit, pos, false);
    }

    /**
     * Creates a new client block entity data packet with better generics than the vanilla method
     */
    public static <B extends BlockEntity> BlockEntityUpdateS2CPacket createBEPacket(B be, Function<? super B, NbtCompound> tagFunction) {
        return BlockEntityUpdateS2CPacket.create(be, blockEntity -> tagFunction.apply(be));
    }
}
