package slimeknights.tconstruct.library.tools.definition;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.common.network.TinkerNetwork;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * JSON loader that loads tool definitions from JSON
 */
@Log4j2
public class ToolDefinitionLoader extends JsonDataLoader {
    public static final String FOLDER = "tinkering/tool_definitions";
    private static final ToolDefinitionLoader INSTANCE = new ToolDefinitionLoader();

    /**
     * Map of loaded tool definition data
     */
    private Map<Identifier, ToolDefinitionData> dataMap = Collections.emptyMap();

    /**
     * Tool definitions registered to be loaded
     */
    private final Map<Identifier, ToolDefinition> definitions = new HashMap<>();

    private ToolDefinitionLoader() {
        super(JsonHelper.DEFAULT_GSON, FOLDER);
    }

    /**
     * Gets the instance of the definition loader
     */
    public static ToolDefinitionLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the tool definition loader
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::addDataPackListeners);
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::onDatapackSync);
    }

    /**
     * Updates the tool data from the server.list. Should only be called client side
     *
     * @param dataMap Server data map
     */
    protected void updateDataFromServer(Map<Identifier, ToolDefinitionData> dataMap) {
        this.dataMap = dataMap;
        for (Entry<Identifier, ToolDefinition> entry : this.definitions.entrySet()) {
            ToolDefinitionData data = dataMap.get(entry.getKey());
            ToolDefinition definition = entry.getValue();
            // errored serverside, so resolve without error here
            if (data != null) {
                definition.setData(data);
            } else {
                definition.clearData();
            }
        }
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> splashList, ResourceManager resourceManagerIn, Profiler profilerIn) {
        long time = System.nanoTime();
        ImmutableMap.Builder<Identifier, ToolDefinitionData> builder = ImmutableMap.builder();
        for (Entry<Identifier, ToolDefinition> entry : this.definitions.entrySet()) {
            Identifier key = entry.getKey();
            ToolDefinition definition = entry.getValue();
            // first, need to have a json for the given name
            JsonElement element = splashList.get(key);
            if (element == null) {
                log.error("Missing tool definition for tool {}", key);
                definition.clearData();
                continue;
            }
            try {
                ToolDefinitionData data = ToolDefinitionData.LOADABLE.convert(element, key.toString());
                builder.put(key, data);
                definition.setData(data);
            } catch (Exception e) {
                log.error("Failed to load tool definition for tool {}", key, e);
                definition.clearData();
            }
        }
        this.dataMap = builder.build();
        log.info("Loaded {} tool definitions in {} ms", this.dataMap.size(), (System.nanoTime() - time) / 1000000f);
    }

    /**
     * Gets a list of all tool definitions registered to the loader
     */
    public Collection<ToolDefinition> getRegisteredToolDefinitions() {
        return this.definitions.values();
    }

    /**
     * Called on datapack sync to send the tool data to all players
     */
    private void onDatapackSync(OnDatapackSyncEvent event) {
        UpdateToolDefinitionDataPacket packet = new UpdateToolDefinitionDataPacket(this.dataMap);
        TinkerNetwork.getInstance().sendToPlayerList(event.getPlayer(), event.getPlayerList(), packet);
    }

    /**
     * Adds the managers as datapack listeners
     */
    private void addDataPackListeners(final AddReloadListenerEvent event) {
        event.addListener(this);
    }

    /**
     * Registers a tool definition with the loader
     */
    public synchronized void registerToolDefinition(ToolDefinition definition) {
        Identifier name = definition.getId();
        if (this.definitions.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate tool definition " + name);
        }
        this.definitions.put(name, definition);
    }
}
