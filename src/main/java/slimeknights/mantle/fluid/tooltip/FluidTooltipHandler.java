package slimeknights.mantle.fluid.tooltip;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.gson.TagKeySerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.JsonHelper;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Handles fluid units displaying in tooltips
 */
@SuppressWarnings("unused")
@Log4j2
public class FluidTooltipHandler extends JsonDataLoader {
    /**
     * Tooltip when not holding shift mentioning that is possible
     */
    public static final Text HOLD_SHIFT = Mantle.makeComponent("gui", "fluid.hold_shift").formatted(Formatting.GRAY);
    /**
     * Folder for saving the logic
     */
    public static final String FOLDER = "mantle/fluid_tooltips";
    /**
     * GSON instance
     */
    // TODO: do we even need GSON here? I feel a classical serializer is sufficient as this class is pretty simple
    public static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(Identifier.class, new Identifier.Serializer())
            .registerTypeAdapter(FluidIngredient.class, FluidIngredient.LOADABLE)
            .registerTypeAdapter(TagKey.class, new TagKeySerializer<>(RegistryKeys.FLUID))
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * ID of the default fallback
     */
    public static final Identifier DEFAULT_ID = Mantle.getResource("fallback");

    /* Base units */
    private static final FluidUnit BUCKET = new FluidUnit(Mantle.makeDescriptionId("gui", "fluid.bucket"), 1000);
    private static final FluidUnit MILLIBUCKET = new FluidUnit(Mantle.makeDescriptionId("gui", "fluid.millibucket"), 1);
    /**
     * Default fallback in case resource pack has none
     */
    private static final FluidUnitList DEFAULT_LIST = new FluidUnitList(null, Collections.singletonList(BUCKET));

    /**
     * Formatter as a biconsumer, shows up in a few places
     */
    public static final BiConsumer<Long, List<Text>> BUCKET_FORMATTER = FluidTooltipHandler::appendBuckets;

    /* Instance data */
    public static final FluidTooltipHandler INSTANCE = new FluidTooltipHandler();

    /**
     * Fallback to use when no list matches
     */
    private FluidUnitList fallback = DEFAULT_LIST;
    /**
     * List of tooltip options
     */
    private Map<Identifier, FluidUnitList> unitLists = Collections.emptyMap();
    /**
     * Cache of fluid to entry
     */
    private final Map<Fluid, FluidUnitList> listCache = new HashMap<>();

    /**
     * Initializes this manager, registering it with the resource manager
     *
     * @param manager Manager
     */
    public static void init(RegisterClientReloadListenersEvent manager) {
        manager.registerReloadListener(INSTANCE);
        // clear the cache on tag reload, if the tags changed it might be wrong
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TagsUpdatedEvent.class, event -> INSTANCE.listCache.clear());
    }

    private FluidTooltipHandler() {
        super(GSON, FOLDER);
    }

    /**
     * Loads from JSON
     */
    @Nullable
    private static FluidUnitList loadList(Identifier key, JsonElement json) {
        try {
            return GSON.fromJson(json, FluidUnitList.class);
        } catch (JsonSyntaxException e) {
            log.error("Failed to load fluid container transfer info from {}", key, e);
            return null;
        }
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> splashList, ResourceManager manager, Profiler profiler) {
        long time = System.nanoTime();
        ImmutableMap.Builder<Identifier, FluidUnitList> builder = ImmutableMap.builder();
        Map<Identifier, Identifier> redirects = new HashMap<>();
        for (Entry<Identifier, JsonElement> entry : splashList.entrySet()) {
            Identifier key = entry.getKey();
            JsonElement element = entry.getValue();

            // if a redirect, store in the map for later
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("redirect")) {
                    Identifier redirect = JsonHelper.getResourceLocation(object, "redirect");
                    redirects.put(key, redirect);
                    continue;
                }
            }
            // parse list regularly
            FluidUnitList list = loadList(key, element);
            if (list != null) {
                builder.put(key, list);
            }
        }
        // process redirects
        Map<Identifier, FluidUnitList> mapBeforeRedirects = builder.build();
        builder = ImmutableMap.builder();
        builder.putAll(mapBeforeRedirects);
        for (Entry<Identifier, Identifier> entry : redirects.entrySet()) {
            Identifier from = entry.getKey();
            Identifier to = entry.getValue();
            FluidUnitList list = mapBeforeRedirects.get(to);
            if (list != null) {
                builder.put(from, list);
            } else {
                log.error("Invalid fluid tooltip redirect {} as unit list {} does not exist", from, to);
            }
        }
        // find the fallback
        this.unitLists = builder.build();
        this.fallback = this.unitLists.getOrDefault(DEFAULT_ID, DEFAULT_LIST);
        this.listCache.clear();
        log.info("Loaded {} fluid unit lists in {} ms", this.unitLists.size(), (System.nanoTime() - time) / 1000000f);
    }

    /**
     * Gets the unit list for the given fluid
     */
    private FluidUnitList getUnitList(Fluid fluid) {
        FluidUnitList cached = this.listCache.get(fluid);
        if (cached != null) {
            return cached;
        }
        for (FluidUnitList list : this.unitLists.values()) {
            if (list.matches(fluid)) {
                this.listCache.put(fluid, list);
                return list;
            }
        }
        this.listCache.put(fluid, this.fallback);
        return this.fallback;
    }

    /**
     * Gets the unit list for the given ID
     */
    private FluidUnitList getUnitList(Identifier id) {
        return this.unitLists.getOrDefault(id, this.fallback);
    }


    /* External utilities */

    /**
     * Gets the tooltip for a fluid stack
     *
     * @param fluid Fluid stack instance
     * @return Fluid tooltip
     */
    public static List<Text> getFluidTooltip(FluidStack fluid) {
        return getFluidTooltip(fluid, fluid.getAmount());
    }

    /**
     * Gets the tooltip for a fluid stack
     *
     * @param fluid  Fluid stack instance
     * @param amount Amount override
     * @return Fluid tooltip
     */
    public static List<Text> getFluidTooltip(FluidStack fluid, long amount) {
        List<Text> tooltip = new ArrayList<>();
        // fluid name, not sure if there is a cleaner way to do this
        tooltip.add(fluid.getDisplayName().copyContentOnly().formatted(Formatting.WHITE));
        // material
        appendMaterial(fluid.getFluid(), amount, tooltip);
        // add mod display name
        FabricLoader.getInstance().getModContainer(Registries.FLUID.getId(fluid.getFluid()).getNamespace())
                .map(container -> container.getMetadata().getName())
                .ifPresent(name -> tooltip.add(Text.literal(name).formatted(Formatting.BLUE, Formatting.ITALIC)));
        return tooltip;
    }

    /**
     * Adds information for the tooltip based on material units
     *
     * @param fluid   Input fluid stack
     * @param tooltip Tooltip to append information
     */
    public static void appendMaterial(FluidStack fluid, List<Text> tooltip) {
        appendMaterial(fluid.getFluid(), fluid.getAmount(), tooltip);
    }

    /**
     * Adds information for the tooltip based on material units
     *
     * @param fluid   Input fluid
     * @param amount  Input amount
     * @param tooltip Tooltip to append information
     */
    public static void appendMaterial(Fluid fluid, long amount, List<Text> tooltip) {
        if (appendMaterialNoShift(fluid, amount, tooltip)) {
            appendShift(tooltip);
        }
    }

    /**
     * Adds information for the tooltip based on material units, does not show "hold shift for buckets"
     *
     * @param fluid    Input fluid
     * @param original Input amount
     * @param tooltip  Tooltip to append information
     * @return True if the amount is not in buckets
     */
    public static boolean appendMaterialNoShift(Fluid fluid, long original, List<Text> tooltip) {
        // if holding shift, skip specific units
        if (SafeClientAccess.getTooltipKey() != TooltipKey.SHIFT) {
            long amount = original;
            amount = INSTANCE.getUnitList(fluid).getText(tooltip, amount);
            MILLIBUCKET.getText(tooltip, amount);
            return INSTANCE.listCache.get(fluid) != INSTANCE.fallback;
        } else {
            // standard display stuff: bucket amounts
            appendBuckets(original, tooltip);
            return false;
        }
    }

    /**
     * Appends the hold shift message to the tooltip
     *
     * @param tooltip Tooltip to append information
     */
    public static void appendShift(List<Text> tooltip) {
        if (!SafeClientAccess.getTooltipKey().isShiftOrUnknown()) {
            tooltip.add(Text.empty());
            tooltip.add(HOLD_SHIFT);
        }
    }

    /**
     * Adds information to the tooltip based on a named list, allows customizing display for a specific location
     *
     * @param id      ID of the list to append
     * @param amount  Fluid amount
     * @param tooltip Tooltip to append information
     */
    public static void appendNamedList(Identifier id, long amount, List<Text> tooltip) {
        amount = INSTANCE.getUnitList(id).getText(tooltip, amount);
        appendBuckets(amount, tooltip);
    }

    /**
     * Adds information to the tooltip based on the fluid using bucket units
     *
     * @param amount  Fluid amount
     * @param tooltip Tooltip to append information
     */
    public static void appendBuckets(long amount, List<Text> tooltip) {
        amount = INSTANCE.fallback.getText(tooltip, amount);
        MILLIBUCKET.getText(tooltip, amount);
    }

}
