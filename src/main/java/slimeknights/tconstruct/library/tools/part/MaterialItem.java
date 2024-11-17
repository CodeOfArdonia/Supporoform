package slimeknights.tconstruct.library.tools.part;

import net.minecraft.registry.Registries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.utils.DomainDisplayName;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

/**
 * Represents an item that has a Material associated with it. The NBT of the itemstack identifies which material the
 * itemstack of this item has.
 */
public class MaterialItem extends Item implements IMaterialItem {
    private static final String ADDED_BY = TConstruct.makeTranslationKey("tooltip", "part.added_by");

    public MaterialItem(Settings properties) {
        super(properties);
    }

    /**
     * Gets the material ID for the given NBT compound
     */
    private static MaterialVariantId getMaterialId(@Nullable NbtCompound nbt) {
        if (nbt != null) {
            String str = nbt.getString(MATERIAL_TAG);
            if (!str.isEmpty()) {
                MaterialVariantId id = MaterialVariantId.tryParse(str);
                if (id != null) {
                    return id;
                }
            }
        }
        return IMaterial.UNKNOWN_ID;
    }

    @Override
    public MaterialVariantId getMaterial(ItemStack stack) {
        return getMaterialId(stack.getNbt());
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.allowedIn(group) && MaterialRegistry.isFullyLoaded()) {
            // if a specific material is set in the config, try adding that
            String showOnlyId = Config.COMMON.showOnlyPartMaterial.get();
            boolean added = false;
            if (!showOnlyId.isEmpty()) {
                MaterialVariantId materialId = MaterialVariantId.tryParse(showOnlyId);
                if (materialId != null && this.canUseMaterial(materialId.getId())) {
                    items.add(this.withMaterialForDisplay(materialId));
                    added = true;
                }
            }
            // if no material is set or we failed to find it, iterate all materials
            if (!added) {
                for (IMaterial material : MaterialRegistry.getInstance().getVisibleMaterials()) {
                    MaterialId id = material.getIdentifier();
                    if (this.canUseMaterial(id)) {
                        items.add(this.withMaterial(id));
                        // if a specific material was requested and not found, stop after first
                        if (!showOnlyId.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private static Text getName(String baseKey, MaterialVariantId material) {
        // if there is a specific name, use that
        Identifier location = material.getLocation('.');
        String fullKey = String.format("%s.%s.%s", baseKey, location.getNamespace(), location.getPath());
        if (Util.canTranslate(fullKey)) {
            return Text.translatable(fullKey);
        }
        // try material name prefix next
        String materialKey = MaterialTooltipCache.getKey(material);
        String materialPrefix = materialKey + ".format";
        if (Util.canTranslate(materialPrefix)) {
            return Text.translatable(materialPrefix, Text.translatable(baseKey));
        }
        // format as "<material> <item name>"
        if (Util.canTranslate(materialKey)) {
            return Text.translatable(TooltipUtil.KEY_FORMAT, Text.translatable(materialKey), Text.translatable(baseKey));
        }
        return null;
    }

    @Override
    public Text getName(ItemStack stack) {
        // if no material, return part name directly
        MaterialVariantId material = this.getMaterial(stack);
        if (material.equals(IMaterial.UNKNOWN_ID)) {
            return super.getName(stack);
        }
        // try variant first
        String key = this.getTranslationKey(stack);
        if (material.hasVariant()) {
            Text component = getName(key, material);
            if (component != null) {
                return component;
            }
        }
        // if variant did not work, do base material
        Text component = getName(key, material.getId());
        if (component != null) {
            return component;
        }
        // if neither worked, format directly
        return Text.translatable(key);
    }

    @Nullable
    @Override
    public String getCreatorModId(ItemStack stack) {
        MaterialVariantId material = this.getMaterial(stack);
        if (!IMaterial.UNKNOWN_ID.equals(material)) {
            return material.getId().getNamespace();
        }
        return Registries.ITEM.getId(this).getNamespace();
    }


    /**
     * Adds the mod that added the material to the tooltip
     *
     * @param tooltip  Tooltip list
     * @param material Material to add
     */
    protected static void addModTooltip(IMaterial material, List<Text> tooltip) {
        if (material != IMaterial.UNKNOWN) {
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable(ADDED_BY, DomainDisplayName.nameFor(material.getIdentifier().getNamespace())));
        }
    }

    @Override
    public void postProcessNbt(NbtCompound nbt) {
        // if the material exists and was changed, update it
        MaterialVariantId id = getMaterialId(nbt);
        if (!id.equals(IMaterial.UNKNOWN_ID)) {
            MaterialId original = id.getId();
            MaterialId resolved = MaterialRegistry.getInstance().resolve(original);
            if (original != resolved) {
                nbt.putString(MATERIAL_TAG, MaterialVariantId.create(resolved, id.getVariant()).toString());
            }
        }
    }
}
