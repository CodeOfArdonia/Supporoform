package slimeknights.mantle.fluid.transfer;

import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.gson.GenericRegisteredSerializer;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.util.JsonHelper;

import java.util.*;
import java.util.function.Consumer;

/**
 * Logic for filling and emptying fluid containers that are not fluid handlers
 */
@Log4j2
public class FluidContainerTransferManager extends JsonDataLoader {
    /**
     * Map of all modifier types that are expected to load in data packs
     */
    public static final GenericRegisteredSerializer<IFluidContainerTransfer> TRANSFER_LOADERS = new GenericRegisteredSerializer<>();
    /**
     * Folder for saving the logic
     */
    public static final String FOLDER = "mantle/fluid_transfer";
    /**
     * GSON instance
     */
    public static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(Identifier.class, new Identifier.Serializer())
            .registerTypeHierarchyAdapter(IFluidContainerTransfer.class, TRANSFER_LOADERS)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    /**
     * Singleton instance of the manager
     */
    public static final FluidContainerTransferManager INSTANCE = new FluidContainerTransferManager();

    /**
     * List of loaded transfer logic, only exists serverside
     */
    private List<IFluidContainerTransfer> transfers = Collections.emptyList();

    /**
     * Set of all items that match a recipe, exists on both sides
     */
    @Setter
    @Nullable
    private Set<Item> containerItems = Collections.emptySet();

    /**
     * Condition context for tags
     */
    private IContext context = IContext.EMPTY;

    private FluidContainerTransferManager() {
        super(GSON, FOLDER);
    }

    /**
     * Lazily initializes the set of container items
     */
    protected Set<Item> getContainerItems() {
        if (this.containerItems == null) {
            ImmutableSet.Builder<Item> builder = ImmutableSet.builder();
            Consumer<Item> consumer = builder::add;
            for (IFluidContainerTransfer transfer : this.transfers) {
                transfer.addRepresentativeItems(consumer);
            }
            this.containerItems = builder.build();
        }
        return this.containerItems;
    }

    /**
     * For internal use only
     */
    public void init() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AddReloadListenerEvent.class, e -> {
            e.addListener(this);
            this.context = e.getConditionContext();
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OnDatapackSyncEvent.class, e -> JsonHelper.syncPackets(e, MantleNetwork.INSTANCE, new FluidContainerTransferPacket(this.getContainerItems())));
    }

    /**
     * Loads transfer from JSON
     */
    @Nullable
    private IFluidContainerTransfer loadFluidTransfer(Identifier key, JsonObject json) {
        try {
            if (!json.has("conditions") || CraftingHelper.processConditions(net.minecraft.util.JsonHelper.getArray(json, "conditions"), this.context)) {
                return GSON.fromJson(json, IFluidContainerTransfer.class);
            }
        } catch (JsonSyntaxException e) {
            log.error("Failed to load fluid container transfer info from {}", key, e);
        }
        return null;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> splashList, ResourceManager manager, Profiler profiler) {
        long time = System.nanoTime();
        this.transfers = splashList.entrySet().stream()
                .map(entry -> this.loadFluidTransfer(entry.getKey(), entry.getValue().getAsJsonObject()))
                .filter(Objects::nonNull)
                .toList();
        this.containerItems = null;
        log.info("Loaded {} dynamic modifiers in {} ms", this.transfers.size(), (System.nanoTime() - time) / 1000000f);
    }

    /**
     * Checks if the given stack could possibly match, used client side to determine if the fluid transfer falls back to opening the UI
     *
     * @param stack Stack to check
     * @return True if a match is possible, basically just checks item ID
     */
    public boolean mayHaveTransfer(ItemStack stack) {
        return this.getContainerItems().contains(stack.getItem());
    }

    /**
     * Gets the transfer for the given item and fluid, or null if its not a valid item and fluid
     */
    @Nullable
    public IFluidContainerTransfer getTransfer(ItemStack stack, FluidStack fluid) {
        for (IFluidContainerTransfer transfer : this.transfers) {
            if (transfer.matches(stack, fluid)) {
                return transfer;
            }
        }
        return null;
    }
}
