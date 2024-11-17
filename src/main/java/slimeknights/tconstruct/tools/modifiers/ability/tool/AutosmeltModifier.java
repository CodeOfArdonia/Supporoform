package slimeknights.tconstruct.tools.modifiers.ability.tool;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ProcessLootModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.recipe.SingleItemContainer;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class AutosmeltModifier extends NoLevelsModifier implements ProcessLootModifierHook {
    /**
     * Cache of relevant smelting recipes
     */
    private final Cache<Item, Optional<SmeltingRecipe>> recipeCache = CacheBuilder
            .newBuilder()
            .maximumSize(64)
            .build();
    /**
     * Inventory instance to use for recipe search
     */
    private final SingleItemContainer inventory = new SingleItemContainer();

    public AutosmeltModifier() {
        RecipeCacheInvalidator.addReloadListener(client -> {
            if (!client) {
                this.recipeCache.invalidateAll();
            }
        });
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROCESS_LOOT);
    }

    /**
     * Gets a furnace recipe without using the cache
     *
     * @param stack Stack to try
     * @param world World instance
     * @return Furnace recipe
     */
    private Optional<SmeltingRecipe> findRecipe(ItemStack stack, World world) {
        this.inventory.setStack(stack);
        return world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, this.inventory, world);
    }

    /**
     * Gets a cached furnace recipe
     *
     * @param stack Stack for recipe
     * @param world World instance
     * @return Cached recipe
     */
    @Nullable
    private SmeltingRecipe findCachedRecipe(ItemStack stack, World world) {
        // don't use the cache if there is a tag, prevent breaking NBT sensitive recipes
        if (stack.hasNbt()) {
            return this.findRecipe(stack, world).orElse(null);
        }
        try {
            return this.recipeCache.get(stack.getItem(), () -> this.findRecipe(stack, world)).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    /**
     * Smelts an item using the relevant furnace recipe
     *
     * @param stack Stack to smelt
     * @param world World instance
     * @return Smelted item, or original if no recipe
     */
    private ItemStack smeltItem(ItemStack stack, World world) {
        // skip blacklisted entries
        if (stack.isIn(TinkerTags.Items.AUTOSMELT_BLACKLIST)) {
            return stack;
        }
        SmeltingRecipe recipe = this.findCachedRecipe(stack, world);
        if (recipe != null) {
            this.inventory.setStack(stack);
            ItemStack output = recipe.craft(this.inventory, world.getRegistryManager());
            if (stack.getCount() > 1) {
                // recipe output is a copy, safe to modify
                output.setCount(output.getCount() * stack.getCount());
            }
            return output;
        }
        return stack;
    }

    @Override
    public void processLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> generatedLoot, LootContext context) {
        World world = context.getWorld();
        if (!generatedLoot.isEmpty()) {
            ListIterator<ItemStack> iterator = generatedLoot.listIterator();
            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                ItemStack smelted = this.smeltItem(stack, world);
                if (stack != smelted) {
                    iterator.set(smelted);
                }
            }
        }
    }
}
