package slimeknights.mantle.recipe.data;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.minecraft.advancement.criterion.InventoryChangedCriterion.Conditions;
import net.minecraft.data.server.recipe.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;

import java.util.function.Consumer;

/**
 * Crafting helper for common recipe types, like stairs, slabs, and packing.
 */
@SuppressWarnings("unused") // API
public interface ICommonRecipeHelper extends IRecipeHelper {
    /* Metals */

    /**
     * Registers a recipe packing a small item into a large one
     *
     * @param consumer  Recipe consumer
     * @param category  Recipe category
     * @param large     Large item
     * @param small     Small item
     * @param largeName Large name
     * @param smallName Small name
     * @param folder    Recipe folder
     */
    default void packingRecipe(Consumer<RecipeJsonProvider> consumer, RecipeCategory category, String largeName, ItemConvertible large, String smallName, ItemConvertible small, String folder) {
        // ingot to block
        Identifier largeId = this.id(large);
        ShapedRecipeJsonBuilder.create(category, large)
                .input('#', small)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .criterion("has_item", RecipeProvider.conditionsFromItem(small))
                .group(largeId.toString())
                .offerTo(consumer, this.wrap(largeId, folder, String.format("_from_%ss", smallName)));
        // block to ingot
        Identifier smallId = this.id(small);
        ShapelessRecipeJsonBuilder.create(category, small, 9)
                .input(large)
                .criterion("has_item", RecipeProvider.conditionsFromItem(large))
                .group(smallId.toString())
                .offerTo(consumer, this.wrap(smallId, folder, String.format("_from_%s", largeName)));
    }

    /**
     * Registers a recipe packing a small item into a large one
     *
     * @param consumer  Recipe consumer
     * @param largeItem Large item
     * @param smallItem Small item
     * @param smallTag  Tag for small item
     * @param largeName Large name
     * @param smallName Small name
     * @param folder    Recipe folder
     */
    default void packingRecipe(Consumer<RecipeJsonProvider> consumer, RecipeCategory category, String largeName, ItemConvertible largeItem, String smallName, ItemConvertible smallItem, TagKey<Item> smallTag, String folder) {
        // ingot to block
        // note our item is in the center, any mod allowed around the edges
        Identifier largeId = this.id(largeItem);
        ShapedRecipeJsonBuilder.create(category, largeItem)
                .input('#', smallTag)
                .input('*', smallItem)
                .pattern("###")
                .pattern("#*#")
                .pattern("###")
                .criterion("has_item", RecipeProvider.conditionsFromItem(smallItem))
                .group(largeId.toString())
                .offerTo(consumer, this.wrap(largeId, folder, String.format("_from_%ss", smallName)));
        // block to ingot
        Identifier smallId = this.id(smallItem);
        ShapelessRecipeJsonBuilder.create(category, smallItem, 9)
                .input(largeItem)
                .criterion("has_item", RecipeProvider.conditionsFromItem(largeItem))
                .group(smallId.toString())
                .offerTo(consumer, this.wrap(smallId, folder, String.format("_from_%s", largeName)));
    }

    /**
     * Adds recipes to convert a block to ingot, ingot to block, and for nuggets
     *
     * @param consumer Recipe consumer
     * @param metal    Metal object
     * @param folder   Folder for recipes
     */
    default void metalCrafting(Consumer<RecipeJsonProvider> consumer, MetalItemObject metal, String folder) {
        ItemConvertible ingot = metal.getIngot();
        this.packingRecipe(consumer, RecipeCategory.MISC, "block", metal.get(), "ingot", ingot, metal.getIngotTag(), folder);
        this.packingRecipe(consumer, RecipeCategory.MISC, "ingot", ingot, "nugget", metal.getNugget(), metal.getNuggetTag(), folder);
    }


    /* Building blocks */

    /**
     * Registers generic saveing block recipes for slabs and stairs
     *
     * @param consumer Recipe consumer
     * @param building Building object instance
     */
    default void slabStairsCrafting(Consumer<RecipeJsonProvider> consumer, BuildingBlockObject building, String folder, boolean addStonecutter) {
        Item item = building.asItem();
        Identifier itemId = this.id(item);
        Conditions hasBlock = RecipeProvider.conditionsFromItem(item);
        // slab
        ItemConvertible slab = building.getSlab();
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, slab, 6)
                .input('B', item)
                .pattern("BBB")
                .criterion("has_item", hasBlock)
                .group(this.id(slab).toString())
                .offerTo(consumer, this.wrap(itemId, folder, "_slab"));
        // stairs
        ItemConvertible stairs = building.getStairs();
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, stairs, 4)
                .input('B', item)
                .pattern("B  ")
                .pattern("BB ")
                .pattern("BBB")
                .criterion("has_item", hasBlock)
                .group(this.id(stairs).toString())
                .offerTo(consumer, this.wrap(itemId, folder, "_stairs"));

        // only add stonecutter if relevant
        if (addStonecutter) {
            Ingredient ingredient = Ingredient.ofItems(item);
            SingleItemRecipeJsonBuilder.createStonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, slab, 2)
                    .criterion("has_item", hasBlock)
                    .offerTo(consumer, this.wrap(itemId, folder, "_slab_stonecutter"));
            SingleItemRecipeJsonBuilder.createStonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, stairs)
                    .criterion("has_item", hasBlock)
                    .offerTo(consumer, this.wrap(itemId, folder, "_stairs_stonecutter"));
        }
    }

    /**
     * Registers generic saveing block recipes for slabs, stairs, and walls
     *
     * @param consumer Recipe consumer
     * @param building Building object instance
     */
    default void stairSlabWallCrafting(Consumer<RecipeJsonProvider> consumer, WallBuildingBlockObject building, String folder, boolean addStonecutter) {
        this.slabStairsCrafting(consumer, building, folder, addStonecutter);
        // wall
        Item item = building.asItem();
        Identifier itemId = this.id(item);
        Conditions hasBlock = RecipeProvider.conditionsFromItem(item);
        ItemConvertible wall = building.getWall();
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, wall, 6)
                .input('B', item)
                .pattern("BBB")
                .pattern("BBB")
                .criterion("has_item", hasBlock)
                .group(this.id(wall).toString())
                .offerTo(consumer, this.wrap(itemId, folder, "_wall"));
        // only add stonecutter if relevant
        if (addStonecutter) {
            Ingredient ingredient = Ingredient.ofItems(item);
            SingleItemRecipeJsonBuilder.createStonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, wall)
                    .criterion("has_item", hasBlock)
                    .offerTo(consumer, this.wrap(itemId, folder, "_wall_stonecutter"));
        }
    }

    /**
     * Registers recipes relevant to wood
     *
     * @param consumer Recipe consumer
     * @param wood     Wood types
     * @param folder   Wood folder
     */
    default void woodCrafting(Consumer<RecipeJsonProvider> consumer, WoodBlockObject wood, String folder) {
        Conditions hasPlanks = RecipeProvider.conditionsFromItem(wood);

        // planks
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, wood, 4).input(wood.getLogItemTag())
                .group("planks")
                .criterion("has_log", RecipeProvider.conditionsFromItemPredicates(ItemPredicate.Builder.create().tag(wood.getLogItemTag()).build()))
                .offerTo(consumer, this.location(folder + "planks"));
        // slab
        ItemConvertible slab = wood.getSlab();
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, slab, 6)
                .input('#', wood)
                .pattern("###")
                .criterion("has_planks", hasPlanks)
                .group("wooden_slab")
                .offerTo(consumer, this.location(folder + "slab"));
        // stairs
        ItemConvertible stairs = wood.getStairs();
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, stairs, 4)
                .input('#', wood)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .criterion("has_planks", hasPlanks)
                .group("wooden_stairs")
                .offerTo(consumer, this.location(folder + "stairs"));

        // log to stripped
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, wood.getWood(), 3)
                .input('#', wood.getLog())
                .pattern("##").pattern("##")
                .group("bark")
                .criterion("has_log", RecipeProvider.conditionsFromItem(wood.getLog()))
                .offerTo(consumer, this.location(folder + "log_to_wood"));
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, wood.getStrippedWood(), 3)
                .input('#', wood.getStrippedLog())
                .pattern("##").pattern("##")
                .group("bark")
                .criterion("has_log", RecipeProvider.conditionsFromItem(wood.getStrippedLog()))
                .offerTo(consumer, this.location(folder + "stripped_log_to_wood"));
        // doors
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, wood.getFence(), 3)
                .input('#', Tags.Items.RODS_WOODEN).input('W', wood)
                .pattern("W#W").pattern("W#W")
                .group("wooden_fence")
                .criterion("has_planks", hasPlanks)
                .offerTo(consumer, this.location(folder + "fence"));
        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, wood.getFenceGate())
                .input('#', Items.STICK).input('W', wood)
                .pattern("#W#").pattern("#W#")
                .group("wooden_fence_gate")
                .criterion("has_planks", hasPlanks)
                .offerTo(consumer, this.location(folder + "fence_gate"));
        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, wood.getDoor(), 3)
                .input('#', wood)
                .pattern("##").pattern("##").pattern("##")
                .group("wooden_door")
                .criterion("has_planks", hasPlanks)
                .offerTo(consumer, this.location(folder + "door"));
        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, wood.getTrapdoor(), 2)
                .input('#', wood)
                .pattern("###").pattern("###")
                .group("wooden_trapdoor")
                .criterion("has_planks", hasPlanks)
                .offerTo(consumer, this.location(folder + "trapdoor"));
        // buttons
        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, wood.getButton())
                .input(wood)
                .group("wooden_button")
                .criterion("has_planks", hasPlanks)
                .offerTo(consumer, this.location(folder + "button"));
        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, wood.getPressurePlate())
                .input('#', wood)
                .pattern("##")
                .group("wooden_pressure_plate")
                .criterion("has_planks", hasPlanks)
                .offerTo(consumer, this.location(folder + "pressure_plate"));
        // signs
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, wood.getSign(), 3)
                .group("sign")
                .input('#', wood).input('X', Tags.Items.RODS_WOODEN)
                .pattern("###").pattern("###").pattern(" X ")
                .criterion("has_planks", RecipeProvider.conditionsFromItem(wood))
                .offerTo(consumer, this.location(folder + "sign"));
    }
}
