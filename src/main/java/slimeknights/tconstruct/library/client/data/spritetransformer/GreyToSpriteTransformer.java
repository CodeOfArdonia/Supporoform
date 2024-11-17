package slimeknights.tconstruct.library.client.data.spritetransformer;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.data.ExistingFileHelper;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToColorMapping.Interpolate;
import slimeknights.tconstruct.library.client.data.util.AbstractSpriteReader;
import slimeknights.tconstruct.library.client.data.util.DataGenSpriteReader;
import slimeknights.tconstruct.library.client.data.util.ResourceManagerSpriteReader;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

import static net.minecraft.client.texture.NativeImage.getA;

/**
 * Supports including sprites as "part of the palette"
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class GreyToSpriteTransformer implements ISpriteTransformer {
    public static final Identifier NAME = TConstruct.getResource("grey_to_sprite");
    public static final Deserializer DESERIALIZER = new Deserializer();

    /**
     * Base folder for texture backgrounds
     */
    private static final String TEXTURE_FOLDER = "textures";
    /**
     * Sprite reader instance, filled in by events
     */
    @Nullable
    static AbstractSpriteReader READER = null;
    /**
     * List of all sprite mappings with cached data that need to be cleared
     */
    private static final List<SpriteMapping> MAPPINGS_TO_CLEAR = new ArrayList<>();

    /**
     * List of sprites to try
     */
    private final List<SpriteMapping> sprites;

    /**
     * Cache of the sprites to use for each color value
     */
    private final SpriteRange[] foundSpriteCache = new SpriteRange[256];

    /**
     * Constructor for search
     */
    private static final Interpolate<SpriteMapping, SpriteRange> SPRITE_RANGE = (first, second, grey) -> new SpriteRange(first, second);
    /**
     * Gets the grey value of a color
     */
    private static final ToIntFunction<SpriteMapping> GET_GREY = SpriteMapping::getGrey;

    /**
     * Gets the sprite for the given color
     */
    protected SpriteRange getSpriteRange(int grey) {
        if (this.foundSpriteCache[grey] == null) {
            this.foundSpriteCache[grey] = GreyToColorMapping.getNearestByGrey(this.sprites, GET_GREY, grey, SPRITE_RANGE);
        }
        return this.foundSpriteCache[grey];
    }

    /**
     * Gets the color at the given location from its full color value
     */
    private int getNewColor(int color, int x, int y) {
        // if fully transparent, just return fully transparent
        // we do not do 0 alpha RGB values to save effort
        if (getA(color) == 0) {
            return 0x00000000;
        }
        int grey = GreyToColorMapping.getGrey(color);
        int newColor = this.getSpriteRange(grey).getColor(x, y, grey);
        return GreyToColorMapping.scaleColor(color, newColor, grey);
    }

    @Override
    public void transform(NativeImage image, boolean allowAnimated) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setColor(x, y, this.getNewColor(image.getColor(x, y), x, y));
            }
        }
    }

    @Override
    public int getFallbackColor() {
        return this.getSpriteRange(216).getAverage(216);
    }

    /* Serializing */

    @Override
    public JsonObject serialize(JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("type", NAME.toString());
        JsonArray colors = new JsonArray();
        for (SpriteMapping mapping : this.sprites) {
            JsonObject pair = new JsonObject();
            pair.addProperty("grey", mapping.grey);
            // color used by both types
            if (mapping.color != -1 || mapping.path == null) {
                pair.addProperty("color", String.format("%08X", Util.translateColorBGR(mapping.color)));
            }
            // path by one
            if (mapping.path != null) {
                pair.addProperty("path", mapping.path.toString());
            }
            colors.add(pair);
        }
        object.add("palette", colors);
        return object;
    }

    /**
     * Serializer for a recolor sprite transformer
     */
    protected static class Deserializer implements JsonDeserializer<GreyToSpriteTransformer> {
        @Override
        public GreyToSpriteTransformer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            JsonArray palette = net.minecraft.util.JsonHelper.getArray(object, "palette");
            Builder paletteBuilder = GreyToSpriteTransformer.builder();
            for (int i = 0; i < palette.size(); i++) {
                JsonObject palettePair = net.minecraft.util.JsonHelper.asObject(palette.get(i), "palette[" + i + ']');
                int grey = net.minecraft.util.JsonHelper.getInt(palettePair, "grey");
                if (i == 0 && grey != 0) {
                    paletteBuilder.addABGR(0, 0xFF000000);
                }
                // get the proper type
                int color = ColorLoadable.ALPHA.getOrDefault(palettePair, "color", -1);
                if (palettePair.has("path")) {
                    paletteBuilder.addTexture(grey, JsonHelper.getResourceLocation(palettePair, "path"), color);
                } else {
                    paletteBuilder.addARGB(grey, color);
                }
            }
            return paletteBuilder.build();
        }
    }


    /* Builder */

    /**
     * Creates a new grey to color builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new grey to color builder starting with greyscale 0 as white
     */
    public static Builder builderFromBlack() {
        return builder().addABGR(0, 0xFF000000);
    }

    /**
     * Builder to create a palette of this type
     */
    public static class Builder {
        private final ImmutableList.Builder<SpriteMapping> builder = ImmutableList.builder();
        private int lastGrey = -1;

        /**
         * Validates the given grey value
         */
        private void checkGrey(int grey) {
            if (grey < 0 || grey > 255) {
                throw new IllegalArgumentException("Invalid grey value, must be between 0 and 255, inclusive");
            }
            if (grey <= this.lastGrey) {
                throw new IllegalArgumentException("Grey value must be greater than the previous value");
            }
            this.lastGrey = grey;
        }

        /**
         * Adds a color to the palette in ABGR format
         */
        public Builder addABGR(int grey, int color) {
            this.checkGrey(grey);
            this.builder.add(new SpriteMapping(grey, color, null));
            return this;
        }

        /**
         * Adds a color to the palette in ARGB format
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder addARGB(int grey, int color) {
            return this.addABGR(grey, Util.translateColorBGR(color));
        }

        /**
         * Adds a texture to the palette
         */
        public Builder addTexture(int grey, Identifier texture, int tint) {
            this.checkGrey(grey);
            this.builder.add(new SpriteMapping(grey, Util.translateColorBGR(tint), texture));
            return this;
        }

        /**
         * Adds a texture to the palette
         */
        public Builder addTexture(int grey, Identifier texture) {
            return this.addTexture(grey, texture, -1);
        }

        /**
         * Builds a color mapping
         */
        public GreyToSpriteTransformer build() {
            List<SpriteMapping> list = this.builder.build();
            if (list.size() < 2) {
                throw new IllegalStateException("Too few colors in palette, must have at least 2");
            }
            return new GreyToSpriteTransformer(list);
        }

        /**
         * Builds an animated transformer
         */
        public AnimatedGreyToSpriteTransformer animated(Identifier metaPath, int frames) {
            List<SpriteMapping> list = this.builder.build();
            if (list.size() < 2) {
                throw new IllegalStateException("Too few colors in palette, must have at least 2");
            }
            return new AnimatedGreyToSpriteTransformer(list, metaPath, frames);
        }
    }


    /* Data classes */

    /**
     * Mapping from greyscale to color
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class SpriteMapping {
        @Getter
        private final int grey;
        private final int color;
        /**
         * Path of the sprite relative to the textures folder
         */
        @Nullable
        private final Identifier path;

        /**
         * Loaded image
         */
        private transient NativeImage image = null;

        /**
         * Gets the image for this mapping
         */
        @Nullable
        private NativeImage getImage() {
            if (this.path != null && this.image == null) {
                if (READER == null) {
                    throw new IllegalStateException("Cannot get image for a sprite without reader");
                }
                try {
                    this.image = READER.read(this.path);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load required image", ex);
                }
                MAPPINGS_TO_CLEAR.add(this);
            }
            return this.image;
        }

        /**
         * Gets the color for the given X, Y, and frame
         */
        public int getColor(int x, int y, int frame) {
            if (this.path != null) {
                NativeImage image = this.getImage();
                if (image != null) {
                    int spriteColor;
                    // -1 means we are not doing frames, treat the whole image as one thing. This notably does not require it to be square
                    if (frame == -1) {
                        spriteColor = image.getColor(x % image.getWidth(), y % image.getHeight());
                    } else {
                        // assume the frames of this are square, otherwise we have to store the ratio somewhere
                        int width = image.getWidth();
                        // ensure the x and y coordinates are within the individual frame by wrapping, needed notably for large tool sprites
                        // then offset the y value, and ensure the offset is within the final height
                        spriteColor = image.getColor(x % width, (y % width + frame * width) % image.getHeight());
                    }
                    // if we have a color set, treat it as a tint
                    if (this.color != -1) {
                        spriteColor = GreyToColorMapping.scaleColor(spriteColor, this.color, 255);
                    }
                    return spriteColor;
                }
            }
            return this.color;
        }

        /**
         * Gets the average color of this sprite in ARGB format, or the base color if no path
         */
        public int getAverage() {
            if (this.path != null) {
                NativeImage image = this.getImage();
                if (image != null) {
                    int red = 0;
                    int green = 0;
                    int blue = 0;
                    int alpha = 0;
                    for (int x = 0; x < image.getWidth(); x++) {
                        for (int y = 0; y < image.getHeight(); y++) {
                            int color = image.getColor(x, y);
                            red += NativeImage.getR(color);
                            green += NativeImage.getG(color);
                            blue += NativeImage.getB(color);
                            alpha += NativeImage.getA(color);
                        }
                    }
                    int pixels = image.getWidth() * image.getHeight();
                    int spriteColor = NativeImage.combine(alpha / pixels, blue / pixels, green / pixels, red / pixels);
                    // if we have a color set, treat it as a tint
                    if (this.color != -1) {
                        spriteColor = GreyToColorMapping.scaleColor(spriteColor, this.color, 255);
                    }
                    return spriteColor;
                }
            }
            return this.color;
        }

        /**
         * Checks if these two mappings have the same values
         */
        public boolean isSame(SpriteMapping other) {
            return this == other || (this.color == other.color && Objects.equals(this.path, other.path));
        }
    }

    /**
     * Result from a sprite search for a given color
     */
    protected record SpriteRange(@Nullable SpriteMapping before, @Nullable SpriteMapping after) {
        /**
         * Gets the color of this range
         */
        public int getColor(int x, int y, int grey) {
            return this.getColor(x, y, -1, grey);
        }

        /**
         * Gets the color of this range for the given frame
         */
        public int getColor(int x, int y, int frame, int grey) {
            // after only
            if (this.before == null) {
                assert this.after != null;
                return this.after.getColor(x, y, frame);
            }
            if (this.after == null || this.before.isSame(this.after)) {
                return this.before.getColor(x, y, frame);
            }
            return GreyToColorMapping.interpolateColors(
                    this.before.getColor(x, y, frame), this.before.getGrey(),
                    this.after.getColor(x, y, frame), this.after.getGrey(),
                    grey);
        }

        /**
         * Gets the average value for the given grey value
         */
        public int getAverage(int grey) {
            if (this.before == null) {
                assert this.after != null;
                return this.after.getAverage();
            }
            if (this.after == null || this.before.isSame(this.after)) {
                return this.before.getAverage();
            }
            return GreyToColorMapping.interpolateColors(
                    this.before.getAverage(), this.before.getGrey(),
                    this.after.getAverage(), this.after.getGrey(),
                    grey);
        }
    }


    /* Event listeners */

    /**
     * If true, the event listeners are registered
     */
    private static boolean init = false;

    /**
     * Registers this transformer where relevant
     */
    public static void init() {
        if (!init) {
            init = true;
            ISpriteTransformer.SERIALIZER.registerDeserializer(NAME, DESERIALIZER);
            MaterialPartTextureGenerator.registerCallback(GreyToSpriteTransformer::textureCallback);
        }
    }

    /**
     * Called before generating to set up the reader
     */
    private static void textureCallback(@Nullable ExistingFileHelper existingFileHelper, @Nullable ResourceManager manager) {
        if (READER != null) {
            MAPPINGS_TO_CLEAR.forEach(mapping -> mapping.image = null);
            MAPPINGS_TO_CLEAR.clear();
            READER.closeAll();
            READER = null;
        }
        if (existingFileHelper != null) {
            READER = new DataGenSpriteReader(existingFileHelper, TEXTURE_FOLDER);
        } else if (manager != null) {
            READER = new ResourceManagerSpriteReader(manager, TEXTURE_FOLDER);
        }
    }
}