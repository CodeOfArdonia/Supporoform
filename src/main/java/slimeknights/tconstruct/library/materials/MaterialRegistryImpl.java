package slimeknights.tconstruct.library.materials;

import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialManager;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsManager;
import slimeknights.tconstruct.library.materials.traits.MaterialTraitsManager;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import org.jetbrains.annotations.Nullable;
import net.minecraft.registry.tag.TagKey;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Holds all materials and the extra information registered for them (stat classes).
 * Materials are reset on every world load/join. Registered extra stuff is not.
 * <p>
 * For the Server, materials are loaded on server start/reload from the data packs.
 * For the Client, materials are synced from the server on server join.
 */
public class MaterialRegistryImpl implements IMaterialRegistry {
    private final MaterialManager materialManager;
    private final MaterialStatsManager materialStatsManager;
    private final MaterialTraitsManager materialTraitsManager;

    protected MaterialRegistryImpl(MaterialManager materialManager, MaterialStatsManager materialStatsManager, MaterialTraitsManager materialTraitsManager) {
        this.materialManager = materialManager;
        this.materialStatsManager = materialStatsManager;
        this.materialTraitsManager = materialTraitsManager;
    }


    /* Materials */

    @Override
    public MaterialId resolve(MaterialId id) {
        return this.materialManager.resolveRedirect(id);
    }

    @Override
    public IMaterial getMaterial(MaterialId id) {
        return this.materialManager.getMaterial(id).orElse(IMaterial.UNKNOWN);
    }

    @Override
    public Collection<IMaterial> getVisibleMaterials() {
        return this.materialManager.getVisibleMaterials();
    }

    @Override
    public Collection<IMaterial> getAllMaterials() {
        return this.materialManager.getAllMaterials();
    }


    /* Tags */

    @Override
    public boolean isInTag(MaterialId id, TagKey<IMaterial> tag) {
        return this.materialManager.isIn(id, tag);
    }

    @Override
    public List<IMaterial> getTagValues(TagKey<IMaterial> tag) {
        return this.materialManager.getValues(tag);
    }


    /* Stats */

    /**
     * Gets the loader for all stat types
     */
    @Override
    public Loadable<MaterialStatType<?>> getStatTypeLoader() {
        return this.materialStatsManager.getStatTypes();
    }

    @Nullable
    @Override
    public <T extends IMaterialStats> MaterialStatType<T> getStatType(MaterialStatsId statsId) {
        return this.materialStatsManager.getStatType(statsId);
    }

    @Override
    public <T extends IMaterialStats> Optional<T> getMaterialStats(MaterialId materialId, MaterialStatsId statsId) {
        return this.materialStatsManager.getStats(materialId, statsId);
    }

    @Override
    public Collection<IMaterialStats> getAllStats(MaterialId materialId) {
        return this.materialStatsManager.getAllStats(materialId);
    }

    @Override
    public void registerStatType(MaterialStatType<?> type) {
        this.materialStatsManager.registerStatType(type);
    }

    @Override
    public void registerStatType(MaterialStatType<?> type, @Nullable MaterialStatsId fallback) {
        this.registerStatType(type);
        if (fallback != null) {
            this.materialTraitsManager.registerStatTypeFallback(type.getId(), fallback);
        }
    }


    /* Traits */

    @Override
    public List<ModifierEntry> getDefaultTraits(MaterialId materialId) {
        return this.materialTraitsManager.getDefaultTraits(materialId);
    }

    @Override
    public boolean hasUniqueTraits(MaterialId materialId, MaterialStatsId statsId) {
        return this.materialTraitsManager.hasUniqueTraits(materialId, statsId);
    }

    @Override
    public List<ModifierEntry> getTraits(MaterialId materialId, MaterialStatsId statsId) {
        return this.materialTraitsManager.getTraits(materialId, statsId);
    }
}
