package slimeknights.mantle.fluid.texture;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.mantle.util.JsonHelper;

import java.util.Objects;

/**
 * Record representing a fluid texture
 */
public record FluidTexture(Identifier still, Identifier flowing, @Nullable Identifier overlay,
                           @Nullable Identifier camera, int color) {

    /**
     * Serializes this to JSON
     */
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("still", this.still.toString());
        json.addProperty("flowing", this.flowing.toString());
        if (this.overlay != null) {
            json.addProperty("overlay", this.overlay.toString());
        }
        // during datagen, we just write the texture directly, we will include the needed prefix/suffix on read
        if (this.camera != null) {
            json.addProperty("camera", this.camera.toString());
        }
        json.addProperty("color", String.format("%08X", this.color));
        return json;
    }

    /**
     * Deserializes this from JSON
     */
    public static FluidTexture deserialize(JsonObject json) {
        Identifier still = JsonHelper.getResourceLocation(json, "still");
        Identifier flowing = JsonHelper.getResourceLocation(json, "flowing");
        //noinspection ConstantConditions
        Identifier overlay = JsonHelper.getResourceLocation(json, "overlay", null);
        Identifier camera = null;
        if (json.has("camera")) {
            camera = LocationExtender.INSTANCE.wrap(JsonHelper.getResourceLocation(json, "camera"), "textures/", ".png");
        }
        int color = ColorLoadable.ALPHA.getOrDefault(json, "color", -1);
        return new FluidTexture(still, flowing, overlay, camera, color);
    }

    /**
     * Builder for this object
     */
    @SuppressWarnings("unused") // API
    @Setter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class Builder {
        private final FluidType fluid;
        private Identifier still;
        private Identifier flowing;
        @Nullable
        private Identifier overlay = null;
        @Nullable
        private Identifier camera = null;
        private int color = -1;

        /**
         * Adds textures using the fluid registry ID
         *
         * @param prefix  Prefix for where to place textures
         * @param suffix  Suffix for placing textures, included before "still" or "flowing". Typically will want "/" or "_".
         * @param overlay If true, include an overlay texture
         * @param camera  If true, include a camera texture
         * @return Builder instance
         */
        public Builder wrapId(String prefix, String suffix, boolean overlay, boolean camera) {
            return this.textures(LocationExtender.INSTANCE.wrap(Objects.requireNonNull(PortingLibFluids.FLUID_TYPES.getId(this.fluid)), prefix, suffix), overlay, camera);
        }

        /**
         * Sets all textures by suffixing the given path
         *
         * @param path    Base path, make sure to include the trailing "_" or "/"
         * @param overlay If true, include an overlay texture
         * @param camera  If true, include a camera texture
         * @return Builder instance
         */
        public Builder textures(Identifier path, boolean overlay, boolean camera) {
            this.still(LocationExtender.INSTANCE.suffix(path, "still"));
            this.flowing(LocationExtender.INSTANCE.suffix(path, "flowing"));
            if (overlay) {
                this.overlay(LocationExtender.INSTANCE.suffix(path, "overlay"));
            }
            if (camera) {
                this.camera(LocationExtender.INSTANCE.suffix(path, "camera"));
            }
            return this;
        }

        /**
         * Builds the fluid texture instance
         */
        public FluidTexture build() {
            if (this.still == null || this.flowing == null) {
                throw new IllegalStateException("Must set both stll and flowing");
            }
            return new FluidTexture(this.still, this.flowing, this.overlay, this.camera, this.color);
        }
    }
}
