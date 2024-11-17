package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import org.jetbrains.annotations.Nullable;

public class ModifierRepairCraftingRecipe extends SpecialCraftingRecipe implements IModifierRepairRecipe {
    public static final RecordLoadable<ModifierRepairCraftingRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), MODIFIER_FIELD, INGREDIENT_FIELD, REPAIR_AMOUNT_FIELD, ModifierRepairCraftingRecipe::new);

    @Getter
    private final ModifierId modifier;
    @Getter
    private final Ingredient ingredient;
    @Getter
    private final int repairAmount;

    public ModifierRepairCraftingRecipe(Identifier idIn, ModifierId modifier, Ingredient ingredient, int repairAmount) {
        super(idIn);
        this.modifier = modifier;
        this.ingredient = ingredient;
        this.repairAmount = repairAmount;
    }

    /**
     * Gets the tool stack and the repair kit material from the crafting grid
     *
     * @param inv Crafting inventory
     * @return Relevant inputs, or null if invalid
     */
    @Nullable
    protected Pair<ToolStack, Integer> getRelevantInputs(RecipeInputInventory inv) {
        ToolStack tool = null;
        int itemsFound = 0;
        int modifierLevel = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            // repair kit - update material
            if (stack.isIn(TinkerTags.Items.DURABILITY)) {
                // cannot repair multiple tools
                if (tool != null) {
                    return null;
                }
                // tool must be damaged
                tool = ToolStack.from(stack);
                if (!tool.isBroken() && tool.getDamage() == 0) {
                    return null;
                }
                // tool must have the modifier
                modifierLevel = tool.getModifierLevel(this.modifier);
                if (modifierLevel == 0) {
                    return null;
                }

                // if we found a stack, add it to our count
            } else if (this.ingredient.test(stack)) {
                itemsFound++;
            } else {
                // unknown item input
                return null;
            }
        }
        // failed to find a tool or item? failed
        if (tool == null || itemsFound == 0) {
            return null;
        }
        return Pair.of(tool, this.repairAmount * itemsFound * modifierLevel);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        return this.getRelevantInputs(inv) != null;
    }

    @Override
    public ItemStack assemble(RecipeInputInventory inv) {
        Pair<ToolStack, Integer> inputs = this.getRelevantInputs(inv);
        if (inputs == null) {
            TConstruct.LOG.error("Recipe repair on {} failed to find items after matching", this.getId());
            return ItemStack.EMPTY;
        }

        // scale the repair based on the modifiers
        float repairAmount = inputs.getSecond();
        ToolStack tool = inputs.getFirst();
        for (ModifierEntry entry : tool.getModifierList()) {
            repairAmount = entry.getHook(ModifierHooks.REPAIR_FACTOR).getRepairFactor(tool, entry, repairAmount);
            if (repairAmount <= 0) {
                // failed to repair
                return ItemStack.EMPTY;
            }
        }

        // repair the tool
        tool = tool.copy();
        ToolDamageUtil.repair(tool, (int) repairAmount);
        return tool.createStack();
    }

    @Override
    public DefaultedList<ItemStack> getRemainingItems(RecipeInputInventory inv) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
        // step 1: find out how much we need to repair
        Pair<ToolStack, Integer> inputs = this.getRelevantInputs(inv);
        int repairPerItem = 0;
        int repairNeeded = 0;
        if (inputs != null) {
            ToolStack tool = inputs.getFirst();
            repairNeeded = tool.getDamage();
            float repairFloat = tool.getModifierLevel(this.modifier) * this.repairAmount;
            if (repairFloat > 0) {
                for (ModifierEntry entry : tool.getModifierList()) {
                    repairFloat = entry.getHook(ModifierHooks.REPAIR_FACTOR).getRepairFactor(tool, entry, repairFloat);
                    if (repairFloat <= 0) {
                        break;
                    }
                }
                repairPerItem = (int) repairFloat;
            }
        }

        // step 2: consume as many items as are needed to do the repair
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (this.ingredient.test(stack)) {
                // if done repairing, leave the items
                if (repairNeeded <= 0) {
                    continue;
                }
                repairNeeded -= repairPerItem;
            }
            if (stack.hasCraftingRemainingItem()) {
                list.set(i, stack.getCraftingRemainingItem());
            }
        }

        return list;
    }

    @Override
    public boolean fits(int width, int height) {
        return (width * height) >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.craftingModifierRepair.get();
    }
}
