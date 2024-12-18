package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairModule;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;

/**
 * Recipe for using a repair kit in a crafting station for a specialized tool
 */
public class ModifierMaterialRepairKitRecipe extends CraftingTableRepairKitRecipe implements IModifierMaterialRepairRecipe {
    public static final RecordLoadable<ModifierMaterialRepairKitRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), MODIFIER_FIELD, REPAIR_MATERIAL_FIELD, STAT_TYPE_FIELD, ModifierMaterialRepairKitRecipe::new);

    /**
     * Tool that can be repaired with this recipe
     */
    @Getter
    private final ModifierId modifier;
    /**
     * ID of material used in repairing
     */
    @Getter
    private final MaterialId repairMaterial;
    /**
     * Stat type used for repairing, null means it will be fetched as the first available stat type
     */
    @Getter
    private final MaterialStatsId statType;

    public ModifierMaterialRepairKitRecipe(Identifier id, ModifierId modifier, MaterialId repairMaterial, MaterialStatsId statType) {
        super(id);
        this.modifier = modifier;
        this.repairMaterial = repairMaterial;
        this.statType = statType;
    }

    @Override
    protected boolean toolMatches(ItemStack stack) {
        return stack.isIn(TinkerTags.Items.DURABILITY) && ModifierUtil.getModifierLevel(stack, this.modifier) > 0;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World worldIn) {
        Pair<ToolStack, ItemStack> inputs = this.getRelevantInputs(inv);
        return inputs != null && this.repairMaterial.equals(IMaterialItem.getMaterialFromStack(inputs.getSecond()).getId());
    }

    @Override
    protected float getRepairAmount(IToolStackView tool, ItemStack repairStack) {
        return MaterialRepairModule.getDurability(tool.getDefinition().getId(), this.repairMaterial.getId(), this.statType) * tool.getModifierLevel(this.modifier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.craftingModifierMaterialRepair.get();
    }
}
