package slimeknights.tconstruct.tables.block.entity.table;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.StringUtils;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.SoundUtils;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.shared.inventory.ConfigurableInvWrapperCapability;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.TinkerStationBlock;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer.ILazyCrafter;
import slimeknights.tconstruct.tables.block.entity.inventory.TinkerStationContainerWrapper;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.network.UpdateTinkerStationRecipePacket;

import org.jetbrains.annotations.Nullable;

public class TinkerStationBlockEntity extends RetexturedTableBlockEntity implements ILazyCrafter {
    /**
     * Slot index of the tool slot
     */
    public static final int TINKER_SLOT = 0;
    /**
     * Slot index of the first input slot
     */
    public static final int INPUT_SLOT = 1;
    /**
     * Name of the TE
     */
    private static final Text NAME = TConstruct.makeTranslation("gui", "tinker_station");

    /**
     * Last crafted crafting recipe
     */
    @Nullable
    @Getter
    private ITinkerStationRecipe lastRecipe;
    /**
     * Result inventory, lazy loads results
     */
    @Getter
    private final LazyResultContainer craftingResult;
    /**
     * Crafting inventory for the recipe calls
     */
    private final TinkerStationContainerWrapper inventoryWrapper;

    @Nullable
    @Getter
    private Text currentError = null;

    @Getter
    private String itemName = "";

    public TinkerStationBlockEntity(BlockPos pos, BlockState state) {
        // if the block is the right type, use it for slot count
        this(pos, state, (state.getBlock() instanceof TinkerStationBlock station) ? station.getSlotCount() : 6);
    }

    public TinkerStationBlockEntity(BlockPos pos, BlockState state, int slots) {
        super(TinkerTables.tinkerStationTile.get(), pos, state, NAME, slots);
        this.itemHandler = new ConfigurableInvWrapperCapability(this, false, false);
        this.itemHandlerCap = LazyOptional.of(() -> this.itemHandler);
        this.inventoryWrapper = new TinkerStationContainerWrapper(this);
        this.craftingResult = new LazyResultContainer(this);
    }

    @Override
    public Text getDefaultName() {
        if (this.world == null) {
            return super.getDefaultName();
        }
        return Text.translatable(this.getCachedState().getBlock().getTranslationKey());
    }

    /**
     * Gets the number of item input slots, ignoring the tool
     *
     * @return Input count
     */
    public int getInputCount() {
        return this.size() - 1;
    }

    @Override
    public void resize(int size) {
        super.resize(size);
        this.inventoryWrapper.resize();
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int menuId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new TinkerStationContainerMenu(menuId, playerInventory, this);
    }

    /* Crafting */

    @Override
    public ItemStack calcResult(@Nullable PlayerEntity player) {
        if (this.world == null) {
            return ItemStack.EMPTY;
        }

        // assume empty unless we learn otherwise
        ItemStack result = ItemStack.EMPTY;
        this.currentError = null;

        if (!this.world.isClient && this.world.getServer() != null) {
            RecipeManager manager = this.world.getServer().getRecipeManager();

            // first, try the cached recipe
            ITinkerStationRecipe recipe = this.lastRecipe;
            // if it does not match, find a new recipe
            if (recipe == null || !recipe.matches(this.inventoryWrapper, this.world)) {
                recipe = manager.getFirstMatch(TinkerRecipeTypes.TINKER_STATION.get(), this.inventoryWrapper, this.world).orElse(null);
            }

            // if we have a recipe, fetch its result
            boolean needsSync = true;
            if (recipe != null) {
                // sync if the recipe is different
                if (this.lastRecipe != recipe) {
                    this.lastRecipe = recipe;
                    this.syncToRelevantPlayers(this::syncRecipe);
                    needsSync = false;
                }

                // try for UI errors
                RecipeResult<ItemStack> validatedResult = recipe.getValidatedResult(this.inventoryWrapper, );
                if (validatedResult.isSuccess()) {
                    result = validatedResult.getResult();
                } else if (validatedResult.hasError()) {
                    this.currentError = validatedResult.getMessage();
                }
            }
            // recipe will sync screen, so only need to call it when not syncing the recipe
            if (needsSync) {
                this.syncScreenToRelevantPlayers();
            }
        }
        // client side only needs to update result, server syncs message elsewhere
        else if (this.lastRecipe != null && this.lastRecipe.matches(this.inventoryWrapper, this.world)) {
            RecipeResult<ItemStack> validatedResult = this.lastRecipe.getValidatedResult(this.inventoryWrapper, );
            if (validatedResult.isSuccess()) {
                result = validatedResult.getResult();
            } else if (validatedResult.hasError()) {
                this.currentError = validatedResult.getMessage();
            }
        }

        // set name if we have one
        if (!result.isEmpty() && !this.itemName.isEmpty()) {
            TooltipUtil.setDisplayName(result, this.itemName);
        }

        return result;
    }

    @Override
    public void onCraft(PlayerEntity player, ItemStack result, int amount) {
        if (amount == 0 || this.lastRecipe == null || this.world == null) {
            return;
        }

        // fire crafting events
        result.onCraft(this.world, player, amount);
        ForgeEventFactory.firePlayerCraftingEvent(player, result, this.inventoryWrapper);
        this.playCraftSound(player);

        // run the recipe, will shrink inputs
        // run both sides for the sake of shift clicking
        this.inventoryWrapper.setPlayer(player);
        this.lastRecipe.updateInputs(result, this.inventoryWrapper, !this.world.isClient);
        this.inventoryWrapper.setPlayer(null);

        // remove the center slot item, just clear it entirely (if you want shrinking you should use the outer slots or ask nicely for a shrink amount hook)
        ItemStack tinkerable = this.getStack(TINKER_SLOT);
        if (!tinkerable.isEmpty()) {
            int shrinkToolSlot = this.lastRecipe.shrinkToolSlotBy();
            if (tinkerable.getCount() <= shrinkToolSlot) {
                this.setStack(TINKER_SLOT, ItemStack.EMPTY);
            } else {
                this.setStack(TINKER_SLOT, ItemHandlerHelper.copyStackWithSize(tinkerable, tinkerable.getCount() - shrinkToolSlot));
            }
        }
        this.itemName = "";
    }

    @Override
    public void setStack(int slot, ItemStack itemstack) {
        super.setStack(slot, itemstack);
        // clear the crafting result when the matrix changes so we recalculate the result
        this.craftingResult.clear();
        this.inventoryWrapper.refreshInput(slot);
    }

    @Override
    protected void playCraftSound(PlayerEntity player) {
        if (this.isSoundReady(player)) {
            SoundUtils.playSoundForAll(player, this.getInputCount() > 4 ? SoundEvents.BLOCK_ANVIL_USE : Sounds.SAW.getSound(), 0.8f, 0.8f + 0.4f * player.getWorld().random.nextFloat());
        }
    }


    /* Item name */

    /**
     * Sets the name of the item
     */
    public void setItemName(String name) {
        this.itemName = name;
        ItemStack result = this.craftingResult.getResult();
        if (!result.isEmpty()) {
            // if blank, set name to original name
            if (StringUtils.isBlank(name)) {
                // if the input was named, instead of clearing restore the old name
                ItemStack input = this.getStack(TINKER_SLOT);
                if (!input.isEmpty()) {
                    name = TooltipUtil.getDisplayName(input);
                } else {
                    // empty string will clear the stack tag
                    name = "";
                }
            }
            TooltipUtil.setDisplayName(result, name);
        }
    }


    /* Syncing */

    /**
     * Sends the current recipe to the given player
     *
     * @param player Player to send an update to
     */
    public void syncRecipe(PlayerEntity player) {
        // must have a last recipe and a server level
        if (this.lastRecipe != null && this.world != null && !this.world.isClient && player instanceof ServerPlayerEntity server) {
            TinkerNetwork.getInstance().sendTo(new UpdateTinkerStationRecipePacket(this.pos, this.lastRecipe), server);
        }
    }

    /**
     * Updates the recipe from the server
     *
     * @param recipe New recipe
     */
    public void updateRecipe(ITinkerStationRecipe recipe) {
        this.lastRecipe = recipe;
        this.craftingResult.clear();
    }
}