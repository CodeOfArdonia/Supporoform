package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.alloying.IAlloyTank;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Module to handle running alloys via a fluid handler, can alloy multiple recipes at once
 */
public class MultiAlloyingModule implements IAlloyingModule {
    private final MantleBlockEntity parent;
    private final IAlloyTank alloyTank;

    /**
     * List of recipes that succeeded last time in {@link #doAlloy()}, only these will be used for the next iteration
     */
    @Nullable
    private List<AlloyRecipe> lastRecipes;

    /**
     * Predicates for common behaviors
     */
    private final Predicate<AlloyRecipe> canPerform, performRecipe;

    public MultiAlloyingModule(MantleBlockEntity parent, IMutableAlloyTank alloyTank) {
        this.parent = parent;
        this.alloyTank = alloyTank;
        this.canPerform = recipe -> recipe.canPerform(alloyTank);
        this.performRecipe = recipe -> {
            recipe.performRecipe(alloyTank);
            return false;
        };
    }

    /**
     * Gets a nonnull world instance from the parent
     */
    private World getLevel() {
        return Objects.requireNonNull(this.parent.getWorld(), "Parent tile entity has null world");
    }

    /**
     * Gets a list of recipes that currently match the tank
     *
     * @return List of recipes that match the tank
     */
    private List<AlloyRecipe> getRecipes() {
        if (this.lastRecipes == null) {
            this.lastRecipes = this.getLevel().getRecipeManager().getAllMatches(TinkerRecipeTypes.ALLOYING.get(), this.alloyTank, this.getLevel());
        }
        return this.lastRecipes;
    }

    /**
     * Runs all the recipes, removing any that no longer match
     *
     * @param predicate Logic to run for recipes, return true to stop looping
     * @return True if any recipe returned true
     */
    private boolean iterateRecipes(Predicate<AlloyRecipe> predicate) {
        List<AlloyRecipe> recipes = this.getRecipes();
        if (recipes.isEmpty()) {
            return false;
        }

        World world = this.getLevel();
        Iterator<AlloyRecipe> iterator = recipes.iterator();
        while (iterator.hasNext()) {
            // if the recipe no longer matches, remove
            // if it matches, run their function and stop if requested
            AlloyRecipe recipe = iterator.next();
            if (recipe.matches(this.alloyTank, world)) {
                if (predicate.test(recipe)) {
                    return true;
                }
            } else {
                iterator.remove();
            }
        }
        return false;
    }

    @Override
    public boolean canAlloy() {
        return this.iterateRecipes(this.canPerform);
    }

    @Override
    public void doAlloy() {
        List<AlloyRecipe> recipes = this.getRecipes();
        if (recipes.isEmpty()) return;
        // shuffle the recipe list, in case we have mutually exclusive recipes it makes them less dependant on name order
        Collections.shuffle(recipes);
        // recipes is the same as lastRecipes at this time, so the iterator will use the shuffled list
        this.iterateRecipes(this.performRecipe);
    }

    /**
     * Clears the list of cached recipes, called when the tank gains a new fluid
     */
    public void clearCachedRecipes() {
        this.lastRecipes = null;
    }
}
