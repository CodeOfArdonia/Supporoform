package slimeknights.mantle.fluid.texture;

import com.google.gson.JsonElement;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.minecraft.registry.Registry;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.JsonHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for handling fluid textures
 */
public class FluidTextureManager extends JsonDataLoader {
    /**
     * Folder containing the logic
     */
    public static final String FOLDER = "mantle/fluid_texture";

    /* Instance data */
    private static final FluidTextureManager INSTANCE = new FluidTextureManager();
    /**
     * Map of fluid type to texture
     */
    private Map<FluidType, FluidTexture> textures = Collections.emptyMap();
    /**
     * Fallback texture instance
     */
    private static final FluidTexture FALLBACK = new FluidTexture(new Identifier("block/water_still"), new Identifier("block/water_flow"), null, null, -1);

    private FluidTextureManager() {
        super(JsonHelper.DEFAULT_GSON, FOLDER);
    }

    /**
     * Initializes this manager, registering it with the resource manager
     */
    public static void init(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsons, ResourceManager pResourceManager, Profiler pProfiler) {
        long time = System.nanoTime();
        Map<FluidType, FluidTexture> map = new HashMap<>();
        Registry<FluidType> fluidTypeRegistry = PortingLibFluids.FLUID_TYPES;

        for (Map.Entry<Identifier, JsonElement> entry : jsons.entrySet()) {
            Identifier fullPath = entry.getKey();
            Identifier id = JsonHelper.localize(fullPath, FOLDER, ".json");
            // first step is to find the matching fluid type, if there is none ignore the file
            FluidType type = fluidTypeRegistry.get(id);
            if (type == null || !id.equals(fluidTypeRegistry.getId(type))) {
                Mantle.logger.debug("Ignoring fluid texture {} from {} as no fluid type exists with that name", id, fullPath);
            } else {
                // parse it if valid
                map.put(type, FluidTexture.deserialize(net.minecraft.util.JsonHelper.asObject(entry.getValue(), "fluid_texture")));
            }
        }
        this.textures = map;
        Mantle.logger.info("Loaded {} fluid textures in {} ms", map.size(), (System.nanoTime() - time) / 1000000f);
    }

    /**
     * Gets the texture for the given fluid
     */
    public static FluidTexture getData(FluidType fluid) {
        return INSTANCE.textures.getOrDefault(fluid, FALLBACK);
    }

    /**
     * Gets the still texture for the given fluid
     */
    public static Identifier getStillTexture(FluidType fluid) {
        return getData(fluid).still();
    }

    /**
     * Gets the still texture for the given fluid
     */
    public static Identifier getFlowingTexture(FluidType fluid) {
        return getData(fluid).flowing();
    }

    /**
     * Gets the still texture for the given fluid
     */
    @Nullable
    public static Identifier getOverlayTexture(FluidType fluid) {
        return getData(fluid).overlay();
    }

    /**
     * Gets the still texture for the given fluid
     */
    @Nullable
    public static Identifier getCameraTexture(FluidType fluid) {
        return getData(fluid).camera();
    }

    /**
     * Gets the still texture for the given fluid
     */
    public static int getColor(FluidType fluid) {
        return getData(fluid).color();
    }
}
