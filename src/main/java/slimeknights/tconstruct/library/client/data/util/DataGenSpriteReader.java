package slimeknights.tconstruct.library.client.data.util;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Logic to read sprites from existing images and return native images which can later be modified
 */
@Log4j2
@RequiredArgsConstructor
public class DataGenSpriteReader extends AbstractSpriteReader {
    private final ExistingFileHelper existingFileHelper;
    private final String folder;

    @Override
    public boolean exists(Identifier path) {
        return this.existingFileHelper.exists(path, ResourceType.CLIENT_RESOURCES, ".png", this.folder);
    }

    @Override
    public boolean metadataExists(Identifier path) {
        return this.existingFileHelper.exists(path, ResourceType.CLIENT_RESOURCES, ".png.mcmeta", this.folder);
    }

    @Override
    public NativeImage read(Identifier path) throws IOException {
        try {
            Resource resource = this.existingFileHelper.getResource(path, ResourceType.CLIENT_RESOURCES, ".png", this.folder);
            NativeImage image = NativeImage.read(resource.getInputStream());
            this.openedImages.add(image);
            return image;
        } catch (IOException | NoSuchElementException e) {
            log.error("Failed to read image at {}", path);
            throw e;
        }
    }

    @Override
    public JsonObject readMetadata(Identifier path) throws IOException {
        try (BufferedReader reader = this.existingFileHelper.getResource(path, ResourceType.CLIENT_RESOURCES, ".png.mcmeta", this.folder).openAsReader()) {
            return JsonHelper.deserialize(reader);
        }
    }
}
