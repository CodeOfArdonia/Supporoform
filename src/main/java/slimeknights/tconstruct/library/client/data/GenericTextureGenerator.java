package slimeknights.tconstruct.library.client.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.data.ExistingFileHelper;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Data generator to create png image files
 */
@Log4j2
public abstract class GenericTextureGenerator implements DataProvider {
    private final DataGenerator generator;
    @Nullable
    private final ExistingFileHelper existingFileHelper;
    private final String folder;
    @Nullable
    private final ExistingFileHelper.ResourceType resourceType;

    /**
     * Constructor which marks files as existing
     */
    public GenericTextureGenerator(DataGenerator generator, @Nullable ExistingFileHelper existingFileHelper, String folder) {
        this.generator = generator;
        this.folder = folder;
        this.existingFileHelper = existingFileHelper;
        if (existingFileHelper != null) {
            this.resourceType = new ExistingFileHelper.ResourceType(ResourceType.CLIENT_RESOURCES, ".png", folder);
        } else {
            this.resourceType = null;
        }
    }

    /**
     * Constructor which does not mark files as existing
     */
    public GenericTextureGenerator(DataGenerator generator, String folder) {
        this(generator, null, folder);
    }

    /**
     * Saves the given image to the given location
     */
    @SuppressWarnings("UnstableApiUsage")
    protected void saveImage(DataWriter cache, Identifier location, NativeImage image) {
        try {
            Path path = this.generator.getOutputFolder().resolve(Paths.get(ResourceType.CLIENT_RESOURCES.getDirectory(), location.getNamespace(), this.folder, location.getPath() + ".png"));
            if (this.existingFileHelper != null && this.resourceType != null) {
                this.existingFileHelper.trackGenerated(location, this.resourceType);
            }
            byte[] bytes = image.getBytes();
            cache.write(path, bytes, Hashing.sha1().hashBytes(bytes));
        } catch (IOException e) {
            log.error("Couldn't write image for {}", location, e);
        }
    }

    /**
     * Saves metadata for the given image
     */
    protected void saveMetadata(DataWriter cache, Identifier location, JsonObject metadata) {
        try {
            Path path = this.generator.getOutputFolder().resolve(Paths.get(ResourceType.CLIENT_RESOURCES.getDirectory(), location.getNamespace(), this.folder, location.getPath() + ".png.mcmeta"));
            DataProvider.writeToPath(cache, metadata, path);
        } catch (IOException e) {
            log.error("Couldn't write image metadata for {}", location, e);
        }
    }
}
