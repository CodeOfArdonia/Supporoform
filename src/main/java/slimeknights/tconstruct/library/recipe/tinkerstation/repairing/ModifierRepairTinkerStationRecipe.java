package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IncrementalModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

@RequiredArgsConstructor
public class ModifierRepairTinkerStationRecipe implements ITinkerStationRecipe, IModifierRepairRecipe {
    public static final RecordLoadable<ModifierRepairTinkerStationRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), MODIFIER_FIELD, INGREDIENT_FIELD, REPAIR_AMOUNT_FIELD, ModifierRepairTinkerStationRecipe::new);

    @Getter
    private final Identifier id;
    @Getter
    private final ModifierId modifier;
    @Getter
    private final Ingredient ingredient;
    @Getter
    private final int repairAmount;

    @Override
    public boolean matches(ITinkerStationContainer inv, World world) {
        ItemStack tinkerable = inv.getTinkerableStack();
        if (!tinkerable.isIn(TinkerTags.Items.DURABILITY)) {
            return false;
        }
        ToolStack tool = inv.getTinkerable();
        if (tool.getModifierLevel(this.modifier) == 0) {
            return false;
        }
        return IncrementalModifierRecipe.containsOnlyIngredient(inv, this.ingredient);
    }

    @Override
    public RecipeResult<ItemStack> getValidatedResult(ITinkerStationContainer inv, DynamicRegistryManager manager) {
        ToolStack tool = inv.getTinkerable();
        int amountPerItem = tool.getModifierLevel(this.modifier) * this.repairAmount;
        if (amountPerItem <= 0) {
            return RecipeResult.pass();
        }

        // apply modifiers to possibly boost it
        float repairFactor = 1;
        for (ModifierEntry entry : tool.getModifierList()) {
            repairFactor = entry.getHook(ModifierHooks.REPAIR_FACTOR).getRepairFactor(tool, entry, repairFactor);
            if (repairFactor <= 0) {
                return RecipeResult.pass();
            }
        }
        amountPerItem *= repairFactor;

        // get the max amount we can repair
        int available = IncrementalModifierRecipe.getAvailableAmount(inv, this.ingredient, amountPerItem);
        if (available <= 0) {
            return RecipeResult.pass();
        }
        // we will just repair the max possible here, no reason to try less
        tool = tool.copy();
        ToolDamageUtil.repair(tool, available);
        return RecipeResult.success(tool.createStack());
    }

    @Override
    public int shrinkToolSlotBy() {
        return 1;
    }

    @Override
    public void updateInputs(ItemStack result, IMutableTinkerStationContainer inv, boolean isServer) {
        ToolStack tool = inv.getTinkerable();

        // rescale the amount based on modifiers
        float repairFactor = 1.0f;
        for (ModifierEntry entry : tool.getModifierList()) {
            repairFactor = entry.getHook(ModifierHooks.REPAIR_FACTOR).getRepairFactor(tool, entry, repairFactor);
            if (repairFactor <= 0) {
                return;
            }
        }
        // also scale by relevant modifier level
        int amountPerItem = (int) (tool.getModifierLevel(this.modifier) * this.repairAmount * repairFactor);
        if (amountPerItem < 0) {
            return;
        }
        // how much do we need to subtract from our inputs still
        int repairRemaining = tool.getDamage() - ToolStack.from(result).getDamage();
        IncrementalModifierRecipe.updateInputs(inv, this.ingredient, repairRemaining, amountPerItem, ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.modifierRepair.get();
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return ItemStack.EMPTY;
    }
}