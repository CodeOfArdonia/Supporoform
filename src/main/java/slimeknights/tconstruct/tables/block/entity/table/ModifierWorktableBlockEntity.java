package slimeknights.tconstruct.tables.block.entity.table;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.shared.inventory.ConfigurableInvWrapperCapability;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer.ILazyCrafter;
import slimeknights.tconstruct.tables.block.entity.inventory.ModifierWorktableContainerWrapper;
import slimeknights.tconstruct.tables.menu.ModifierWorktableContainerMenu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ModifierWorktableBlockEntity extends RetexturedTableBlockEntity implements ILazyCrafter {
    /**
     * Index containing the tool
     */
    public static final int TINKER_SLOT = 0;
    /**
     * First input slot index
     */
    public static final int INPUT_START = 1;
    /**
     * Number of input slots
     */
    public static final int INPUT_COUNT = 2;
    /**
     * Title for the GUI
     */
    private static final Text NAME = TConstruct.makeTranslation("gui", "modifier_worktable");

    /**
     * Result inventory, lazy loads results
     */
    @Getter
    private final LazyResultContainer craftingResult;
    /**
     * Crafting inventory for the recipe calls
     */
    @Getter
    private final ModifierWorktableContainerWrapper inventoryWrapper;

    /**
     * If true, the last recipe is the current recipe. If false, no recipe was found. If null, have not tried recipe lookup
     */
    private Boolean recipeValid;
    /**
     * Cache of the last recipe, may not be the current one
     */
    @Nullable
    private IModifierWorktableRecipe lastRecipe;
    /* Current buttons to display */
    @Nonnull
    private List<ModifierEntry> buttons = Collections.emptyList();
    /**
     * Index of the currently selected modifier
     */
    private int selectedModifierIndex = -1;

    /**
     * Current result, may be modified again later
     */
    @Nullable
    @Getter
    private LazyToolStack result = null;
    /**
     * Current message displayed on the screen
     */
    @Getter
    private Text currentMessage = Text.empty();

    public ModifierWorktableBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerTables.modifierWorktableTile.get(), pos, state, NAME, 3);
        this.itemHandler = new ConfigurableInvWrapperCapability(this, false, false);
        this.itemHandlerCap = LazyOptional.of(() -> this.itemHandler);
        this.inventoryWrapper = new ModifierWorktableContainerWrapper(this);
        this.craftingResult = new LazyResultContainer(this);
    }

    /**
     * Selects a modifier by index. Will fetch the buttons list if the index is non-negative
     *
     * @param index New index
     */
    public void selectModifier(int index) {
        result = null;
        craftingResult.clear();
        if (index >= 0) {
            List<ModifierEntry> list = getCurrentButtons();
            if (index < list.size()) {
                selectedModifierIndex = index;
                ModifierEntry entry = list.get(index);

                // last recipe must be nonnull for list to be non-empty
                assert lastRecipe != null;
                RecipeResult<LazyToolStack> recipeResult = lastRecipe.getResult(inventoryWrapper, entry);
                if (recipeResult.isSuccess()) {
                    result = recipeResult.getResult();
                    currentMessage = Text.empty();
                } else if (recipeResult.hasError()) {
                    currentMessage = recipeResult.getMessage();
                } else {
                    currentMessage = lastRecipe.getDescription(inventoryWrapper);
                }
                return;
            }
        }
        // index is either not valid or the list is empty, so just clear
        selectedModifierIndex = -1;
        currentMessage = recipeValid == Boolean.TRUE && lastRecipe != null
                ? lastRecipe.getDescription(inventoryWrapper)
                : Text.empty();
    }

    /**
     * Gets the index of the selected pattern
     */
    public int getSelectedIndex() {
        return selectedModifierIndex;
    }

    /**
     * Updates the current recipe
     */
    public IModifierWorktableRecipe updateRecipe(IModifierWorktableRecipe recipe) {
        lastRecipe = recipe;
        recipeValid = true;
        currentMessage = lastRecipe.getDescription(inventoryWrapper);
        buttons = recipe.getModifierOptions(inventoryWrapper);
        //        if (!level.isClientSide) {
        //          syncToRelevantPlayers(this::syncScreen);
        //        }

        // clear the active modifier
        selectModifier(-1);
        return recipe;
    }

    /**
     * Gets the currently active recipe
     */
    @Nullable
    public IModifierWorktableRecipe getCurrentRecipe() {
        if (recipeValid == Boolean.TRUE) {
            return lastRecipe;
        }
        if (recipeValid == null && world != null) {
            // if the previous recipe matches, flip state to use that again
            if (lastRecipe != null && lastRecipe.matches(inventoryWrapper, world)) {
                return updateRecipe(lastRecipe);
            }
            // look for a new recipe, if it matches cache it
            Optional<IModifierWorktableRecipe> recipe = world.getRecipeManager().getFirstMatch(TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), inventoryWrapper, world);
            if (recipe.isPresent()) {
                return updateRecipe(recipe.get());
            }
            recipeValid = false;
            currentMessage = Text.empty();
            buttons = Collections.emptyList();
            selectModifier(-1);
        }
        // level null or no recipe found
        return null;
    }

    /**
     * Gets a map of all recipes for the current inputs
     *
     * @return List of recipes for the current inputs
     */
    public List<ModifierEntry> getCurrentButtons() {
        if (world == null) {
            return Collections.emptyList();
        }
        // if last recipe is not fetched, the buttons may be outdated
        getCurrentRecipe();
        return buttons;
    }

    /**
     * Called when a slot changes to clear the current result
     */
    public void onSlotChanged(int slot) {
        this.inventoryWrapper.refreshInput(slot);
        this.recipeValid = null;
        this.buttons = Collections.emptyList();
        selectModifier(-1);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack original = getStack(slot);
        super.setStack(slot, stack);
        // if the stack changed, clear everything
        if (original.getCount() != stack.getCount() || !ItemStack.canCombine(original, stack)) {
            onSlotChanged(slot);
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int menuId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new ModifierWorktableContainerMenu(menuId, playerInventory, this);
    }

    @Override
    public ItemStack calcResult(@Nullable PlayerEntity player) {
        if (selectedModifierIndex != -1) {
            IModifierWorktableRecipe recipe = getCurrentRecipe();
            if (recipe != null && result != null) {
                return result.getStack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void onCraft(PlayerEntity player, ItemStack resultItem, int amount) {
        // the recipe should match if we got this far, but being null is a problem
        LazyToolStack result = this.result;  // result is going to get cleared as we update things
        if (amount == 0 || this.world == null || lastRecipe == null || result == null) {
            return;
        }

        // we are definitely crafting at this point
        resultItem.onCraft(this.world, player, amount);
        ForgeEventFactory.firePlayerCraftingEvent(player, resultItem, this.inventoryWrapper);
        this.playCraftSound(player);

        // run the recipe, will shrink inputs
        // run both sides for the sake of shift clicking
        this.inventoryWrapper.setPlayer(player);
        this.lastRecipe.updateInputs(result, inventoryWrapper, getCurrentButtons().get(selectedModifierIndex), !world.isClient);
        this.inventoryWrapper.setPlayer(null);

        ItemStack tinkerable = this.getStack(TINKER_SLOT);
        if (!tinkerable.isEmpty()) {
            int shrinkToolSlot = this.lastRecipe.shrinkToolSlotBy(result);
            if (tinkerable.getCount() <= shrinkToolSlot) {
                this.setStack(TINKER_SLOT, ItemStack.EMPTY);
            } else {
                this.setStack(TINKER_SLOT, ItemHandlerHelper.copyStackWithSize(tinkerable, tinkerable.getCount() - shrinkToolSlot));
            }
        }
        // screen should reset back to empty now that we crafted
//    syncRecipe();
    }
}
