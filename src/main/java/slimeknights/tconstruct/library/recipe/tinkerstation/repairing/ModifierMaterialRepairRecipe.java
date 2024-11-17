package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.LazyMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairModule;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;

/**
 * Recipe to repair a specialized tool in the tinker station
 */
public class ModifierMaterialRepairRecipe extends TinkerStationRepairRecipe implements IModifierMaterialRepairRecipe {
    public static final RecordLoadable<ModifierMaterialRepairRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), MODIFIER_FIELD, REPAIR_MATERIAL_FIELD, STAT_TYPE_FIELD, ModifierMaterialRepairRecipe::new);

    /**
     * Tool that can be repaired with this recipe
     */
    @Getter
    private final ModifierId modifier;
    /**
     * ID of material used in repairing
     */
    private final LazyMaterial repairMaterial;
    /**
     * Stat type used for repairing, null means it will be fetched as the first available stat type
     */
    @Getter
    private final MaterialStatsId statType;

    public ModifierMaterialRepairRecipe(Identifier id, ModifierId modifier, MaterialId repairMaterialID, MaterialStatsId statType) {
        super(id);
        this.modifier = modifier;
        this.repairMaterial = LazyMaterial.of(repairMaterialID);
        this.statType = statType;
    }

    @Override
    public MaterialId getRepairMaterial() {
        return this.repairMaterial.getId();
    }

    @Override
    public boolean matches(ITinkerStationContainer inv, World world) {
        if (this.repairMaterial.isUnknown()) {
            return false;
        }
        // must have the modifier
        ItemStack tinkerable = inv.getTinkerableStack();
        if (!tinkerable.isIn(TinkerTags.Items.MODIFIABLE) || ModifierUtil.getModifierLevel(tinkerable, this.modifier) == 0) {
            return false;
        }
        return findMaterialItem(inv, this.repairMaterial.getId());
    }

    @Override
    protected float getRepairAmount(IToolStackView tool, MaterialId repairMaterial) {
        return MaterialRepairModule.getDurability(tool.getDefinition().getId(), repairMaterial, this.statType) * tool.getModifierLevel(this.modifier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.modifierMaterialRepair.get();
    }


    /**
     * Find the repair item in the inventory
     */
    private static boolean findMaterialItem(ITinkerStationContainer inv, MaterialId repairMaterial) {
        // validate that we have at least one material
        boolean found = false;
        for (int i = 0; i < inv.getInputCount(); i++) {
            // skip empty slots
            ItemStack stack = inv.getInput(i);
            if (stack.isEmpty()) {
                continue;
            }

            // ensure we have a material
            if (!repairMaterial.equals(TinkerStationRepairRecipe.getMaterialFrom(inv, i))) {
                return false;
            }
            found = true;
        }
        return found;
    }
}
