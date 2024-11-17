package slimeknights.tconstruct.tables.recipe;

import com.mojang.datafixers.util.Pair;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairToolHook;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IRepairKitItem;
import slimeknights.tconstruct.tables.TinkerTables;

/**
 * Recipe using repair kits in the crafting table
 */
public class CraftingTableRepairKitRecipe extends SpecialCraftingRecipe {
    public CraftingTableRepairKitRecipe(Identifier id) {
        super(id, CraftingRecipeCategory.MISC);
    }

    /**
     * Checks if the tool is valid for this recipe
     *
     * @param stack Tool to check
     * @return True if valid
     */
    protected boolean toolMatches(ItemStack stack) {
        return stack.isIn(TinkerTags.Items.DURABILITY);
    }

    /**
     * Gets the tool stack and the repair kit material from the crafting grid
     *
     * @param inv Crafting inventory
     * @return Relevant inputs, or null if invalid
     */
    @Nullable
    protected Pair<ToolStack, ItemStack> getRelevantInputs(RecipeInputInventory inv) {
        ToolStack tool = null;
        ItemStack repairKit = null;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            // repair kit - update material
            if (stack.getItem() instanceof IRepairKitItem) {
                // already found repair kit
                if (repairKit != null) {
                    return null;
                }
                MaterialId inputMaterial = IMaterialItem.getMaterialFromStack(stack).getId();
                // if the material is invalid, also fail
                if (inputMaterial.equals(IMaterial.UNKNOWN_ID)) {
                    return null;
                }
                repairKit = stack;
            } else if (toolMatches(stack)) {
                // cannot repair multiple tools
                if (tool != null) {
                    return null;
                }
                // tool must be damaged
                tool = ToolStack.from(stack);
                if (!tool.isBroken() && tool.getDamage() == 0) {
                    return null;
                }
            } else {
                // unknown item input
                return null;
            }
        }
        if (tool == null || repairKit == null) {
            return null;
        }
        return Pair.of(tool, repairKit);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World worldIn) {
        Pair<ToolStack, ItemStack> inputs = getRelevantInputs(inv);
        return inputs != null && MaterialRepairToolHook.canRepairWith(inputs.getFirst(), IMaterialItem.getMaterialFromStack(inputs.getSecond()).getId());
    }

    /**
     * Gets the amount to repair for the given material
     */
    protected float getRepairAmount(IToolStackView tool, ItemStack repairStack) {
        return MaterialRepairToolHook.repairAmount(tool, IMaterialItem.getMaterialFromStack(repairStack).getId());
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, DynamicRegistryManager manager) {
        Pair<ToolStack, ItemStack> inputs = getRelevantInputs(inv);
        if (inputs == null) {
            TConstruct.LOG.error("Recipe repair on {} failed to find items after matching", getId());
            return ItemStack.EMPTY;
        }

        // first identify materials and durability
        ToolStack tool = inputs.getFirst().copy();
        ItemStack repairKit = inputs.getSecond();
        // vanilla says 25% durability per ingot, repair kits are worth 2 ingots
        float repairAmount = getRepairAmount(tool, repairKit);
        if (repairAmount > 0) {
            // add in repair kit value
            repairAmount *= (repairKit.getItem() instanceof IRepairKitItem kit ? kit.getRepairAmount() : Config.COMMON.repairKitAmount.get().floatValue()) / MaterialRecipe.INGOTS_PER_REPAIR;
            // adjust the factor based on modifiers
            // main example is wood, +25% per level
            for (ModifierEntry entry : tool.getModifierList()) {
                repairAmount = entry.getHook(ModifierHooks.REPAIR_FACTOR).getRepairFactor(tool, entry, repairAmount);
                if (repairAmount <= 0) {
                    // failed to repair
                    return tool.createStack();
                }
            }

            // repair the tool
            ToolDamageUtil.repair(tool, (int) repairAmount);
        }
        // return final stack
        return tool.createStack();
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerTables.craftingTableRepairSerializer.get();
    }
}
