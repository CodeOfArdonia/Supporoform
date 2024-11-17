package slimeknights.tconstruct.library.modifiers.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.JsonUtils;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manager for spilling fluids for spilling, slurping, and wetting
 */
public class FluidEffectManager extends JsonDataLoader {
    /**
     * Recipe folder
     */
    public static final String FOLDER = "tinkering/fluid_effects";

    /**
     * Singleton instance of the modifier manager
     */
    public static final FluidEffectManager INSTANCE = new FluidEffectManager();

    /**
     * List of available fluids, only exists serverside
     */
    private List<FluidEffects> fluids = List.of();
    /**
     * Cache of fluid to recipe, recipe will be null client side
     */
    private final Map<Fluid, FluidEffects> cache = new ConcurrentHashMap<>();

    /**
     * Empty spilling fluid instance
     */
    private static final FluidEffects EMPTY = new FluidEffects(FluidIngredient.EMPTY, List.of(), List.of());

    /**
     * Condition context for recipe loading
     */
    private IContext conditionContext = IContext.EMPTY;

    private FluidEffectManager() {
        super(JsonHelper.DEFAULT_GSON, FOLDER);
    }

    /**
     * For internal use only
     */
    public void init() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AddReloadListenerEvent.class, this::addDataPackListeners);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OnDatapackSyncEvent.class, e -> JsonUtils.syncPackets(e, new UpdateFluidEffectsPacket(this.fluids)));
    }

    /**
     * Adds the managers as datapack listeners
     */
    private void addDataPackListeners(final AddReloadListenerEvent event) {
        event.addListener(this);
        this.conditionContext = event.getConditionContext();
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> splashList, ResourceManager pResourceManager, Profiler pProfiler) {
        long time = System.nanoTime();

        // load spilling from JSON
        this.fluids = splashList.entrySet().stream()
                .map(entry -> this.loadFluid(entry.getKey(), entry.getValue().getAsJsonObject()))
                .filter(Objects::nonNull)
                .toList();
        this.cache.clear();
        TConstruct.LOG.info("Loaded {} spilling fluids in {} ms", this.fluids.size(), (System.nanoTime() - time) / 1000000f);
    }

    /**
     * Loads a modifier from JSON
     */
    @Nullable
    private FluidEffects loadFluid(Identifier key, JsonElement element) {
        try {
            JsonObject json = net.minecraft.util.JsonHelper.asObject(element, "fluid_effect");

            // want to parse condition without parsing effects, as the effect serializer may be missing
            if (!CraftingHelper.processConditions(json, "conditions", this.conditionContext)) {
                return null;
            }
            return FluidEffects.LOADABLE.deserialize(json);
        } catch (JsonSyntaxException e) {
            TConstruct.LOG.error("Failed to load fluid effect {}", key, e);
            return null;
        }
    }

    /**
     * Updates the modifiers from the server
     */
    void updateFromServer(List<FluidEffects> fluids) {
        this.fluids = fluids;
        this.cache.clear();
    }

    /**
     * Finds a fluid without checking the cache, returns null if missing
     */
    private final Function<Fluid, FluidEffects> FIND_UNCACHED = fluid -> {
        // find all severing recipes for the entity
        for (FluidEffects recipe : this.fluids) {
            if (recipe.matches(fluid)) {
                return recipe;
            }
        }
        // cache null if nothing
        return EMPTY;
    };

    /**
     * Gets the recipe for the given fluid. Does not work client side
     *
     * @param fluid Fluid
     * @return Fluid, or empty if none exists
     */
    public FluidEffects find(Fluid fluid) {
        return this.cache.computeIfAbsent(fluid, this.FIND_UNCACHED);
    }
}
