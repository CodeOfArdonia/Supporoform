package slimeknights.tconstruct.shared.block;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import net.minecraft.block.AbstractGlassBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.DyeColor;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.Locale;

public class ClearStainedGlassBlock extends AbstractGlassBlock {

    private final GlassColor glassColor;

    public ClearStainedGlassBlock(Settings properties, GlassColor glassColor) {
        super(properties);
        this.glassColor = glassColor;
    }

    @Nullable
    public float[] getBeaconColorMultiplier(BlockState state, WorldView world, BlockPos pos, BlockPos beaconPos) {
        return GlassColor.calcRGB(glassColor.color);
    }

    /**
     * Enum used for registration of this and the pane block
     */
    public enum GlassColor implements StringIdentifiable {
        WHITE(0xffffff, DyeColor.WHITE),
        ORANGE(0xd87f33, DyeColor.ORANGE),
        MAGENTA(0xb24cd8, DyeColor.MAGENTA),
        LIGHT_BLUE(0x6699d8, DyeColor.LIGHT_BLUE),
        YELLOW(0xe5e533, DyeColor.YELLOW),
        LIME(0x7fcc19, DyeColor.LIME),
        PINK(0xf27fa5, DyeColor.PINK),
        GRAY(0x4c4c4c, DyeColor.GRAY),
        LIGHT_GRAY(0x999999, DyeColor.LIGHT_GRAY),
        CYAN(0x4c7f99, DyeColor.CYAN),
        PURPLE(0x7f3fb2, DyeColor.PURPLE),
        BLUE(0x334cb2, DyeColor.BLUE),
        BROWN(0x664c33, DyeColor.BROWN),
        GREEN(0x667f33, DyeColor.GREEN),
        RED(0x993333, DyeColor.RED),
        BLACK(0x191919, DyeColor.BLACK);

        /**
         * -- GETTER --
         *  Variant color to reduce number of models
         *
         * @return Variant color for BlockColors and ItemColors
         */
        @Getter
        private final int color;
        /**
         * -- GETTER --
         *  Gets the vanilla dye color associated with this color
         */
        @Getter
        private final DyeColor dye;
        /**
         * -- GETTER --
         *  Gets the RGB value for this color as an array
         *
         * @return Color RGB for beacon
         */
        @Getter
        private final float[] rgb;
        private final String name;

        GlassColor(int color, DyeColor dye) {
            this.color = color;
            this.dye = dye;
            this.rgb = calcRGB(color);
            this.name = this.name().toLowerCase(Locale.US);
        }

        /**
         * Converts the color into an RGB float array
         *
         * @param color Color input
         * @return Float array
         */
        private static float[] calcRGB(int color) {
            float[] out = new float[3];
            out[0] = ((color >> 16) & 0xFF) / 255f;
            out[1] = ((color >> 8) & 0xFF) / 255f;
            out[2] = (color & 0xFF) / 255f;
            return out;
        }

        public DyeColor getDye() {
            return this.dye;
        }

        @Override
        public String asString() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
