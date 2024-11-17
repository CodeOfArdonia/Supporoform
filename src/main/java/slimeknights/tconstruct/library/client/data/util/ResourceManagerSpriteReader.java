package slimeknights.tconstruct.library.client.data.util;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * Sprite reader pulling from a datapack resource manager
 */
@RequiredArgsConstructor
public class ResourceManagerSpriteReader extends AbstractSpriteReader {
    private final ResourceManager manager;
    private final String folder;

    /**
     * Gets a location with the given extension
     */
    private Identifier getLocation(Identifier base, String extension) {
        return new Identifier(base.getNamespace(), folder + "/" + base.getPath() + extension);
    }

    /**
     * Gets a location for .png
     */
    private Identifier getLocation(Identifier base) {
        return getLocation(base, ".png");
    }

    @Override
    public boolean exists(Identifier path) {
        return manager.getResource(getLocation(path)).isPresent();
    }

    @Override
    public boolean metadataExists(Identifier path) {
        return manager.getResource(getLocation(path, ".png.mcmeta")).isPresent();
    }

    @Override
    public NativeImage read(Identifier path) throws IOException {
        Resource resource = manager.getResource(getLocation(path)).orElseThrow(FileNotFoundException::new);
        NativeImage image = NativeImage.read(resource.getInputStream());
        openedImages.add(image);
        return image;
    }

    @Nullable
    @Override
    public NativeImage readIfExists(Identifier path) {
        Optional<Resource> resource = manager.getResource(getLocation(path));
        if (resource.isPresent()) {
            try {
                NativeImage image = NativeImage.read(resource.get().getInputStream());
                openedImages.add(image);
                return image;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public JsonObject readMetadata(Identifier path) throws IOException {
        try (BufferedReader reader = manager.getResource(getLocation(path, ".png.mcmeta")).orElseThrow(FileNotFoundException::new).getReader()) {
            return JsonHelper.deserialize(reader);
        }
    }
}
