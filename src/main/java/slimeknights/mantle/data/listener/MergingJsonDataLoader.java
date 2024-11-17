package slimeknights.mantle.data.listener;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import slimeknights.mantle.util.JsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Alternative to {@link net.minecraft.resource.JsonDataLoader} that merges all json into a single builder rather than taking the top most JSON.
 *
 * @param <B> Builder class
 */
@RequiredArgsConstructor
@Log4j2
public abstract class MergingJsonDataLoader<B> implements SynchronousResourceReloader {

    @VisibleForTesting
    protected final Gson gson;
    @VisibleForTesting
    protected final String folder;
    @VisibleForTesting
    protected final Function<Identifier, B> builderConstructor;

    /**
     * Parses a particular JSON into the builder
     *
     * @param builder Builder object
     * @param id      ID of json being parsed
     * @param element JSON data
     * @throws JsonSyntaxException If the json failed to parse
     */
    protected abstract void parse(B builder, Identifier id, JsonElement element) throws JsonSyntaxException;

    /**
     * Called when the JSON finished parsing to handle the final map
     *
     * @param map     Map of data
     * @param manager Resource manager
     */
    protected abstract void finishLoad(Map<Identifier, B> map, ResourceManager manager);

    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, B> map = new HashMap<>();
        for (Entry<Identifier, List<Resource>> entry : manager.findAllResources(this.folder, fileName -> fileName.getPath().endsWith(".json")).entrySet()) {
            Identifier filePath = entry.getKey();
            Identifier id = JsonHelper.localize(filePath, this.folder, ".json");

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.getReader()) {
                    JsonElement json = net.minecraft.util.JsonHelper.deserialize(this.gson, reader, JsonElement.class);
                    if (json == null) {
                        log.error("Couldn't load data file {} from {} in data pack {} as its null or empty", id, filePath, resource.getResourcePackName());
                    } else {
                        B builder = map.computeIfAbsent(id, this.builderConstructor);
                        this.parse(builder, id, json);
                    }
                } catch (RuntimeException | IOException ex) {
                    log.error("Couldn't parse data file {} from {} in data pack {}", id, filePath, resource.getResourcePackName(), ex);
                }
            }
        }
        this.finishLoad(map, manager);
    }
}
