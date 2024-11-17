package slimeknights.tconstruct.library.materials.definition;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import slimeknights.mantle.data.gson.ConditionSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.exception.TinkerJSONException;
import slimeknights.tconstruct.library.json.JsonRedirect;
import slimeknights.tconstruct.library.materials.json.MaterialJson;
import slimeknights.tconstruct.library.utils.GenericTagUtil;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

/**
 * Loads the material data from datapacks and provides them to whatever needs them.
 * Contains only the very basic material information, craftability, traits, but no stats.
 * See {@link slimeknights.tconstruct.library.materials.stats.MaterialStatsManager} for stats.
 * <p>
 * The location inside datapacks is "materials".
 * So if your mods name is "foobar", the location for your mods materials is "data/foobar/materials".
 */
@Log4j2
public class MaterialManager extends JsonDataLoader {
    /**
     * Location of materials
     */
    public static final String FOLDER = "tinkering/materials/definition";
    /**
     * Location of material tags
     */
    public static final String TAG_FOLDER = "tinkering/tags/materials";
    /**
     * Registry key to make tag keys, will not work on actual registry lookup
     */
    public static final RegistryKey<? extends Registry<IMaterial>> REGISTRY_KEY = RegistryKey.ofRegistry(TConstruct.getResource("materials"));

    /**
     * GSON for loading materials
     */
    public static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(Identifier.class, new Identifier.Serializer())
            .registerTypeAdapter(ICondition.class, ConditionSerializer.INSTANCE)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * Runnable to run after loading material stats
     */
    private final Runnable onLoaded;
    /**
     * Map of all materials
     */
    private Map<MaterialId, IMaterial> materials = Collections.emptyMap();
    /**
     * Map of material ID redirects
     */
    private Map<MaterialId, MaterialId> redirects = Collections.emptyMap();
    /**
     * Sorted list of visible materials
     */
    private List<IMaterial> sortedMaterials = Collections.emptyList();

    /**
     * Modifier tags loaded from JSON
     */
    private Map<TagKey<IMaterial>, List<IMaterial>> tags = Collections.emptyMap();
    /**
     * Map from modifier to tags on the modifier
     */
    private Map<MaterialId, Set<TagKey<IMaterial>>> reverseTags = Collections.emptyMap();
    /**
     * Context for conditions
     */
    @Setter
    private IContext conditionContext = IContext.EMPTY;

    public MaterialManager(Runnable onLoaded) {
        super(GSON, FOLDER);
        this.onLoaded = onLoaded;
    }

    @VisibleForTesting
    MaterialManager() {
        this(() -> {
        });
    }

    /**
     * Gets a collection of all loaded materials, sorted by tier and sort orders
     *
     * @return All loaded materials
     */
    public Collection<IMaterial> getVisibleMaterials() {
        return sortedMaterials;
    }

    /**
     * Gets a collection of all loaded materials, unsorted and including hidden materials
     *
     * @return All loaded materials
     */
    public Collection<IMaterial> getAllMaterials() {
        return materials.values();
    }

    /**
     * Gets a material based on its ID
     *
     * @param materialId Material ID
     * @return Optional of material, empty if missing
     */
    public Optional<IMaterial> getMaterial(MaterialId materialId) {
        return Optional.ofNullable(materials.get(materialId));
    }

    /**
     * Resolves any redirect for the given material ID
     *
     * @param materialId Original material ID
     * @return Redirected ID, or original if no redirect is set up for this ID
     */
    public MaterialId resolveRedirect(MaterialId materialId) {
        return redirects.getOrDefault(materialId, materialId);
    }


    /* Tags */

    /**
     * Creates a tag key for a material
     */
    public static TagKey<IMaterial> getTag(Identifier id) {
        return TagKey.of(REGISTRY_KEY, id);
    }

    /**
     * Checks if the given modifier is in the given tag
     *
     * @return True if the modifier is in the tag
     */
    public boolean isIn(MaterialId id, TagKey<IMaterial> tag) {
        return reverseTags.getOrDefault(id, Collections.emptySet()).contains(tag);
    }

    /**
     * Gets all values contained in the given tag
     *
     * @param tag Tag instance
     * @return Contained values
     */
    public List<IMaterial> getValues(TagKey<IMaterial> tag) {
        return tags.getOrDefault(tag, List.of());
    }


    /**
     * Recreates the fluid lookup and sorted list using the new materials list
     */
    private void onMaterialUpdate() {
        this.sortedMaterials = this.materials.values().stream()
                .filter(mat -> !mat.isHidden())
                .sorted().collect(Collectors.toList());
        onLoaded.run();
    }

    /**
     * Updates the material list from the server.list. Should only be called client side
     */
    public void updateMaterialsFromServer(Map<MaterialId, IMaterial> materials, Map<MaterialId, MaterialId> redirects, Map<TagKey<IMaterial>, List<IMaterial>> tags) {
        this.materials = materials;
        this.redirects = redirects;
        this.tags = tags;
        this.reverseTags = GenericTagUtil.reverseTags(IMaterial::getIdentifier, tags);
        onMaterialUpdate();
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> splashList, ResourceManager resourceManagerIn, Profiler profilerIn) {
        long time = System.nanoTime();
        Map<MaterialId, MaterialId> redirects = new HashMap<>();
        this.materials = splashList.entrySet().stream()
                .filter(entry -> entry.getValue().isJsonObject())
                .map(entry -> loadMaterial(entry.getKey(), entry.getValue().getAsJsonObject(), redirects))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        IMaterial::getIdentifier,
                        material -> material)
                );
        // validate redirects
        Iterator<Entry<MaterialId, MaterialId>> redirectIterator = redirects.entrySet().iterator();
        while (redirectIterator.hasNext()) {
            Entry<MaterialId, MaterialId> entry = redirectIterator.next();
            if (!this.materials.containsKey(entry.getValue())) {
                log.error("Invalid material redirect {} as material {} does not exist", entry.getKey(), entry.getValue());
                redirectIterator.remove();
            }
        }
        this.redirects = redirects;
        onMaterialUpdate();

        log.debug("Loaded materials: {}", Util.toIndentedStringList(materials.keySet()));
        log.debug("Loaded redirects: {}", Util.toIndentedStringList(redirects.keySet()));
        long timeStep = System.nanoTime();
        log.info("Loaded {} materials in {} ms", materials.size(), (timeStep - time) / 1000000f);


        // load modifier tags
        TagGroupLoader<IMaterial> tagLoader = new TagGroupLoader<>(id -> getMaterial(new MaterialId(id)), TAG_FOLDER);
        this.tags = GenericTagUtil.mapLoaderResults(REGISTRY_KEY, tagLoader.load(resourceManagerIn));
        this.reverseTags = GenericTagUtil.reverseTags(IMaterial::getIdentifier, this.tags);
        log.info("Loaded {} material tags for {} materials in {} ms", this.tags.size(), this.reverseTags.size(), (System.nanoTime() - timeStep) / 1000000f);
    }

    /**
     * Gets the packet to send on player login
     *
     * @return Packet object
     */
    public UpdateMaterialsPacket getUpdatePacket() {
        return new UpdateMaterialsPacket(this.materials, this.redirects, this.tags);
    }

    @Nullable
    private IMaterial loadMaterial(Identifier materialId, JsonObject jsonObject, Map<MaterialId, MaterialId> redirects) {
        try {
            MaterialJson materialJson = GSON.fromJson(jsonObject, MaterialJson.class);

            // if defined, the material will redirect to another material
            // processed first so a material can both conditionally redirect and fallback to a conditional material
            JsonRedirect[] redirectsJson = materialJson.getRedirect();
            if (redirectsJson != null) {
                for (JsonRedirect redirect : redirectsJson) {
                    ICondition redirectCondition = redirect.getCondition();
                    if (redirectCondition == null || redirectCondition.test(this.conditionContext)) {
                        MaterialId redirectTarget = new MaterialId(redirect.getId());
                        log.debug("Redirecting material {} to {}", materialId, redirectTarget);
                        redirects.put(new MaterialId(materialId), redirectTarget);
                        return null;
                    }
                }
            }

            // condition
            ICondition condition = materialJson.getCondition();
            if (condition != null && !condition.test(this.conditionContext)) {
                log.debug("Skipped loading material {} as it did not match the condition", materialId);
                return null;
            }

            if (materialJson.getCraftable() == null) {
                throw TinkerJSONException.materialJsonWithoutCraftingInformation(materialId);
            }

            boolean isCraftable = Boolean.TRUE.equals(materialJson.getCraftable());
            boolean hidden = Boolean.TRUE.equals(materialJson.getHidden());

            // parse trait
            return new Material(materialId, requireNonNullElse(materialJson.getTier(), 0), requireNonNullElse(materialJson.getSortOrder(), 100), isCraftable, hidden);
        } catch (Exception e) {
            log.error("Could not deserialize material {}. JSON: {}", materialId, jsonObject, e);
            return null;
        }
    }
}
