package slimeknights.tconstruct.library.client.data.util;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base sprite reader implementation for both datagen and the command
 */
@RequiredArgsConstructor
public abstract class AbstractSpriteReader {
    protected final List<NativeImage> openedImages = new ArrayList<>();

    /**
     * Checks if an image exists in the given location
     */
    public abstract boolean exists(Identifier path);

    /**
     * Checks if metadata exists at the given location
     */
    public abstract boolean metadataExists(Identifier path);

    /**
     * Reads an image at the given path, relative to the folder
     *
     * @param path Path containing the file
     * @return Loaded image
     * @throws IOException If the image failed to load
     */
    public abstract NativeImage read(Identifier path) throws IOException;

    /**
     * Reads the file if it exists
     */
    @Nullable
    public NativeImage readIfExists(Identifier path) {
        if (this.exists(path)) {
            try {
                return this.read(path);
            } catch (IOException e) {
                // no-op should never happen
            }
        }
        return null;
    }

    /**
     * Reads metadata from the given path
     */
    public abstract JsonObject readMetadata(Identifier path) throws IOException;

    /**
     * Creates a new image with the given dimensions
     */
    public NativeImage create(int width, int height) {
        NativeImage image = new NativeImage(width, height, true);
        this.openedImages.add(image);
        return image;
    }

    /**
     * Tracks the given image so when this reader is closed, that image is closed
     */
    public void track(NativeImage transformed) {
        this.openedImages.add(transformed);
    }

    /**
     * Closes all opened images
     */
    public void closeAll() {
        for (NativeImage image : this.openedImages) {
            image.close();
        }
        this.openedImages.clear();
    }
}
